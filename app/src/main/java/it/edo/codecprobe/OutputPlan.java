package it.edo.codecprobe;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Queue-local reservations: existing files and earlier outputs occupy the same namespace. */
final class OutputPlan {
    final ArrayList<String> names=new ArrayList<>();
    final boolean[] omitted;
    private final Map<String,Set<String>> occupied=new HashMap<>();
    OutputPlan(int size) { omitted=new boolean[size]; }
    void directory(String key, Set<String> names) {
        occupied.computeIfAbsent(key,k->{Set<String> copy=new HashSet<>();
            for(String name:names)copy.add(name.toLowerCase(Locale.ROOT));return copy;});
    }
    boolean containsDirectory(String key) { return occupied.containsKey(key); }
    boolean conflicts(String key,String name) { return occupied.get(key).contains(name.toLowerCase(Locale.ROOT)); }
    String suggestion(String key,String name) { return OutputName.unique(name,occupied.get(key)); }
    void accept(String key,String name) { occupied.get(key).add(name.toLowerCase(Locale.ROOT));names.add(name); }
    void skip() { omitted[names.size()]=true;names.add(""); }
    static String manual(String value) {
        String name=value.trim();
        if(name.isEmpty()||name.matches(".*[\\p{Cntrl}/\\\\:*?\"<>|].*"))
            throw new IllegalArgumentException("Inserisci un nome valido, senza percorsi o caratteri speciali.");
        if(!name.toLowerCase(Locale.ROOT).endsWith(".mp4"))name+=".mp4";
        if(name.equalsIgnoreCase(".mp4")||name.getBytes(StandardCharsets.UTF_8).length>240)
            throw new IllegalArgumentException("Il nome è vuoto o troppo lungo (massimo 240 byte).");
        return name;
    }
}
