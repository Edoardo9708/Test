package it.edo.codecprobe;

public final class NumberValuesTest {
    static void rejected(Number value,boolean asLong) {
        try {if(asLong)NumberValues.longExact(value,"test");else NumberValues.intExact(value,"test");}
        catch(IllegalArgumentException expected){return;}
        throw new AssertionError("Invalid value accepted: "+value);
    }
    public static void main(String[] args) {
        // Android 16 returned Long(52) for accurate-framecount in the device log.
        if(NumberValues.intExact(Long.valueOf(52),"accurate-framecount")!=52)throw new AssertionError("Long frame count rejected");
        if(NumberValues.intExact(Integer.valueOf(52),"frame-count")!=52)throw new AssertionError();
        if(NumberValues.intExact(Float.valueOf(90),"rotation")!=90)throw new AssertionError();
        if(NumberValues.longExact(Integer.valueOf(1747200),"durationUs")!=1747200L)throw new AssertionError();
        if(NumberValues.longExact(Long.MAX_VALUE,"durationUs")!=Long.MAX_VALUE)throw new AssertionError();
        rejected(2147483648L,false);rejected(-2147483649L,false);
        rejected(29.97,false);rejected(Double.NaN,false);rejected(Double.POSITIVE_INFINITY,true);
        rejected(0x1p63,true);rejected(null,false);
        System.out.println("Provider numeric types, device frame-count regression and range checks passed");
    }
}
