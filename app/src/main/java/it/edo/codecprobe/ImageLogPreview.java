package it.edo.codecprobe;

/** Screen preview only; complete events remain in the disk log and exported files. */
final class ImageLogPreview {
    static String text(String log){int first=Math.max(0,log.length()-4000),lines=0;for(int i=log.length()-1;i>=first;i--)if(log.charAt(i)=='\n'&&++lines==16){first=i+1;break;}if(first>0&&first<log.length()&&Character.isLowSurrogate(log.charAt(first))&&Character.isHighSurrogate(log.charAt(first-1)))first++;return log.substring(first);}
    private ImageLogPreview(){}
}
