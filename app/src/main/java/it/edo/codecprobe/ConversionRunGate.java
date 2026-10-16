package it.edo.codecprobe;

/** Process-wide lease: a destroyed service keeps ownership until its worker has cleaned up. */
final class ConversionRunGate {
    private long sequence,owner;
    synchronized long acquire(){if(owner!=0)return 0;owner=++sequence;return owner;}
    synchronized boolean owns(long token){return token!=0&&owner==token;}
    synchronized void release(long token){if(owns(token))owner=0;}
}
