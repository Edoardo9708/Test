package it.edo.codecprobe;

/** Exact conversion of provider numbers, independent of their boxed Java type. */
final class NumberValues {
    private NumberValues() {}
    static long longExact(Number n,String field) {
        if(n==null)throw new IllegalArgumentException("Campo numerico assente: "+field);
        if(n instanceof Byte||n instanceof Short||n instanceof Integer||n instanceof Long)return n.longValue();
        double d=n.doubleValue();
        if(!Double.isFinite(d)||d!=Math.rint(d)||d < -0x1p63||d >= 0x1p63)
            throw new IllegalArgumentException("Campo "+field+" non rappresentabile come intero: "+n);
        return (long)d;
    }
    static int intExact(Number n,String field) {
        long v=longExact(n,field);
        if(v<Integer.MIN_VALUE||v>Integer.MAX_VALUE)
            throw new IllegalArgumentException("Campo "+field+" fuori dal range intero: "+n);
        return (int)v;
    }
}
