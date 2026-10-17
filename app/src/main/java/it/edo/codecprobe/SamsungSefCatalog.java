package it.edo.codecprobe;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Names/types from ExifTool Samsung.pm and Android Media3 SefReader.
 * A recognized name is not permission to copy an unknown payload schema. */
final class SamsungSefCatalog {
    static final class Entry {
        final int type,alternate;
        final String description,schema,reason;
        Entry(int type,int alternate,String description,String schema,String reason){this.type=type;this.alternate=alternate;this.description=description;this.schema=schema;this.reason=reason;}
        boolean matches(int value){return type==value||alternate!=0&&alternate==value;}
    }
    private static final Map<String,Entry> ENTRIES=build();
    static Entry find(String name){return ENTRIES.get(name);}
    static int size(){return ENTRIES.size();}
    static int supported(){int n=0;for(Entry e:ENTRIES.values())if(e.schema!=null)n++;return n;}
    private static void add(Map<String,Entry> m,int type,String name,String description,String schema,String reason){
        if(m.put(name,new Entry(type,0,description,schema,reason))!=null)throw new IllegalStateException("Nome SEF duplicato: "+name);
    }
    private static Map<String,Entry> build(){
        Map<String,Entry> m=new LinkedHashMap<>();
        add(m,0x08b0,"HighlightVideo_Data","Selezione automatica momenti salienti","highlight",null);
        add(m,0x0c51,"Samsung_Capture_Info","Origine della registrazione schermo","capture",null);
        add(m,0x0a01,"Image_UTC_Data","Timestamp Unix in millisecondi","utc",null);
        add(m,0x0aa1,"MCC_Data","Codice paese della rete mobile (non coordinate GPS)","mcc",null);
        add(m,0x09e0,"Burst_Shot_Info","Identificativo della raffica fotografica","burst",null);
        add(m,0x0c61,"Camera_Capture_Mode_Info","Codice modalità di acquisizione","mode",null);
        add(m,0x0910,"Front_Cam_Selfie_Info","Indicatore fotocamera frontale","selfie",null);
        add(m,0x08e0,"Panorama_Shot_Info","Indicatore panorama","panorama",null);
        add(m,0xa050,"Jpeg360_2D_Info","Indicatore immagine a 360 gradi","jpeg360",null);
        add(m,0x0e41,"Video_Edited_UTC_Offset","Fuso orario del video modificato","timezone",null);
        add(m,0x7ffe,"Invalid_Data","Record opaco: supportata solo la variante osservata di 13 byte nulli","invalid-zero13",null);

        String binary="Contenuto binario/proprietario: struttura interna e rilocazione non verificate";
        String edits="Istruzioni di modifica: possibili riferimenti a originale, dimensioni o timeline da verificare";
        String embedded="Contenuto multimediale aggiuntivo: collegamenti e semantica dopo ricodifica non verificati";
        add(m,0x0ba1,"Original_Path_Hash_Key","Riferimento/chiave del file originale",null,"Formato della chiave o del percorso originale non ancora verificato; il tipo 0x0ba1 è condiviso da più nomi");
        add(m,0x0ba1,"PhotoEditor_Re_Edit_Data","Dati di rimodifica dell'editor Samsung",null,edits+"; ExifTool descrive JSON con originalPath, clipInfoValue, rotazione e altri campi");
        for(String name:new String[]{"deco_doodle_bitmap","deco_sticker_bitmap","deco_text_bitmap"})add(m,0x0ba1,name,"Livello grafico dell'editor Samsung",null,embedded);
        add(m,0x0ba2,"Copy_Available_Edit_Info","Disponibilità delle modifiche copiabili",null,edits);
        add(m,0x0b51,"Intelligent_PhotoEditor_Data","Modifiche intelligenti dell'editor",null,edits);
        add(m,0x0b60,"UltraWide_PhotoEditor_Data","Correzioni dell'editor per ultragrandangolo",null,edits);
        add(m,0x0bf0,"Remaster_Info","Informazioni di rimasterizzazione",null,edits);
        add(m,0x0d91,"PEg_Info","Dati editor Samsung (JSON)",null,edits);
        add(m,0x0a41,"BackupRestore_Data","Informazioni ripristino originale",null,edits);
        add(m,0x0ce1,"Gallery_DC_Data","Dati elaborazione della galleria",null,edits);
        add(m,0x08c0,"Auto_Enhance_Info","Parametri miglioramento automatico",null,edits);
        add(m,0x0b30,"Camera_Sticker_Info","Sticker della fotocamera",null,edits);
        add(m,0x0c81,"Watermark_Info","Filigrana Samsung",null,edits);
        add(m,0x0b90,"Document_Scan_Info","Informazioni scansione documento",null,edits);
        add(m,0x0a30,"MotionPhoto_Data","Video Motion Photo o riferimento mpv2",null,"Può contenere offset assoluto e dimensione del video incorporato; non si può rilocare senza analizzarli");
        add(m,0x0a31,"MotionPhoto_Version","Versione Motion Photo",null,"Versione riconosciuta per nome; contenuto e collegamento alla Motion Photo non verificati");
        add(m,0x0a33,"MotionPhoto_AutoPlay","Video di riproduzione automatica Motion Photo",null,embedded);
        add(m,0x0001,"DualShot_1","Prima immagine incorporata",null,embedded);
        add(m,0x0001,"DualShot_2","Seconda immagine incorporata",null,embedded);
        add(m,0x0001,"SingleShot","Immagine incorporata",null,embedded);
        add(m,0x0001,"Auto_Enhance_Unprocessed","Immagine prima del miglioramento",null,embedded);
        add(m,0x0100,"SoundShot_000","Audio Sound & Shot",null,embedded);
        add(m,0x0800,"SoundShot_Meta_Info","Descrizione Sound & Shot",null,binary);
        add(m,0x0201,"Interactive_Panorama_000","Video panorama interattivo",null,embedded);
        add(m,0x08d0,"Interactive_Panorama_Info","Descrizione panorama interattivo",null,binary);
        add(m,0x08e1,"Motion_Panorama_Info","Descrizione panorama animato",null,binary);
        add(m,0x0a20,"FlipPhoto_002","Immagine doppia fotocamera",null,embedded);
        add(m,0x0ab0,"DualShot_Meta_Info","Metadati doppia fotocamera",null,binary);
        add(m,0x0ab3,"DualShot_Extra_Info","Dimensioni e altri dati doppia fotocamera",null,binary);
        add(m,0x0ab4,"DualShot_Core_Info","Dati principali doppia fotocamera",null,binary);
        add(m,0x0b40,"SingleShotMeta","Dati ritratto e dimensioni immagine",null,binary);
        add(m,0x0b41,"SingleShot_DepthMap_1","Mappa profondità bitmap",null,binary);
        for(int i=1;i<=6;i++){
            add(m,0x0ab1,"DualShot_DepthMap_"+i,"Mappa profondità doppia fotocamera",null,binary);
            add(m,0x0b41,"SingeShot_DepthMap_"+i,"Mappa profondità (nome Samsung SingeShot)",null,binary);
        }
        add(m,0x0bc0,"Single_Relighting_Bokeh_Info","Ritratto con illuminazione singola",null,binary);
        add(m,0x0bd0,"Dual_Relighting_Bokeh_Info","Ritratto con illuminazione doppia",null,binary);
        add(m,0x0be0,"Livefocus_JDM_Info","Profondità e Live Focus",null,binary);
        add(m,0x0c21,"Portrait_Effect_Info","Effetti ritratto",null,edits);
        add(m,0x09e1,"BurstShot_Best_Photo_Info","Selezione migliore foto della raffica",null,binary);
        add(m,0x09f0,"Pro_Mode_Info","Modalità Pro della fotocamera",null,binary);
        add(m,0x0c71,"Pro_White_Balance_Info","Bilanciamento del bianco Pro",null,binary);
        add(m,0x0cc1,"Color_Display_P3","Informazioni Display P3",null,"Semantica colore e collegamento al nuovo video non verificati");
        add(m,0x0cd2,"Photo_HDR_Info","Dati HDR fotografici",null,"Dati fotografici non assimilabili automaticamente ai metadati HDR del video");
        add(m,0x0d01,"Camera_Scene_Info2","Analisi scena fotocamera variante 2",null,binary);
        add(m,0x0d01,"Camera_Scene_Info3","Analisi scena fotocamera variante 3",null,binary);
        m.put("Camera_Scene_Info",new Entry(0x0d01,0x0d21,"Analisi scena fotocamera",null,binary));
        add(m,0x0d11,"Video_Snapshot_Info","Fotogramma estratto da un video",null,binary);
        add(m,0x0d31,"Food_Blur_Effect_Info","Sfocatura modalità cibo",null,binary);
        add(m,0x0da1,"Captured_App_Info","Applicazione di origine acquisizione",null,binary);
        add(m,0x0ac0,"ZoomInOut_Info","Informazioni zoom",null,binary);
        String speed="Dati rallentatore: timeline/velocità di riproduzione da verificare prima della copia";
        add(m,0x0890,"SlowMotion_Data","Intervalli temporali e fattori rallentatore standard","slow-motion",null);
        add(m,0x0b00,"Super_SlowMotion_Data","Dati super rallentatore",null,speed);
        add(m,0x0b01,"Super_SlowMotion_BGM","Audio super rallentatore",null,embedded);
        add(m,0x0b03,"Super_SlowMotion_Edit_Data","Modifiche super rallentatore",null,speed);
        add(m,0x0b04,"Super_SlowMotion_Deflickering_On","Correzione sfarfallio rallentatore",null,speed);
        return Collections.unmodifiableMap(m);
    }
    private SamsungSefCatalog(){}
}
