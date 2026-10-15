package it.edo.codecprobe;

import java.io.IOException;
import java.time.*;
import java.time.format.*;
import java.util.Locale;

/** Dates in MP4 use seconds since 1904; DATE_ADDED uses seconds since 1970. */
final class CreationDatePolicy {
    static final long MP4_EPOCH_SECONDS=2082844800L;
    enum Embedded { MISSING, PLACEHOLDER, VALID, UNRECOGNIZED }
    static Instant parse(String value) {
        if(value==null||value.trim().isEmpty())return null;
        try{return Instant.parse(value.trim());}catch(DateTimeException ignored){}
        try{return OffsetDateTime.parse(value.trim(),DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss[.SSS]X",Locale.ROOT)
                .withResolverStyle(ResolverStyle.STRICT)).toInstant();}catch(DateTimeException ignored){return null;}
    }
    static Embedded embedded(String value) {
        if(value==null||value.trim().isEmpty())return Embedded.MISSING;
        Instant time=parse(value);
        if(time==null)return Embedded.UNRECOGNIZED;
        return time.equals(Instant.ofEpochSecond(-MP4_EPOCH_SECONDS))?Embedded.PLACEHOLDER:Embedded.VALID;
    }
    static boolean needsFallback(long movieCreation,String embeddedDate) {
        Embedded kind=embedded(embeddedDate);
        return movieCreation==0&&(kind==Embedded.MISSING||kind==Embedded.PLACEHOLDER);
    }
    static long fromAddedSeconds(long added)throws IOException {
        if(added<=0)return 0;
        try{return Math.addExact(added,MP4_EPOCH_SECONDS);}
        catch(ArithmeticException error){throw new IOException("Data DATE_ADDED Android fuori intervallo",error);}
    }
    static String displayMp4(long seconds) {
        try{return Instant.ofEpochSecond(Math.subtractExact(seconds,MP4_EPOCH_SECONDS)).toString();}
        catch(DateTimeException|ArithmeticException error){return "fuori intervallo: "+seconds;}
    }
    private CreationDatePolicy() {}
}
