package it.edo.codecprobe;
import org.junit.Test;
import static org.junit.Assert.*;
public class DashboardMetricsTest {
    @Test public void clockContinuesInBackgroundAndFreezesAtEnd(){
        assertEquals("01:01:01",DashboardMetrics.elapsed(1000,0,3662000));
        assertEquals("00:01:05",DashboardMetrics.elapsed(1000,66000,3662000));
        assertEquals("00:00:00",DashboardMetrics.elapsed(0,0,10000));
    }
    @Test public void colorsHaveExplicitBoundariesAndUnknownValues(){
        assertEquals(0,DashboardMetrics.cpuBand(29.9));assertEquals(1,DashboardMetrics.cpuBand(30));
        assertEquals(2,DashboardMetrics.cpuBand(70));assertEquals(3,DashboardMetrics.cpuBand(90));assertEquals(-1,DashboardMetrics.cpuBand(-1));
        assertEquals(0,DashboardMetrics.batteryBand(34.9));assertEquals(1,DashboardMetrics.batteryBand(35));
        assertEquals(2,DashboardMetrics.batteryBand(40));assertEquals(3,DashboardMetrics.batteryBand(45));assertEquals(-1,DashboardMetrics.batteryBand(Double.NaN));
    }
    @Test public void serviceSnapshotKeepsClockAndStartedConfiguration(){
        ConversionService.State source=new ConversionService.State();source.startedAt=1000;source.endedAt=66000;source.requestedCrf=28;source.requestedCpu=2;source.requestedPreset="slow";
        ConversionService.State copy=new ConversionService.State(source);source.requestedCrf=18;source.startedAt=9000;
        assertEquals(28,copy.requestedCrf);assertEquals(2,copy.requestedCpu);assertEquals("slow",copy.requestedPreset);assertEquals(1000,copy.startedAt);assertEquals(66000,copy.endedAt);
    }
}
