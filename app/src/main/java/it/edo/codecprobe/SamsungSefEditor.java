package it.edo.codecprobe;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.*;

/** Observed Samsung NDE 1.0/2.0 project schemas. Never evaluates edits or rewrites references. */
final class SamsungSefEditor {
    static String validate(SamsungSefDirectory.Record r,String schema)throws IOException{
        if(schema.equals("original-key")){
            String key=SamsungSefSchemas.text(r);
            require(key.matches("[0-9a-f]{64}/[1-9][0-9]{0,18}"),"Chiave originale: atteso hash esadecimale/numero byte");
            positiveLong(key.substring(65));
            return "riferimento all’originale conservato; non è ricalcolato sul video ricodificato";
        }
        require(r.payloadBytes()>0&&r.payloadBytes()<=65536,"Progetto editor oltre il limite di 64 KiB");
        String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(r.bytes,r.payload,r.payloadBytes())).toString();
        Object parsed=new Json(text).parse();Map<String,Object> root=map(parsed);
        String version=string(root.get("NDE_VERSION"));
        if(version.equals("1.0"))v1(root);else if(version.equals("2.0"))v2(root);else throw new IOException("Versione NDE non verificata: "+version);
        return "progetto Samsung NDE "+version+" verificato; istruzioni e riferimenti esterni conservati byte per byte, senza applicare nuovamente le modifiche. Riapertura nell’editor Samsung da verificare sul dispositivo.";
    }
    private static void v1(Map<String,Object> root)throws IOException{
        keys(root,"NDE_VERSION","ORIGINAL_FILE_PATH","ORIGINAL_FILE_INFO","EXPORT_OPTIONS","FLAG_TRANSFORM_EXIST","transformPosition","transformScale","transformRotation","perspectivePosition","perspectiveScale","perspectiveDegree","userRotation","perspectiveMatrix","roiInfo","roiRatioXY","isMinRoi","roiAspectRatio","selectedAR","deltaRoi","initRoi","initRoiMatrix");
        originalPath(root.get("ORIGINAL_FILE_PATH"));
        Map<String,Object> original=obj(root.get("ORIGINAL_FILE_INFO"),"resolution","videoDuration");
        resolution(original.get("resolution"));positive(original.get("videoDuration"));
        Map<String,Object> export=obj(root.get("EXPORT_OPTIONS"),"res","CF","DR","BD");resolution(export.get("res"));
        options(export,"CF","DR","BD");bool(root.get("FLAG_TRANSFORM_EXIST"));
        for(String key:new String[]{"transformPosition","transformScale","perspectivePosition","perspectiveScale","perspectiveDegree"})vector(root.get(key),3);
        vector(root.get("transformRotation"),4);vector(root.get("perspectiveMatrix"),16);vector(root.get("roiInfo"),4);
        vector(root.get("roiRatioXY"),2);vector(root.get("initRoi"),6);vector(root.get("initRoiMatrix"),9);
        number(root.get("userRotation"));bool(root.get("isMinRoi"));positive(root.get("roiAspectRatio"));
        integer(root.get("selectedAR"),0,100);positive(root.get("deltaRoi"));
    }
    private static void v2(Map<String,Object> root)throws IOException{
        keys(root,"NDE_VERSION","ALLOW_REEDIT","PROJECT_INFO","EXPORT_OPTIONS","LAYERS_INFO","CONTENTS","ITEMS","ORIGINAL_FILE_PATH");
        bool(root.get("ALLOW_REEDIT"));originalPath(root.get("ORIGINAL_FILE_PATH"));
        Map<String,Object> project=obj(root.get("PROJECT_INFO"),"NAME","ASPECT_RATIO");string(project.get("NAME"));positive(project.get("ASPECT_RATIO"));
        Map<String,Object> export=obj(obj(root.get("EXPORT_OPTIONS"),"EXPORT_INFO").get("EXPORT_INFO"),"RESOLUTION","BIT_DEPTH","CODEC_FORMAT","DYNAMIC_RANGE");
        Map<String,Object> res=obj(export.get("RESOLUTION"),"WIDTH","HEIGHT");integer(res.get("WIDTH"),1,16384);integer(res.get("HEIGHT"),1,16384);
        options(export,"CODEC_FORMAT","DYNAMIC_RANGE","BIT_DEPTH");
        // Only the single video/single media layer schema observed in the diagnostic files.
        Map<String,Object> content=obj(single(root.get("CONTENTS")),"ID","CONTENTS_INFO");integer(content.get("ID"),0,0);
        Map<String,Object> info=obj(content.get("CONTENTS_INFO"),"TYPE","PATH","DISPLAY_NAME","FILE_SIZE","HASH_CODE","MEDIA_ID");
        require(string(info.get("TYPE")).equals("VIDEO"),"Tipo contenuto editor non verificato");string(info.get("DISPLAY_NAME"));
        String hash=string(info.get("HASH_CODE")),size=string(info.get("FILE_SIZE"));
        require(hash.matches("[a-f0-9]{64}"),"Hash del contenuto editor non valido");positiveLong(size);positiveLong(string(info.get("MEDIA_ID")));
        String path=string(info.get("PATH"));require(path.matches("/data/sec/photoeditor/[0-9]+/"+hash+"_"+size+"\\.mp4"),"Percorso originale non corrisponde a hash e dimensioni del contenuto");
        Map<String,Object> layer=obj(single(root.get("LAYERS_INFO")),"LAYER_ID","LAYER_INFO");integer(layer.get("LAYER_ID"),0,0);
        Map<String,Object> li=obj(layer.get("LAYER_INFO"),"TYPE","Z_ORDER","TRANSFORM","ITEMS_LIST");
        require(string(li.get("TYPE")).equals("MEDIA"),"Tipo livello editor non verificato");integer(li.get("Z_ORDER"),0,0);integer(single(li.get("ITEMS_LIST")),0,0);
        Map<String,Object> lt=obj(li.get("TRANSFORM"),"ZOOM_LEVEL","TRANSLATION","INIT_RESOLUTION_AT_SCALE_ONE","ROTATION");
        coordinates(lt.get("ZOOM_LEVEL"),false,"X","Y");coordinates(lt.get("TRANSLATION"),false,"X","Y");
        coordinates(lt.get("INIT_RESOLUTION_AT_SCALE_ONE"),false,"WIDTH","HEIGHT");coordinates(lt.get("ROTATION"),false,"X","Y","Z");
        Map<String,Object> item=obj(single(root.get("ITEMS")),"ID","EDIT_INFO");integer(item.get("ID"),0,0);
        Map<String,Object> edit=obj(item.get("EDIT_INFO"),"ITEM_TYPE","CONTENTS_ID","TRIM","VOLUME","TRANSFORM","FADE_IN_DURATION","FADE_OUT_DURATION","DEFLICKER");
        require(string(edit.get("ITEM_TYPE")).equals("VIDEO"),"Tipo elemento editor non verificato");integer(edit.get("CONTENTS_ID"),0,0);
        Map<String,Object> trim=obj(edit.get("TRIM"),"CONTENT_START_TIME","CONTENT_END_TIME","PADDING","DURATION");
        long start=integer(trim.get("CONTENT_START_TIME"),0,Integer.MAX_VALUE),end=integer(trim.get("CONTENT_END_TIME"),1,Integer.MAX_VALUE);
        require(end>start&&integer(trim.get("DURATION"),1,Integer.MAX_VALUE)==end-start,"Timeline di progetto editor non coerente");integer(trim.get("PADDING"),0,Integer.MAX_VALUE);
        integer(obj(edit.get("VOLUME"),"INTENSITY").get("INTENSITY"),0,100);
        Map<String,Object> transform=obj(edit.get("TRANSFORM"),"INIT_RESOLUTION_AT_SCALE_ONE","ZOOM_LEVEL");
        coordinates(transform.get("INIT_RESOLUTION_AT_SCALE_ONE"),true,"WIDTH","HEIGHT");coordinates(transform.get("ZOOM_LEVEL"),true,"X","Y","Z");
        integer(edit.get("FADE_IN_DURATION"),0,Integer.MAX_VALUE);integer(edit.get("FADE_OUT_DURATION"),0,Integer.MAX_VALUE);bool(edit.get("DEFLICKER"));
    }
    private static void options(Map<String,Object> m,String codec,String range,String depth)throws IOException{
        integer(m.get(codec),0,1);integer(m.get(range),0,1);long bits=integer(m.get(depth),8,10);require(bits==8||bits==10,"Profondità editor non verificata");
    }
    private static void originalPath(Object value)throws IOException{
        String[] parts=string(value).split("\\]",-1);require(parts.length==3,"Riferimento originale protetto: struttura non verificata");
        try{
            require(Base64.getDecoder().decode(parts[0]).length==32&&Base64.getDecoder().decode(parts[1]).length==16,"Riferimento originale protetto non valido");
            int n=Base64.getDecoder().decode(parts[2]).length;require(n>=16&&n<=8192&&n%16==0,"Riferimento originale protetto: dimensione non verificata");
        }catch(IllegalArgumentException error){throw new IOException("Riferimento originale protetto: BASE64 non valido",error);}
    }
    private static void coordinates(Object value,boolean text,String... names)throws IOException{
        Map<String,Object> m=obj(value,names);for(String key:names){Object v=m.get(key);if(text){String s=string(v);require(s.length()<=64,"Numero editor troppo lungo");try{v=new BigDecimal(s);}catch(NumberFormatException e){throw new IOException("Numero testuale editor non valido",e);}}number(v);}
    }
    private static void vector(Object value,int count)throws IOException{List<?> values=list(value);require(values.size()==count,"Dimensione vettore editor non verificata");for(Object v:values)number(v);}
    private static void resolution(Object value)throws IOException{List<?> values=list(value);require(values.size()==2,"Risoluzione editor non valida");for(Object v:values)integer(v,1,16384);}
    private static Object single(Object value)throws IOException{List<?> values=list(value);require(values.size()==1,"Schema editor supportato: un solo livello/contenuto/elemento");return values.get(0);}
    private static List<?> list(Object v)throws IOException{require(v instanceof List,"Atteso vettore editor");return (List<?>)v;}
    @SuppressWarnings("unchecked") private static Map<String,Object> map(Object v)throws IOException{require(v instanceof Map,"Atteso oggetto editor");return (Map<String,Object>)v;}
    private static Map<String,Object> obj(Object v,String... fields)throws IOException{Map<String,Object> m=map(v);keys(m,fields);return m;}
    private static void keys(Map<String,Object> m,String... fields)throws IOException{require(m.keySet().equals(new HashSet<>(Arrays.asList(fields))),"Campi progetto editor mancanti o non verificati");}
    private static String string(Object v)throws IOException{require(v instanceof String,"Attesa stringa editor");return (String)v;}
    private static void bool(Object v)throws IOException{require(v instanceof Boolean,"Atteso valore booleano editor");}
    private static BigDecimal number(Object v)throws IOException{require(v instanceof BigDecimal&&Double.isFinite(((BigDecimal)v).doubleValue()),"Atteso numero editor finito");return (BigDecimal)v;}
    private static void positive(Object v)throws IOException{require(number(v).signum()>0,"Atteso numero editor positivo");}
    private static long integer(Object v,long low,long high)throws IOException{try{long n=number(v).longValueExact();require(n>=low&&n<=high,"Numero intero editor fuori intervallo");return n;}catch(ArithmeticException e){throw new IOException("Numero editor non intero o fuori intervallo",e);}}
    private static void positiveLong(String value)throws IOException{require(value.matches("[1-9][0-9]{0,18}"),"Dimensione/identificativo originale non valido");try{Long.parseLong(value);}catch(NumberFormatException e){throw new IOException("Dimensione/identificativo originale fuori intervallo",e);}}
    private static void require(boolean condition,String message)throws IOException{if(!condition)throw new IOException(message);}

    /** Small strict JSON reader with explicit limits; no Android/parser-library dependency. */
    private static final class Json {
        final String text;int at,nodes;
        Json(String value){text=value;}
        Object parse()throws IOException{Object value=value(0);space();require(at==text.length(),"Dati aggiunti dopo il progetto JSON");return value;}
        void space(){while(at<text.length()&&" \r\n\t".indexOf(text.charAt(at))>=0)at++;}
        Object value(int depth)throws IOException{
            space();require(depth<=20&&++nodes<=5000&&at<text.length(),"Limite o valore JSON non valido");char c=text.charAt(at);
            if(c=='{'){
                at++;Map<String,Object> m=new LinkedHashMap<>();space();if(take('}'))return m;
                do{space();require(at<text.length()&&text.charAt(at)=='"',"Chiave JSON non valida");String key=quoted();require(!m.containsKey(key)&&m.size()<128,"Chiave JSON duplicata o troppi campi");space();require(take(':'),"Separatore JSON assente");m.put(key,value(depth+1));space();if(take('}'))return m;require(take(','),"Separatore oggetto JSON non valido");}while(true);
            }
            if(c=='['){at++;List<Object> a=new ArrayList<>();space();if(take(']'))return a;do{require(a.size()<1024,"Vettore JSON troppo grande");a.add(value(depth+1));space();if(take(']'))return a;require(take(','),"Separatore vettore JSON non valido");}while(true);}
            if(c=='"')return quoted();
            for(String word:new String[]{"true","false","null"})if(text.startsWith(word,at)){at+=word.length();return word.equals("null")?null:word.equals("true");}
            int begin=at;while(at<text.length()&&" \r\n\t,]}".indexOf(text.charAt(at))<0)at++;
            String token=text.substring(begin,at);require(token.length()<=128&&token.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?"),"Numero JSON non valido");
            try{BigDecimal n=new BigDecimal(token);require(Double.isFinite(n.doubleValue())&&Math.abs((long)n.scale())<=308,"Numero JSON fuori intervallo");return n;}catch(NumberFormatException e){throw new IOException("Numero JSON non valido",e);}
        }
        boolean take(char c){if(at<text.length()&&text.charAt(at)==c){at++;return true;}return false;}
        String quoted()throws IOException{
            require(take('"'),"Stringa JSON assente");StringBuilder out=new StringBuilder();
            while(at<text.length()){
                char c=text.charAt(at++);if(c=='"')return out.toString();require(c>=32&&out.length()<16384,"Stringa JSON non valida/troppo lunga");
                if(c=='\\'){
                    require(at<text.length(),"Escape JSON troncato");c=text.charAt(at++);
                    if(c=='u'){require(at+4<=text.length(),"Escape Unicode troncato");String code=text.substring(at,at+4);require(code.matches("[a-fA-F0-9]{4}"),"Escape Unicode non valido");c=(char)Integer.parseInt(code,16);at+=4;}
                    else{int i="\"\\/bfnrt".indexOf(c);require(i>=0,"Escape JSON non valido");c="\"\\/\b\f\n\r\t".charAt(i);}
                }
                out.append(c);
            }throw new IOException("Stringa JSON troncata");
        }
    }
    private SamsungSefEditor(){}
}
