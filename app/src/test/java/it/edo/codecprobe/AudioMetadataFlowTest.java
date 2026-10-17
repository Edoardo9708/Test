package it.edo.codecprobe;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowContentResolver;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class AudioMetadataFlowTest {
    static final Uri TREE=Uri.parse("content://audio.documents/tree/root");
    public static class Documents extends ContentProvider {
        final Map<String,File> files=new LinkedHashMap<>();final Map<String,String> names=new LinkedHashMap<>();
        File directory;boolean failLyrics,corruptLyrics,cancelAfterWrite;int serial;
        public boolean onCreate(){return true;}
        public String getType(Uri uri){return "application/octet-stream";}
        public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){
            MatrixCursor rows=new MatrixCursor(projection);
            if("children".equals(uri.getLastPathSegment())){for(String id:names.keySet())row(rows,projection,id);}
            else if(names.containsKey(uri.getLastPathSegment()))row(rows,projection,uri.getLastPathSegment());return rows;
        }
        private void row(MatrixCursor rows,String[] columns,String id){Object[] values=new Object[columns.length];for(int n=0;n<columns.length;n++)values[n]="_size".equals(columns[n])?files.get(id).length():names.get(id);rows.addRow(values);}
        @Override public Bundle call(String method,String arg,Bundle extras){
            Uri uri=extras.getParcelable("uri");
            if("android:createDocument".equals(method)){
                String name=extras.getString(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
                if(failLyrics&&name.endsWith(".lrc"))return new Bundle();
                String id="out-"+(++serial);try{File file=File.createTempFile("output-",".tmp",directory);files.put(id,file);names.put(id,name);}catch(IOException e){throw new IllegalStateException(e);}
                Bundle result=new Bundle();result.putParcelable("uri",DocumentsContract.buildDocumentUriUsingTree(TREE,id));return result;
            }
            if("android:deleteDocument".equals(method)){String id=uri.getLastPathSegment();File file=files.remove(id);names.remove(id);if(file!=null)file.delete();return new Bundle();}
            throw new UnsupportedOperationException(method);
        }
        public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
            String id=uri.getLastPathSegment();File file=files.get(id);if(file==null)throw new FileNotFoundException(id);
            if(corruptLyrics&&mode.equals("r")&&id.startsWith("out-")&&names.get(id).endsWith(".lrc"))try(FileOutputStream out=new FileOutputStream(file,true)){out.write(1);}catch(IOException e){throw new FileNotFoundException(e.toString());}
            return ParcelFileDescriptor.open(file,ParcelFileDescriptor.parseMode(mode));
        }
        public Uri insert(Uri u,ContentValues v){throw new AssertionError();}public int update(Uri u,ContentValues v,String s,String[] a){throw new AssertionError();}public int delete(Uri u,String s,String[] a){throw new AssertionError();}
    }
    private Context context;private Documents provider;private File audio;
    @Before public void setup()throws Exception{
        context=RuntimeEnvironment.getApplication();provider=new Documents();provider.directory=Files.createTempDirectory("audio-docs").toFile();ShadowContentResolver.registerProviderInternal("audio.documents",provider);
        audio=new File(provider.directory,"encoded.opus");Files.write(audio.toPath(),new byte[]{1,2,3,4});
    }
    @After public void cleanup(){for(File file:provider.directory.listFiles())file.delete();provider.directory.delete();}
    @Test public void matchingPrefersSameParentAndRejectsAmbiguousTitles()throws Exception{
        String input="content://docs/document/primary%3AMusic%2FAlbumA%2FSong.flac";
        List<String> names=List.of("Song.lrc","Song.lrc");List<String> uris=List.of("content://docs/document/primary%3AMusic%2FAlbumB%2FSong.lrc","content://docs/document/primary%3AMusic%2FAlbumA%2FSong.lrc");
        assertEquals(uris.get(1),AudioLyrics.match(input,"Song.flac",uris,names));
        assertThrows(IOException.class,()->AudioLyrics.match("content://other/song","Song.flac",uris,names));
        assertEquals("",AudioLyrics.match(input,"Other.flac",uris,names));
        assertEquals("Song (2).opus",AudioOutput.unique("Song.opus",Set.of("Song.lrc"),true));
    }
    @Test public void savesMatchingPairWithOriginalBytesAndHash()throws Exception{
        byte[] lyrics=new byte[]{(byte)0xef,(byte)0xbb,(byte)0xbf,'[','0','0',':','0','1',']','x','\r','\n'};
        ArrayList<String> log=new ArrayList<>();Uri output=AudioOutput.save(context,TREE,audio,"Song.flac",()->false,lyrics,log::add);
        assertNotNull(output);assertEquals(List.of("Song.opus","Song.lrc"),new ArrayList<>(provider.names.values()));
        File text=provider.files.entrySet().stream().filter(e->provider.names.get(e.getKey()).endsWith(".lrc")).findFirst().get().getValue();
        assertArrayEquals(lyrics,Files.readAllBytes(text.toPath()));assertTrue(log.get(0).contains(AudioLyrics.hash(lyrics)));
    }
    @Test public void sidecarFailureRollsBackNewAudioAndKeepsExistingFiles()throws Exception{
        File old=new File(provider.directory,"old");Files.write(old.toPath(),new byte[]{9});provider.files.put("old",old);provider.names.put("old","keep.opus");
        provider.failLyrics=true;assertThrows(IOException.class,()->AudioOutput.save(context,TREE,audio,"Song.flac",()->false,new byte[]{4},s->{}));
        assertEquals(List.of("keep.opus"),new ArrayList<>(provider.names.values()));assertArrayEquals(new byte[]{9},Files.readAllBytes(old.toPath()));
    }
    @Test public void failedReadbackRemovesBothNewFiles()throws Exception{
        provider.corruptLyrics=true;assertThrows(IOException.class,()->AudioOutput.save(context,TREE,audio,"Song.flac",()->false,new byte[]{4},s->{}));assertTrue(provider.files.isEmpty());
    }
    @Test public void lyricsDraftIsIndependentAndSurvivesReopen(){
        context.getSharedPreferences("audio-workspace",0).edit().clear().commit();AudioState state=new AudioState();state.lyricsInputs.add("content://text/1");state.lyricsNames.add("Song.lrc");AudioDraftStore.save(context,state);
        AudioState read=AudioDraftStore.load(context);assertEquals(state.lyricsInputs,read.lyricsInputs);assertEquals(state.lyricsNames,read.lyricsNames);context.getSharedPreferences("audio-workspace",0).edit().clear().commit();
    }
}
