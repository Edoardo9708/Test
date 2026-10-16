package it.edo.codecprobe;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

/** Samples only known repetitive successes; errors, setup and unknown messages pass unchanged. */
final class CompactLog {
    private static final String[] HOT={
        "BEGIN conversione YUV frame=", "OK conversione YUV", "BEGIN x265_encoder_encode frame=",
        "END x265_encoder_encode rc=", "PACCHETTO: pts_us=", "BEGIN scrittura MP4 packet=",
        "OK scrittura MP4 packet=", "VERIFY frame=", "OK audio sample=", "OK metadati sample=",
        "FRAME HDR ·", "CODIFICA HDR · frame=", "FLUSH rc=", "Metadati originali:"
    };
    private static final long INTERVAL_MS=15000;
    private static final int CONTEXT_LINES=24,MAX_SAMPLE_CHARS=2048;
    private final Consumer<String> output;
    private final long[] seen=new long[HOT.length];
    private final String[] latest=new String[HOT.length];
    private final ArrayDeque<String> context=new ArrayDeque<>();
    private long dropped,lastSummary=-1;

    CompactLog(Consumer<String> output){this.output=output;}

    void accept(String message,long now){
        String body=body(message);
        if(now<lastSummary)lastSummary=now;
        boolean error=body.contains("ERRORE")||body.contains("AVVISO")||body.contains("Exception")
            ||body.contains("ERROR")||body.contains("rc=-")||body.contains("risultato=-");
        if(body.startsWith("*** INIZIO VIDEO ")){
            flush();Arrays.fill(seen,0);context.clear();lastSummary=now;
        }
        if(error){
            flush();
            if(!context.isEmpty()){
                output.accept("*** ULTIMI EVENTI DETTAGLIATI PRIMA DELL’ERRORE ***\n"+String.join("\n",context));
                context.clear();
            }
            output.accept(message);return;
        }
        int kind=-1;
        for(int i=0;i<HOT.length;i++)if(body.startsWith(HOT[i])){kind=i;break;}
        if(kind<0){
            if(body.contains("***")||body.startsWith("SESSION END")||body.startsWith("OK x265 frames")||body.startsWith("OK HDR · frames"))flush();
            output.accept(message);return;
        }
        if(lastSummary<0)lastSummary=now;
        if(++seen[kind]<=2){output.accept(message);return;}
        String sample=shortSample(message);
        latest[kind]=sample;dropped++;
        context.addLast(message.startsWith("[")?sample:"["+now+" ms] "+sample);
        while(context.size()>CONTEXT_LINES)context.removeFirst();
        if(now-lastSummary>=INTERVAL_MS){flush();lastSummary=now;}
    }

    void finish(){flush();}

    private void flush(){
        if(dropped==0)return;
        StringBuilder summary=new StringBuilder("RIEPILOGO EVENTI RIPETITIVI · ").append(dropped)
            .append(" messaggi raggruppati; ultimi valori per tipo:\n");
        for(int i=0;i<latest.length;i++)if(latest[i]!=null){summary.append("• ").append(latest[i]).append('\n');latest[i]=null;}
        dropped=0;output.accept(summary.toString().trim());
    }

    private static String shortSample(String text){
        return text.length()<=MAX_SAMPLE_CHARS?text:text.substring(0,MAX_SAMPLE_CHARS)+" … [campione abbreviato]";
    }
    private static String body(String line){
        if(line.startsWith("[")){
            int end=line.indexOf(" ms] ");
            if(end>1&&end<24){
                boolean digits=true;for(int i=1;i<end;i++)digits &= Character.isDigit(line.charAt(i));
                if(digits)return line.substring(end+5);
            }
        }
        return line;
    }
    private static long timestamp(String line,long previous){
        if(line.startsWith("[")){
            int end=line.indexOf(" ms] ");
            if(end>1&&end<24)try{return Long.parseLong(line.substring(1,end));}catch(NumberFormatException ignored){}
        }
        return previous;
    }

    /** Reads even old verbose logs sequentially; never loads the whole file into memory. */
    static void export(InputStream input,OutputStream destination)throws IOException{
        BufferedReader reader=new BufferedReader(new InputStreamReader(input,StandardCharsets.UTF_8),65536);
        BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(destination,StandardCharsets.UTF_8),65536);
        writer.write("*** LOG COMPATTO ***\nErrori, parametri e procedure conservati. I messaggi ripetitivi di fotogrammi e pacchetti sono raggruppati.\nIl file originale nell’app non viene modificato.\n\n");
        CompactLog compact=new CompactLog(text->{try{writer.write(text);writer.newLine();}catch(IOException e){throw new UncheckedIOException(e);}});
        long now=0;
        try{
            String line;while((line=reader.readLine())!=null){now=timestamp(line,now);compact.accept(line,now);}
            compact.finish();writer.flush();
        }catch(UncheckedIOException e){throw e.getCause();}
    }
}
