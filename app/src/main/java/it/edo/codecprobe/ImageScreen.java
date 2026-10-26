package it.edo.codecprobe;

import android.os.Bundle;
import android.widget.*;

/** Only the real progress and diagnostic Views embedded in Compose. */
final class ImageScreen {
    static final String FILES="01  FOTO E DESTINAZIONE",COMPRESSION="02  COMPRESSIONE IMMAGINI",HEIC="03  AVANZAMENTO IMMAGINI",DETAILS="DIAGNOSTICA";
    final AppUi ui;
    final ImageSteps sections;
    final Button retryFailures,export,checkMetadata,convert,cancel,pause,resumeSession,discardSession,exportLog,exportErrors,clearLog,toggleLog;
    final CheckBox screenOn;
    final ProgressBar progress;
    final TextView runStatus,log;
    final WorkReportView workReport;
    final ConversionDashboard dashboard;
    ImageScreen(AppUi ui,Bundle saved){
        this.ui=ui;sections=new ImageSteps(saved);
        LinearLayout output=ImageSteps.group(ui,sections.panel(ui,HEIC));
        LinearLayout screenRow=ui.row(output);screenOn=new PhotoSwitch(ui);screenOn.setText("Mantieni schermo acceso");screenRow.addView(screenOn,new LinearLayout.LayoutParams(0,-2,1));
        ui.infoButton(screenRow,"Mantieni schermo acceso","Evita lo spegnimento automatico durante la conversione mentre questa schermata è visibile. Puoi comunque spegnere lo schermo con il tasto del telefono. La scelta viene ricordata e non garantisce una velocità maggiore.");
        checkMetadata=ui.button(output,"Verifica compatibilità dei metadati");retryFailures=ui.button(output,"Carica foto fallite");
        ui.text(output,"La verifica legge i metadati senza convertire. I pixel saranno verificati durante la conversione.",13);
        LinearLayout buttons=ui.row(output);convert=ui.halfButton(buttons,"Converti e salva",true);cancel=ui.halfButton(buttons,"Annulla",false);ui.danger(cancel);pause=ui.button(output,"Pausa");
        LinearLayout recovery=ui.row(output);resumeSession=ui.halfButton(recovery,"Riprendi coda interrotta",true);discardSession=ui.halfButton(recovery,"Scarta coda interrotta",false);
        ui.text(output,"Pausa completa la foto in corso, poi attende. Riprendi continua dalla foto successiva, senza ricreare la coda.",13);

        dashboard=new ConversionDashboard(ui,output,new String[]{},new String[]{"Solo se più piccolo","Qualità","CRF","Preset","Tune","Formato","Dimensioni"});progress=dashboard.progress;runStatus=dashboard.status;
        workReport=new WorkReportView(ui,output);
        LinearLayout diagnostic=ImageSteps.group(ui,sections.panel(ui,DETAILS)),exports=ui.row(diagnostic);exportLog=ui.halfButton(exports,"Esporta log",false);export=ui.halfButton(exports,"Metadati foto · JSON",false);
        exportErrors=ui.button(diagnostic,"Esporta archivio errori");
        ui.text(diagnostic,"Puoi esportare log e archivio errori anche durante la conversione. Viene salvata una copia degli eventi già registrati al momento della richiesta; la registrazione prosegue mentre scegli la destinazione. Il file in corso potrebbe non avere ancora un esito nel log.",13);
        ui.text(diagnostic,"Il report include i blocchi metadati originali, che possono contenere posizione GPS e altri dati personali. Non include i pixel della foto.",13);
        ui.parameterRow(diagnostic,"Dettagli del motore immagini","JPEG YCbCr a 8 bit, lati 64–16384 pixel, massimo 48 megapixel e file di 64 MiB. Decodifica JPEG e verifica HEIC con FFmpeg incluso; contenitore libheif, codifica x265. Nessun ridimensionamento, rotazione dei pixel o normalizzazione. Conversione con perdita. EXIF/ICC standard conservati e riletti; XMP conservato o adattato per Motion Photo, con pacchetto originale archiviato; i blocchi APP/COM originali e il SEF foto supportato sono conservati anche in un archivio interno. Le funzioni dell’editor Samsung non sono garantite. Altri APP/COM proprietari restano archiviati senza interpretazione. Foto principali CMYK/RGB o in scala di grigi, HDR/MPF diversi dalla mappa Apple v1 riconosciuta, XMP esteso e SEF non supportato vengono rifiutati. Mappa HDR Apple v1: immagine ausiliaria monocromatica HEVC lossless, XMP originale e collegamento auxl/auxC; pixel della mappa riletti e confrontati esattamente. JPEG ausiliario originale conservato anche nell’archivio. Resa HDR da verificare sul dispositivo. APP11/JUMBF C2PA riconosciuto: manifesto originale archiviato, firma non verificata; nessuna certificazione C2PA del nuovo HEIC. Motion Photo JPEG con video MP4 SEF: filmato originale copiato senza ricodifica nel box HEIC mpvd finale; XMP aggiornato e archivio CPJPEG2/3 senza duplicazione del video. SEF massimo 52 MiB complessivi, video Motion Photo massimo 48 MiB, nel limite file di 64 MiB. Per code oltre 4 MiB (video escluso), archivio UUID CPJPEG3 separato e verificato, senza copia nell’item metadati HEIF. Prefisso SEF tutto zero ammesso fino a 64 KiB; buchi interni e sovrapposizioni rifiutati. Playback da verificare nella galleria del dispositivo. EXIF orientamento 0: nessuna rotazione applicata, valore originale conservato.\n\nSelezione manuale fino a 5.000 foto. Importazione automatica della cartella JPEG, senza sottocartelle: nessuna selezione manuale dei lotti. Il registro su disco mantiene un solo esito per originale; originale e risultato già presenti vengono verificati con SHA-256 prima di evitare una nuova codifica. «Carica fallite» riporta in selezione gli ultimi fallimenti del registro, con le destinazioni conservate e le impostazioni di compressione correnti. Carosello: prime 100 e puntini. Risultati: finestra di 100 righe; la corrente è al 50º posto quando ci sono abbastanza elementi prima e dopo. Totali cumulativi su tutta la coda. Log completo dell’ultima esecuzione su disco, anteprima di 24.000 caratteri in RAM. Archivio errori persistente ed esportabile, separato dal report. La lettura dei metadati di anteprima non blocca l’avvio; ogni foto viene controllata integralmente prima della codifica.\n\nLa coda continua uscendo dalla pagina. Se Android termina il processo, «Riprendi coda interrotta» recupera impostazioni, posizione e totali dall’ultimo salvataggio. I dettagli precedenti restano nel log. Interrompi agisce alla fine della fase nativa in corso; non interrompe x265 a metà fotogramma.");
        LogPanelView logPanel=new LogPanelView(ui,diagnostic,"Cancella log immagini","Cancella il log immagini nell’app. Le foto, il resoconto e i TXT già esportati restano salvati.");clearLog=logPanel.clear;toggleLog=logPanel.toggle;log=logPanel.log;
        Button diagnosticButton=ui.button(output,"Diagnostica e log");diagnosticButton.setOnClickListener(v->sections.focusCard(DETAILS));
    }
}
