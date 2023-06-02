/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.JOptionPane;

import org.etools.j1939tools.j1939.J1939;
import org.etools.j1939tools.j1939.packets.MonitoredSystem;
import org.etools.j1939tools.j1939.packets.PerformanceRatio;
import org.etools.j1939tools.modules.DateTimeModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.soliddesign.iumpr.modules.BannerModule;
import net.soliddesign.iumpr.modules.ComparisonModule;
import net.soliddesign.iumpr.modules.DiagnosticReadinessModule;
import net.soliddesign.iumpr.modules.EngineSpeedModule;
import net.soliddesign.iumpr.modules.MonitorTrackingModule;
import net.soliddesign.iumpr.modules.ReportFileModule;
import net.soliddesign.iumpr.modules.VehicleInformationModule;

/**
 * Unit tests for the {@link MonitorCompletionController} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", justification = "It's complaining the verify steps")
@RunWith(MockitoJUnitRunner.class)
public class MonitorCompletionControllerTest {

    @Mock
    private BannerModule bannerModule;

    @Mock
    private ComparisonModule comparisonModule;

    @Mock
    private DateTimeModule dateTimeModule;

    @Mock
    private DiagnosticReadinessModule diagnosticReadinessModule;

    @Mock
    private EngineSpeedModule engineSpeedModule;

    @Mock
    private ScheduledExecutorService executor;

    private MonitorCompletionController instance;

    @Mock
    private J1939 j1939;

    @Mock
    private ResultsListener listener;

    @Mock
    private MonitorTrackingModule monitorTrackingModule;

    @Mock
    private ReportFileModule reportFileModule;

    @Mock
    private VehicleInformationModule vehicleInformationModule;

    @Before
    public void setUp() throws Exception {
        when(bannerModule.getTypeName()).thenReturn("Monitor Tracking");
        instance = new MonitorCompletionController(executor, engineSpeedModule, bannerModule, dateTimeModule,
                vehicleInformationModule, diagnosticReadinessModule, comparisonModule, monitorTrackingModule);
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(engineSpeedModule, bannerModule, vehicleInformationModule, diagnosticReadinessModule,
                dateTimeModule, comparisonModule, j1939, reportFileModule, listener, executor);
    }

    @Test
    public void testAbortIfVehicleMismatch() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);
        when(comparisonModule.compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2), eq(17)))
                .thenReturn(false);

        instance.execute(listener, j1939, reportFileModule);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        InOrder inOrder = inOrder(listener, engineSpeedModule, comparisonModule, bannerModule,
                reportFileModule, vehicleInformationModule, diagnosticReadinessModule);

        inOrder.verify(vehicleInformationModule).setJ1939(j1939);
        inOrder.verify(diagnosticReadinessModule).setJ1939(j1939);
        inOrder.verify(comparisonModule).setJ1939(j1939);
        inOrder.verify(engineSpeedModule).setJ1939(j1939);

        inOrder.verify(listener).onProgress(0, 17, "");
        inOrder.verify(reportFileModule).onProgress(0, 17, "");

        inOrder.verify(listener).onProgress(1, 17, "Reading Engine Speed");
        inOrder.verify(reportFileModule).onProgress(1, 17, "Reading Engine Speed");
        inOrder.verify(engineSpeedModule).isEngineCommunicating();

        inOrder.verify(listener).onProgress(2, 17, "Comparing Vehicle to Report File");
        inOrder.verify(reportFileModule).onProgress(2, 17, "Comparing Vehicle to Report File");
        inOrder.verify(comparisonModule).reset();
        inOrder.verify(comparisonModule).compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(17));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(bannerModule).reportAborted(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");

        inOrder.verify(bannerModule).getTypeName();
        inOrder.verify(listener).onProgress(17, 17, "Monitor Tracking Aborted");
        inOrder.verify(reportFileModule).onProgress(17, 17, "Monitor Tracking Aborted");
        inOrder.verify(listener).onComplete(false);
        inOrder.verify(reportFileModule).onComplete(false);
    }

    @Test
    public void testAbortWithoutEngineComm() throws Exception {
        final boolean[] lock = new boolean[] { false };

        when(engineSpeedModule.isEngineCommunicating()).thenAnswer(arg0 -> {
            Thread.sleep(1);
            synchronized (lock) {
                lock[0] = true;
                lock.notify();
            }
            return false;
        });

        instance.execute(listener, j1939, reportFileModule);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(runnableCaptor.capture());

        new Thread(() -> {
            runnableCaptor.getValue().run();
            synchronized (lock) {
                lock[0] = true;
                lock.notify();
            }
        }).start();

        synchronized (lock) {
            if (!lock[0]) {
                lock.wait(1000);
                lock[0] = false;
            }
        }

        instance.stop();

        synchronized (lock) {
            if (!lock[0]) {
                lock.wait(100);
            }
        }

        verify(j1939).interrupt();
        InOrder inOrder = inOrder(listener, engineSpeedModule, comparisonModule, bannerModule,
                reportFileModule, vehicleInformationModule, diagnosticReadinessModule);

        inOrder.verify(vehicleInformationModule).setJ1939(j1939);
        inOrder.verify(diagnosticReadinessModule).setJ1939(j1939);
        inOrder.verify(comparisonModule).setJ1939(j1939);
        inOrder.verify(engineSpeedModule).setJ1939(j1939);

        inOrder.verify(listener).onProgress(0, 17, "");
        inOrder.verify(reportFileModule).onProgress(0, 17, "");

        inOrder.verify(listener).onProgress(1, 17, "Reading Engine Speed");
        inOrder.verify(reportFileModule).onProgress(1, 17, "Reading Engine Speed");

        verify(engineSpeedModule, atLeastOnce()).isEngineCommunicating();

        inOrder.verify(listener).onMessage(
                "The engine is not communicating.  Please check the adapter connection with the vehicle and/or turn the key on/start the vehicle.",
                "Engine Not Communicating", JOptionPane.WARNING_MESSAGE);
        inOrder.verify(reportFileModule).onMessage(
                "The engine is not communicating.  Please check the adapter connection with the vehicle and/or turn the key on/start the vehicle.",
                "Engine Not Communicating", JOptionPane.WARNING_MESSAGE);
        inOrder.verify(listener).onProgress(1, 17, "Engine Not Communicating.  Please start vehicle or push Stop");
        inOrder.verify(reportFileModule).onProgress(1, 17,
                "Engine Not Communicating.  Please start vehicle or push Stop");

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(bannerModule).reportStopped(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");

        inOrder.verify(bannerModule).getTypeName();
        inOrder.verify(listener).onProgress(17, 17, "Monitor Tracking Stopped");
        inOrder.verify(reportFileModule).onProgress(17, 17, "Monitor Tracking Stopped");
        inOrder.verify(listener).onComplete(false);
        inOrder.verify(reportFileModule).onComplete(false);
    }

    @Test
    public void testEndTracking() {
        instance.endTracking();
        verify(monitorTrackingModule).endTracking();
    }

    @Test
    public void testHappyPath() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);
        when(comparisonModule.compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2), eq(17)))
                .thenReturn(true);
        when(reportFileModule.getMinutesSinceCodeClear()).thenReturn((double) 1234);

        LocalDateTime monitorTime = LocalDateTime.now().minusSeconds(2);
        when(reportFileModule.getInitialMonitorsTime()).thenReturn(monitorTime);
        when(dateTimeModule.format(monitorTime)).thenReturn("MonitorTime");

        Set<MonitoredSystem> initialSystems = new HashSet<>();
        when(reportFileModule.getInitialMonitors()).thenReturn(initialSystems);
        when(monitorTrackingModule.getLastDm5Time()).thenReturn("LastDM5Time");
        Set<MonitoredSystem> lastSystems = new HashSet<>();
        when(monitorTrackingModule.getLastSystems()).thenReturn(lastSystems);

        LocalDateTime ratioTime = LocalDateTime.now().plusSeconds(2);
        when(reportFileModule.getInitialRatiosTime()).thenReturn(ratioTime);
        when(dateTimeModule.format(ratioTime)).thenReturn("RatioTime");

        when(monitorTrackingModule.getLastDm20Time()).thenReturn("LastDM20Time");
        Set<PerformanceRatio> lastRatios = new HashSet<>();
        when(monitorTrackingModule.getLastRatios()).thenReturn(lastRatios);
        when(monitorTrackingModule.getLastIgnitionCycles()).thenReturn(789);
        when(monitorTrackingModule.getLastObdCounts()).thenReturn(456);

        Set<PerformanceRatio> initialRatios = new HashSet<>();
        when(reportFileModule.getInitialRatios()).thenReturn(initialRatios);
        when(reportFileModule.getInitialIgnitionCycles()).thenReturn(987);
        when(reportFileModule.getInitialOBDCounts()).thenReturn(645);

        instance.execute(listener, j1939, reportFileModule);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        InOrder inOrder = inOrder(listener, engineSpeedModule, comparisonModule, monitorTrackingModule, bannerModule,
                reportFileModule, vehicleInformationModule, diagnosticReadinessModule, dateTimeModule);

        inOrder.verify(vehicleInformationModule).setJ1939(j1939);
        inOrder.verify(diagnosticReadinessModule).setJ1939(j1939);
        inOrder.verify(comparisonModule).setJ1939(j1939);
        inOrder.verify(engineSpeedModule).setJ1939(j1939);

        inOrder.verify(listener).onProgress(0, 17, "");
        inOrder.verify(reportFileModule).onProgress(0, 17, "");

        inOrder.verify(listener).onProgress(1, 17, "Reading Engine Speed");
        inOrder.verify(reportFileModule).onProgress(1, 17, "Reading Engine Speed");
        inOrder.verify(engineSpeedModule).isEngineCommunicating();

        inOrder.verify(listener).onProgress(2, 17, "Comparing Vehicle to Report File");
        inOrder.verify(reportFileModule).onProgress(2, 17, "Comparing Vehicle to Report File");
        inOrder.verify(comparisonModule).reset();
        inOrder.verify(comparisonModule).compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(17));
        inOrder.verify(monitorTrackingModule).setJ1939(j1939);

        inOrder.verify(listener).onProgress(7, 17, "Generating Header");
        inOrder.verify(reportFileModule).onProgress(7, 17, "Generating Header");
        inOrder.verify(bannerModule).reportHeader(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(8, 17, "Requesting VIN");
        inOrder.verify(reportFileModule).onProgress(8, 17, "Requesting VIN");
        inOrder.verify(vehicleInformationModule).reportVin(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(9, 17, "Requesting Calibration Information");
        inOrder.verify(reportFileModule).onProgress(9, 17, "Requesting Calibration Information");
        inOrder.verify(vehicleInformationModule).reportCalibrationInformation(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(10, 17, "Requesting DM21");
        inOrder.verify(reportFileModule).onProgress(10, 17, "Requesting DM21");
        inOrder.verify(reportFileModule).getMinutesSinceCodeClear();
        inOrder.verify(diagnosticReadinessModule).reportDM21(any(ResultsListener.class), eq((double) 1234));

        inOrder.verify(listener).onProgress(11, 17, "Tracking Monitor Completion Status");
        inOrder.verify(reportFileModule).onProgress(11, 17, "Tracking Monitor Completion Status");
        inOrder.verify(monitorTrackingModule).trackMonitors(any(ResultsListener.class), eq(reportFileModule));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(monitorTrackingModule).getLastDm5Time();
        inOrder.verify(listener).onProgress(12, 17, "Reporting Monitored Systems Results");
        inOrder.verify(reportFileModule).onProgress(12, 17, "Reporting Monitored Systems Results");
        inOrder.verify(monitorTrackingModule).getLastSystems();
        inOrder.verify(reportFileModule).getInitialMonitorsTime();
        inOrder.verify(dateTimeModule).format(monitorTime);
        inOrder.verify(reportFileModule).getInitialMonitors();
        inOrder.verify(diagnosticReadinessModule).reportMonitoredSystems(any(ResultsListener.class), eq(lastSystems),
                eq(initialSystems), eq("MonitorTime"), eq("LastDM5Time"));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(monitorTrackingModule).getLastDm20Time();
        inOrder.verify(listener).onProgress(13, 17, "Reporting Performance Ratio Results");
        inOrder.verify(reportFileModule).onProgress(13, 17, "Reporting Performance Ratio Results");
        inOrder.verify(monitorTrackingModule).getLastRatios();
        inOrder.verify(monitorTrackingModule).getLastIgnitionCycles();
        inOrder.verify(monitorTrackingModule).getLastObdCounts();
        inOrder.verify(reportFileModule).getInitialRatiosTime();
        inOrder.verify(dateTimeModule).format(ratioTime);
        inOrder.verify(reportFileModule).getInitialRatios();
        inOrder.verify(reportFileModule).getInitialIgnitionCycles();
        inOrder.verify(reportFileModule).getInitialOBDCounts();

        inOrder.verify(diagnosticReadinessModule).reportPerformanceRatios(any(ResultsListener.class), eq(initialRatios),
                eq(lastRatios), eq(987), eq(789), eq(645), eq(456), eq("RatioTime"), eq("LastDM20Time"));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(14, 17, "Requesting DM21");
        inOrder.verify(reportFileModule).onProgress(14, 17, "Requesting DM21");
        inOrder.verify(diagnosticReadinessModule).reportDM21(any(ResultsListener.class), eq((double) -1));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(15, 17, "Reading Vehicle Distance");
        inOrder.verify(reportFileModule).onProgress(15, 17, "Reading Vehicle Distance");
        inOrder.verify(vehicleInformationModule).reportVehicleDistance(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(16, 17, "Requesting Engine Hours");
        inOrder.verify(reportFileModule).onProgress(16, 17, "Requesting Engine Hours");
        inOrder.verify(vehicleInformationModule).reportEngineHours(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(17, 17, "Generating Quality Information");
        inOrder.verify(reportFileModule).onProgress(17, 17, "Generating Quality Information");
        inOrder.verify(reportFileModule).reportAndResetCommunicationQuality(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(bannerModule).reportFooter(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");

        inOrder.verify(bannerModule).getTypeName();
        inOrder.verify(listener).onProgress(17, 17, "Monitor Tracking Completed");
        inOrder.verify(reportFileModule).onProgress(17, 17, "Monitor Tracking Completed");
        inOrder.verify(listener).onComplete(true);
        inOrder.verify(reportFileModule).onComplete(true);
    }

    @Test
    public void testHappyPathWithNoDM5orDM20Results() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);
        when(comparisonModule.compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2), eq(17)))
                .thenReturn(true);
        when(reportFileModule.getMinutesSinceCodeClear()).thenReturn((double) 1234);
        when(dateTimeModule.getDateTime()).thenReturn("Time").thenReturn("Time");
        when(monitorTrackingModule.getLastDm5Time()).thenReturn(null);

        when(monitorTrackingModule.getLastDm20Time()).thenReturn(null);

        instance.execute(listener, j1939, reportFileModule);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        InOrder inOrder = inOrder(listener, engineSpeedModule, comparisonModule, monitorTrackingModule, bannerModule,
                reportFileModule, vehicleInformationModule, diagnosticReadinessModule, dateTimeModule);

        inOrder.verify(vehicleInformationModule).setJ1939(j1939);
        inOrder.verify(diagnosticReadinessModule).setJ1939(j1939);
        inOrder.verify(comparisonModule).setJ1939(j1939);
        inOrder.verify(engineSpeedModule).setJ1939(j1939);

        inOrder.verify(listener).onProgress(0, 17, "");
        inOrder.verify(reportFileModule).onProgress(0, 17, "");

        inOrder.verify(listener).onProgress(1, 17, "Reading Engine Speed");
        inOrder.verify(reportFileModule).onProgress(1, 17, "Reading Engine Speed");
        inOrder.verify(engineSpeedModule).isEngineCommunicating();

        inOrder.verify(listener).onProgress(2, 17, "Comparing Vehicle to Report File");
        inOrder.verify(reportFileModule).onProgress(2, 17, "Comparing Vehicle to Report File");
        inOrder.verify(comparisonModule).reset();
        inOrder.verify(comparisonModule).compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(17));
        inOrder.verify(monitorTrackingModule).setJ1939(j1939);

        inOrder.verify(listener).onProgress(7, 17, "Generating Header");
        inOrder.verify(reportFileModule).onProgress(7, 17, "Generating Header");
        inOrder.verify(bannerModule).reportHeader(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(8, 17, "Requesting VIN");
        inOrder.verify(reportFileModule).onProgress(8, 17, "Requesting VIN");
        inOrder.verify(vehicleInformationModule).reportVin(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(9, 17, "Requesting Calibration Information");
        inOrder.verify(reportFileModule).onProgress(9, 17, "Requesting Calibration Information");
        inOrder.verify(vehicleInformationModule).reportCalibrationInformation(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(10, 17, "Requesting DM21");
        inOrder.verify(reportFileModule).onProgress(10, 17, "Requesting DM21");
        inOrder.verify(reportFileModule).getMinutesSinceCodeClear();
        inOrder.verify(diagnosticReadinessModule).reportDM21(any(ResultsListener.class), eq((double) 1234));

        inOrder.verify(listener).onProgress(11, 17, "Tracking Monitor Completion Status");
        inOrder.verify(reportFileModule).onProgress(11, 17, "Tracking Monitor Completion Status");
        inOrder.verify(monitorTrackingModule).trackMonitors(any(ResultsListener.class), eq(reportFileModule));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(monitorTrackingModule).getLastDm5Time();
        inOrder.verify(dateTimeModule).getDateTime();
        inOrder.verify(listener).onResult("Time Monitored Systems Results cannot be reported");
        inOrder.verify(reportFileModule).onResult("Time Monitored Systems Results cannot be reported");
        inOrder.verify(listener).onProgress(12, 17, "Monitored Systems Results cannot be reported");
        inOrder.verify(reportFileModule).onProgress(12, 17, "Monitored Systems Results cannot be reported");

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(monitorTrackingModule).getLastDm20Time();
        inOrder.verify(dateTimeModule).getDateTime();
        inOrder.verify(listener).onResult("Time Performance Ratio Results cannot be reported");
        inOrder.verify(reportFileModule).onResult("Time Performance Ratio Results cannot be reported");
        inOrder.verify(listener).onProgress(13, 17, "Performance Ratio Results cannot be reported");
        inOrder.verify(reportFileModule).onProgress(13, 17, "Performance Ratio Results cannot be reported");

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(14, 17, "Requesting DM21");
        inOrder.verify(reportFileModule).onProgress(14, 17, "Requesting DM21");
        inOrder.verify(diagnosticReadinessModule).reportDM21(any(ResultsListener.class), eq((double) -1));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(15, 17, "Reading Vehicle Distance");
        inOrder.verify(reportFileModule).onProgress(15, 17, "Reading Vehicle Distance");
        inOrder.verify(vehicleInformationModule).reportVehicleDistance(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(16, 17, "Requesting Engine Hours");
        inOrder.verify(reportFileModule).onProgress(16, 17, "Requesting Engine Hours");
        inOrder.verify(vehicleInformationModule).reportEngineHours(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(17, 17, "Generating Quality Information");
        inOrder.verify(reportFileModule).onProgress(17, 17, "Generating Quality Information");
        inOrder.verify(reportFileModule).reportAndResetCommunicationQuality(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(bannerModule).reportFooter(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");

        inOrder.verify(bannerModule).getTypeName();
        inOrder.verify(listener).onProgress(17, 17, "Monitor Tracking Completed");
        inOrder.verify(reportFileModule).onProgress(17, 17, "Monitor Tracking Completed");
        inOrder.verify(listener).onComplete(true);
        inOrder.verify(reportFileModule).onComplete(true);
    }
}
