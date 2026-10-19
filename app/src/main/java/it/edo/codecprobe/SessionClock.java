package it.edo.codecprobe;

/** One elapsed-time formula for the dashboard, its timer and the persistent journal. */
final class SessionClock {
    static long millis(ConversionService.State state,long now){
        return state.elapsedBefore+(state.startedAt>0?Math.max(0,(state.endedAt>0?state.endedAt:now)-state.startedAt):0);
    }
    static String text(ConversionService.State state,long now){return DashboardMetrics.elapsed(1,1+millis(state,now),0);}
    static String duration(long millis){return DashboardMetrics.elapsed(1,1+Math.max(0,millis),0);}
    private SessionClock(){}
}
