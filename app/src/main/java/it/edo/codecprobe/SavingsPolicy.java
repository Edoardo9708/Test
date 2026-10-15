package it.edo.codecprobe;

/** Whole finalized-file comparison, before creating any output document. */
final class SavingsPolicy {
    enum Decision { SAVE, NO_SAVING, UNKNOWN_SIZE }
    static Decision decide(boolean enabled,long original,long converted) {
        if(!enabled)return Decision.SAVE;
        if(original<=0||converted<=0)return Decision.UNKNOWN_SIZE;
        return converted<original?Decision.SAVE:Decision.NO_SAVING;
    }
    static final class NotSaved extends Exception {
        final long original,converted;final Decision decision;
        NotSaved(Decision decision,long original,long converted){
            super(decision==Decision.NO_SAVING?"Nessun risparmio · risultato non salvato":"Confronto dimensioni non disponibile · risultato non salvato");
            this.decision=decision;this.original=original;this.converted=converted;
        }
    }
    private SavingsPolicy(){}
}
