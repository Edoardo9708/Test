package it.edo.codecprobe;

import android.os.*;
import android.view.*;
import android.widget.*;

/**
* View construction only; actions are bound by HomeConversion.
*/
final class HomeConversionSection {
    final ConversionDashboard dashboard;
    Button convert;
    Button stop;
    Button pause;
    Button skip;
    Button savedSession;
    TextView sessionHint;
    LinearLayout playbackActions;
    Button retryFailed;
    CheckBox onlySmaller;
    TextView activeOnlySmaller;
    CheckBox keepScreenOn;
    TextView queueLabel;
    TextView elapsedLabel;
    TextView currentFile;
    TextView percentLabel;
    TextView frameLabel;
    TextView timeline;
    TextView status;
    TextView activePreset;
    TextView activeCrf;
    TextView activeCpu;
    TextView activeBframes;
    TextView activeLookahead;
    TextView activeTree;
    TextView activeAq;
    TextView activeStrength;
    TextView cpuValue;
    TextView ramValue;
    TextView batteryValue;
    TextView sizeReportTitle;
    TextView sizeReport;
    LinearLayout sizeReportPanel;
    LinearLayout failureReport;
    ProgressBar progress;

    HomeConversionSection(AppUi ui, LinearLayout work, AppSettings defaults, Bundle state) {
        LinearLayout actions = new LinearLayout(ui.activity);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        work.addView(actions, new LinearLayout.LayoutParams(-1, -2));
        convert = ui.halfButton(actions, "Converti e salva", true);
        stop = ui.halfButton(actions, "Annulla", false);
        stop.setEnabled(false);
        ui.danger(stop);
        playbackActions=ui.row(work);
        pause=ui.halfButton(playbackActions,"Pausa",false);
        skip=ui.halfButton(playbackActions,"Salta video",false);
        ui.danger(skip);
        playbackActions.setVisibility(View.GONE);
        savedSession=ui.button(work,"Salva e interrompi");
        savedSession.setVisibility(View.GONE);
        sessionHint=ui.text(work,"",14);sessionHint.setTextColor(ui.muted);sessionHint.setVisibility(View.GONE);
        ConversionOptionsView options=new ConversionOptionsView(ui,work,defaults.keepScreenOn,defaults.onlySmaller);keepScreenOn=options.screenOn;onlySmaller=options.smaller;
        dashboard=new ConversionDashboard(ui,work,new String[]{"Fotogrammi","Secondi di video"},new String[]{"Solo se più piccolo","Preset","CRF","Modalità CPU","B-frame","Lookahead","CU-tree","AQ mode","AQ strength"});
        queueLabel=dashboard.summary;elapsedLabel=dashboard.elapsed;currentFile=dashboard.current;percentLabel=dashboard.percent;progress=dashboard.progress;frameLabel=dashboard.measures.get("Fotogrammi");timeline=dashboard.measures.get("Secondi di video");status=dashboard.status;
        activeOnlySmaller=dashboard.parameters.get("Solo se più piccolo");activePreset=dashboard.parameters.get("Preset");activeCrf=dashboard.parameters.get("CRF");activeCpu=dashboard.parameters.get("Modalità CPU");activeBframes=dashboard.parameters.get("B-frame");activeLookahead=dashboard.parameters.get("Lookahead");activeTree=dashboard.parameters.get("CU-tree");activeAq=dashboard.parameters.get("AQ mode");activeStrength=dashboard.parameters.get("AQ strength");cpuValue=dashboard.cpu;ramValue=dashboard.ram;batteryValue=dashboard.battery;
        retryFailed=ui.button(work,"Riprova i falliti");retryFailed.setVisibility(View.GONE);
        WorkReportView report = new WorkReportView(ui, work);
        sizeReportPanel = report.panel;
        sizeReportTitle = report.title;
        sizeReport = report.summary;
        failureReport = report.rows;
        failureReport.setVisibility(View.GONE);
        sizeReportPanel.setVisibility(View.GONE);
    }
}
