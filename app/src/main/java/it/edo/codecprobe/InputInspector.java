package it.edo.codecprobe;

import android.content.Context;
import android.database.Cursor;
import android.media.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;

/** Reads declared properties and track payload sizes without decoding media. */
final class InputInspector {
    private InputInspector(){}
    static final class Report {
        final Map<String,String> summarySections,detailSections;
        final String summary,details;
        Report(Map<String,String> summary,Map<String,String> details){summarySections=summary;detailSections=details;this.summary=flatten(summary);this.details=flatten(details);}
        Report(String summary,String details){this(Collections.singletonMap("INFORMAZIONI",summary),Collections.singletonMap("DETTAGLI",details));}
        String full(){return summary+"\n\nMETADATI E DETTAGLI\n"+details;}
        private static String flatten(Map<String,String> sections){StringBuilder out=new StringBuilder();for(Map.Entry<String,String> e:sections.entrySet())out.append("\n*** ").append(e.getKey()).append(" ***\n").append(e.getValue()).append("\n");return out.toString();}
    }
    static String describe(Context c,Uri u){return inspect(c,u,()->false).full();}
    static String describe(Context c,Uri u,BooleanSupplier cancel){return inspect(c,u,cancel).full();}
    static Report inspect(Context context,Uri uri,BooleanSupplier cancel){
        Map<String,String> summary=new LinkedHashMap<>(),details=new LinkedHashMap<>();
        String access;
        if(OriginalMedia.hasLocationPermission(context)){
            try{uri=OriginalMedia.resolve(context,uri);access=OriginalMedia.requiresOriginal(uri)?"Originale richiesto senza oscuramento GPS":"Provider esterno: originale senza oscuramento non garantito";}
            catch(Exception e){return new Report("Apertura originale non riuscita",String.valueOf(e.getMessage()));}
        }else access="Permesso posizione dei file non ancora concesso: il GPS potrebbe essere nascosto. Sarà richiesto prima della conversione.";
        StringBuilder file=new StringBuilder(),dates=new StringBuilder(),authorship=new StringBuilder(),other=new StringBuilder(),sizes=new StringBuilder(),structure=new StringBuilder();
        line(dates,"Accesso alla posizione",access);
        long bytes=-1;
        try(Cursor c=context.getContentResolver().query(uri,new String[]{OpenableColumns.SIZE},null,null,null)){if(c!=null&&c.moveToFirst()&&!c.isNull(0))bytes=c.getLong(0);}catch(Exception ignored){}
        if(bytes<0)try(ParcelFileDescriptor fd=context.getContentResolver().openFileDescriptor(uri,"r")){if(fd!=null)bytes=fd.getStatSize();}catch(Exception ignored){}
        line(file,"Dimensione totale",bytes>=0?sizeText(bytes):"non disponibile dal provider");
        String container="non dichiarato";
        MediaMetadataRetriever retriever=new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context,uri);String mime=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);if(mime!=null)container=containerName(mime);
            String duration=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);if(duration!=null)try{line(file,"Durata",String.format(Locale.ITALY,"%.2f s",Long.parseLong(duration)/1000.0));}catch(NumberFormatException ignored){}
            String rotation=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);if(rotation!=null)line(file,"Rotazione",rotation+"°");
            TreeMap<String,String> values=new TreeMap<>();
            for(Field field:MediaMetadataRetriever.class.getFields())if(field.getName().startsWith("METADATA_KEY_"))try{String value=retriever.extractMetadata(field.getInt(null));if(value!=null)values.put(field.getName().substring(13),value);}catch(Exception ignored){}
            if(!values.containsKey("LOCATION"))line(dates,"Posizione GPS","NON RILEVATA dal lettore Android · non prova che il file sia privo di coordinate");
            for(Map.Entry<String,String> entry:values.entrySet()){
                String k=entry.getKey(),v=entry.getValue();
                StringBuilder group=k.equals("DATE")||k.equals("LOCATION")?dates:Arrays.asList("TITLE","ARTIST","AUTHOR","ALBUM","ALBUMARTIST","COMPOSER","GENRE","WRITER","CD_TRACK_NUMBER","DISC_NUMBER","YEAR").contains(k)?authorship:other;
                line(group,label(k),v);
            }
        }catch(Exception e){line(other,"Lettura metadati",e.getMessage());}finally{try{retriever.release();}catch(Exception ignored){}}
        line(file,"Contenitore",container);summary.put("FILE ORIGINALE",file.toString());
        MediaExtractor extractor=new MediaExtractor();int videos=0,audios=0,metadata=0;long sampleTotal=0;boolean counted=false;
        List<String> names=new ArrayList<>();
        try {
            extractor.setDataSource(context,uri,null);
            for(int i=0;i<extractor.getTrackCount();i++){
                MediaFormat f=extractor.getTrackFormat(i);String mime=f.getString(MediaFormat.KEY_MIME);String title;
                StringBuilder info=new StringBuilder();line(info,"Codec",codecName(mime));
                if(mime!=null&&mime.startsWith("video/")){
                    title="VIDEO "+(++videos);int w=integer(f,MediaFormat.KEY_WIDTH,-1),h=integer(f,MediaFormat.KEY_HEIGHT,-1);if(w>0&&h>0)line(info,"Risoluzione",w+" × "+h);
                    Number fps=null;try{fps=f.getNumber(MediaFormat.KEY_FRAME_RATE);}catch(RuntimeException ignored){}
                    line(info,"FPS dichiarati",fps==null?"non disponibili":String.format(Locale.ITALY,"%.3f",fps.doubleValue()));
                    int transfer=integer(f,MediaFormat.KEY_COLOR_TRANSFER,-1);line(info,"Gamma dinamica",transfer==6?"HDR · PQ":transfer==7?"HDR · HLG":transfer==3||transfer==1?"SDR":"non determinabile dai metadati");
                    int profile=integer(f,MediaFormat.KEY_PROFILE,-1);String depth="non dichiarata";if("video/hevc".equals(mime)){if(profile==1)depth="8 bit · Main";else if(profile==2||profile==4096||profile==8192)depth="10 bit";}line(info,"Profondità colore",depth);
                    int bitrate=integer(f,MediaFormat.KEY_BIT_RATE,-1);if(bitrate>0)line(info,"Bitrate dichiarato",String.format(Locale.ITALY,"%.2f Mbit/s",bitrate/1000000.0));
                    if(transfer==6||transfer==7)line(info,"Compatibilità","HDR non ancora supportato per la conversione");
                }else if(mime!=null&&mime.startsWith("audio/")){
                    title="AUDIO "+(++audios);int channels=integer(f,MediaFormat.KEY_CHANNEL_COUNT,-1),hz=integer(f,MediaFormat.KEY_SAMPLE_RATE,-1);if(channels>0)line(info,"Canali",Integer.toString(channels));if(hz>0)line(info,"Campionamento",hz+" Hz");
                    int bitrate=integer(f,MediaFormat.KEY_BIT_RATE,-1);if(bitrate>0)line(info,"Bitrate dichiarato",String.format(Locale.ITALY,"%.1f kbit/s",bitrate/1000.0));
                    line(info,"Conversione","audio/mp4a-latm".equals(mime)?"AAC copiato senza ricodifica":"codec audio non ancora supportato");
                }else{title="TRACCIA METADATI "+(++metadata);line(info,"Trattamento",mime!=null&&mime.startsWith("application/")?"campioni copiati con i timestamp originali":"traccia non supportata: la conversione si interromperà");}
                summary.put(title,info.toString());names.add(title+" · "+codecName(mime));
                StringBuilder raw=new StringBuilder();for(String key:new TreeSet<>(f.getKeys())){
                    String value;try{int type=f.getValueTypeForKey(key);if(type==MediaFormat.TYPE_STRING)value=f.getString(key);else if(type==MediaFormat.TYPE_BYTE_BUFFER){ByteBuffer b=f.getByteBuffer(key);value=b==null?"null":b.remaining()+" byte di dati binari";}else value=String.valueOf(f.getNumber(key));}catch(RuntimeException e){value="non leggibile";}line(raw,label(key),value);
                }details.put(title+" · DETTAGLI TECNICI",raw.toString());
            }
            long[] trackBytes=new long[names.size()];for(int i=0;i<trackBytes.length;i++)extractor.selectTrack(i);
            while(extractor.getSampleTrackIndex()>=0){if(cancel.getAsBoolean())return new Report("Analisi annullata","");int i=extractor.getSampleTrackIndex();long n=extractor.getSampleSize();if(n<0)throw new IllegalStateException("Dimensione campione assente");trackBytes[i]=Math.addExact(trackBytes[i],n);sampleTotal=Math.addExact(sampleTotal,n);if(!extractor.advance())break;}
            for(int i=0;i<trackBytes.length;i++)line(sizes,names.get(i),sizeText(trackBytes[i]));counted=true;
            if(videos==0)summary.put("VIDEO","Nessuna traccia video rilevata");if(audios==0)summary.put("AUDIO","Assente");
            if(videos>1||audios>1)line(structure,"Compatibilità","Più tracce video o audio non ancora supportate");
        }catch(Exception e){line(structure,"Analisi tracce",e.getMessage());}finally{extractor.release();}
        summary.put("OCCUPAZIONE DELLE TRACCE",sizes.length()==0?"Non disponibile":sizes.toString());
        if(counted&&bytes>=sampleTotal)line(structure,"Dati fuori dai campioni",sizeText(bytes-sampleTotal)+" · struttura, tag e padding");
        if(container.startsWith("MP4")||container.equals("MOV / QuickTime")){
            ParcelFileDescriptor fd=null;try{fd=context.getContentResolver().openFileDescriptor(uri,"r");if(fd==null)throw new IllegalStateException("File assente");try(ParcelFileDescriptor.AutoCloseInputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){
                Mp4Boxes.Sizes b=Mp4Boxes.read(in.getChannel());line(structure,"Indici e metadati tecnici · moov",sizeText(b.moov));line(structure,"Blocchi descrittivi · udta/meta",sizeText(b.tags)+" · già inclusi nel moov se interni");
                Mp4Metadata.check(in.getChannel(),cancel);line(structure,"Copia metadati","MP4/MOV compatibile · blocchi originali, date e lingua");
            }}catch(Exception e){line(structure,"Copia metadati","Non garantibile: "+e.getMessage());}finally{if(fd!=null)try{fd.close();}catch(Exception ignored){}}
        }else line(structure,"Copia metadati","Richiede MP4/MOV non frammentato; gli altri contenitori sono attualmente rifiutati per evitare perdite");
        summary.put("METADATI E CONTENITORE",structure.toString());
        Map<String,String> ordered=new LinkedHashMap<>();ordered.put("DATE E POSIZIONE",dates.length()==0?"Nessun campo esposto da Android":dates.toString());ordered.put("TITOLO, AUTORE E CONTENUTO",authorship.length()==0?"Nessun campo esposto da Android":authorship.toString());ordered.put("ALTRI CAMPI DEL FILE",other.toString());ordered.putAll(details);
        ordered.put("COME VENGONO CONSERVATI","I blocchi descrittivi vengono copiati integralmente e verificati tramite SHA-256, anche se i singoli tag non sono leggibili da Android. Date interne e lingua sono conservate. Codec, bitrate, indici e struttura del nuovo video vengono rigenerati. Blocchi o tracce non supportati interrompono la conversione. Tag proprietari con riferimenti interni possono richiedere una gestione specifica.");
        return new Report(summary,ordered);
    }
    private static void line(StringBuilder b,String key,String value){if(b.length()>0)b.append("\n\n");b.append(key).append("\n").append(value==null?"non disponibile":value);}
    private static String label(String key){switch(key){case "DATE":return "Data dichiarata dal file";case "LOCATION":return "Posizione GPS";case "TITLE":return "Titolo";case "ARTIST":return "Artista";case "AUTHOR":return "Autore";case "ALBUM":return "Album";case "ALBUMARTIST":return "Artista dell’album";case "COMPOSER":return "Compositore";case "GENRE":return "Genere";case "WRITER":return "Autore del contenuto";case "YEAR":return "Anno";case "language":return "Lingua della traccia";case "durationUs":return "Durata · microsecondi";case "frame-rate":return "FPS dichiarati";case "width":return "Larghezza · pixel";case "height":return "Altezza · pixel";case "bitrate":return "Bitrate · bit/s";case "mime":return "Tipo MIME";default:return key.replace('_',' ').replace('-',' ')+" · campo originale";}}
    private static String sizeText(long n){return String.format(Locale.ITALY,"%.3f MB · %,d byte",n/1000000.0,n);}
    private static int integer(MediaFormat f,String k,int fallback){try{return f.containsKey(k)?NumberValues.intExact(f.getNumber(k),k):fallback;}catch(RuntimeException e){return fallback;}}
    private static String containerName(String mime){switch(mime){case "video/mp4":return "MP4";case "video/quicktime":return "MOV / QuickTime";case "video/x-matroska":return "Matroska / MKV";case "video/webm":return "WebM";case "video/3gpp":return "3GP";default:return mime;}}
    private static String codecName(String mime){if(mime==null)return "sconosciuto";switch(mime){case "video/hevc":return "H.265 / HEVC";case "video/avc":return "H.264 / AVC";case "video/av01":return "AV1";case "video/x-vnd.on2.vp9":return "VP9";case "audio/mp4a-latm":return "AAC";case "audio/mpeg":return "MP3";case "audio/opus":return "Opus";default:return mime;}}
}
