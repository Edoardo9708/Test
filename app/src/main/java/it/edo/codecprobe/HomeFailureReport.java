package it.edo.codecprobe;

import android.app.AlertDialog;
import android.view.View;
import java.util.List;

/** Per-file failure actions and readable, copyable error dialog. No log scanning. */
final class HomeFailureReport {
    private final HomeController home;
    private String rendered = "";
    private List<ConversionFailure> renderedFailures = java.util.Collections.emptyList();
    private List<String> renderedOutputs = java.util.Collections.emptyList();
    AlertDialog dialog;
    private final ResultDialog popup;

    HomeFailureReport(HomeController home) { this.home = home;popup=new ResultDialog(home.ui,home::toast); }

    void render(ConversionService.State state) {
        HomeConversionSection views = home.screen.conversion;
        boolean structured = !state.failures.isEmpty() || !state.results.isEmpty();
        views.sizeReport.setVisibility(structured ? View.GONE : View.VISIBLE);
        views.failureReport.setVisibility(structured ? View.VISIBLE : View.GONE);
        if (!structured) { views.failureReport.removeAllViews(); rendered = ""; renderedFailures = java.util.Collections.emptyList(); renderedOutputs=java.util.Collections.emptyList(); return; }
        if (rendered.equals(state.sizeReport) && renderedFailures.equals(state.failures) && renderedOutputs.equals(state.outputUris)) return;
        rendered = state.sizeReport;
        renderedFailures = new java.util.ArrayList<>(state.failures);
        renderedOutputs = new java.util.ArrayList<>(state.outputUris);
        views.failureReport.removeAllViews();
        for (String paragraph : state.sizeReport.split("\n\n")) {
            ConversionFailure found = null;
            for (ConversionFailure failure : state.failures)
                if (paragraph.startsWith(failure.title() + "\nConversione fallita")) { found = failure; break; }
            Runnable open=null;
            for(int n=0;n<state.items.size()&&n<state.outputUris.size();n++){
                CompressionSummary.Item item=state.items.get(n);String title="Video "+item.number+" · "+item.name.replace('\n',' ').replace('\r',' ');
                if(item.status==CompressionSummary.Status.COMPLETE&&!state.outputUris.get(n).isEmpty()&&paragraph.startsWith(title+"\n")){
                    final android.net.Uri output=android.net.Uri.parse(state.outputUris.get(n));open=()->home.openResult(output);break;
                }
            }
            final ConversionFailure failure=found;
            ResultReportRow.add(home.ui,views.failureReport,paragraph,open,failure==null?null:()->show(failure));
        }
    }

    void show(ConversionFailure failure) {
        dialog=popup.show("Conversione fallita",failure.title(),failure.message,failure.lastState,failure.details,failure.copyText());
    }
    void close(){popup.close();dialog=null;}
}
