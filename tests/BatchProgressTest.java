package it.edo.codecprobe;

public final class BatchProgressTest {
    private static void expect(int wanted,int actual){if(wanted!=actual)throw new AssertionError(wanted+" != "+actual);}
    public static void main(String[] args){
        expect(0,BatchProgress.percent(0,0,0));
        expect(50,BatchProgress.percent(0,50,1));
        expect(100,BatchProgress.percent(1,0,1));
        expect(25,BatchProgress.percent(0,50,2));
        expect(50,BatchProgress.percent(1,0,2));
        expect(75,BatchProgress.percent(1,50,2));
        expect(100,BatchProgress.percent(2,0,2));
        expect(100,BatchProgress.percent(Integer.MAX_VALUE,100,100));
        expect(0,BatchProgress.percent(-1,-10,2));
        int last=-1;
        for(int file=0;file<BatchProgress.MAX_FILES;file++)for(int percent=0;percent<=100;percent++){
            int value=BatchProgress.percent(file,percent,BatchProgress.MAX_FILES);
            if(value<last||value>100)throw new AssertionError("Queue progress must not regress");last=value;
        }
        expect(100,last);System.out.println("Batch progress: boundaries, max queue and monotonic file transitions OK");
    }
}
