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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.JOptionPane;

import org.etools.j1939tools.bus.Packet;
import org.etools.j1939tools.j1939.J1939;
import org.etools.j1939tools.j1939.packets.DM20MonitorPerformanceRatioPacket;
import org.etools.j1939tools.j1939.packets.DM5DiagnosticReadinessPacket;
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
import net.soliddesign.iumpr.modules.NoxBinningGhgTrackingModule;
import net.soliddesign.iumpr.modules.OBDTestsModule;
import net.soliddesign.iumpr.modules.ReportFileModule;
import net.soliddesign.iumpr.modules.VehicleInformationModule;

/**
 * Unit tests for the {@link CollectResultsController} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", justification = "It's complaining about the verify steps")
@RunWith(MockitoJUnitRunner.class)
public class CollectResultsControllerTest {

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

    private CollectResultsController instance;

    @Mock
    private J1939 j1939;

    @Mock
    private ResultsListener listener;

    @Mock
    private NoxBinningGhgTrackingModule noxBinningGhgTrackingModule;

    @Mock
    private OBDTestsModule obdTestsModule;

    @Mock
    private ReportFileModule reportFileModule;

    @Mock
    private VehicleInformationModule vehicleInformationModule;

    @Before
    public void setUp() throws Exception {
        when(bannerModule.getTypeName()).thenReturn("Data Collection");

        instance = new CollectResultsController(executor, engineSpeedModule, bannerModule, dateTimeModule,
                vehicleInformationModule, diagnosticReadinessModule, obdTestsModule, comparisonModule, noxBinningGhgTrackingModule);
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(engineSpeedModule, bannerModule, vehicleInformationModule, diagnosticReadinessModule,
                comparisonModule, j1939, reportFileModule, listener, obdTestsModule, noxBinningGhgTrackingModule,
                executor);
    }

    @Test
    public void testAbortIfNoOBDModules() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);
        when(comparisonModule.compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2), eq(22)))
                .thenReturn(true);
        when(reportFileModule.getMinutesSinceCodeClear()).thenReturn((double) 1234);
        List<Integer> obdModules = Collections.emptyList();
        when(diagnosticReadinessModule.getOBDModules(any(ResultsListener.class))).thenReturn(obdModules);

        instance.execute(listener, j1939, reportFileModule);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        InOrder inOrder = inOrder(listener, engineSpeedModule, comparisonModule, obdTestsModule,noxBinningGhgTrackingModule, bannerModule,
                reportFileModule, vehicleInformationModule, diagnosticReadinessModule);

        inOrder.verify(vehicleInformationModule).setJ1939(j1939);
        inOrder.verify(diagnosticReadinessModule).setJ1939(j1939);
        inOrder.verify(comparisonModule).setJ1939(j1939);
        inOrder.verify(engineSpeedModule).setJ1939(j1939);

        inOrder.verify(listener).onProgress(0, 22, "");
        inOrder.verify(reportFileModule).onProgress(0, 22, "");

        inOrder.verify(listener).onProgress(1, 22, "Reading Engine Speed");
        inOrder.verify(reportFileModule).onProgress(1, 22, "Reading Engine Speed");
        inOrder.verify(engineSpeedModule).isEngineCommunicating();

        inOrder.verify(listener).onProgress(2, 22, "Comparing Vehicle to Report File");
        inOrder.verify(reportFileModule).onProgress(2, 22, "Comparing Vehicle to Report File");
        inOrder.verify(comparisonModule).reset();
        inOrder.verify(comparisonModule).compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(22));

        // Why are these late?
        inOrder.verify(obdTestsModule).setJ1939(j1939);
        inOrder.verify(noxBinningGhgTrackingModule).setJ1939(j1939);

        inOrder.verify(listener).onProgress(7, 22, "Generating Header");
        inOrder.verify(reportFileModule).onProgress(7, 22, "Generating Header");
        inOrder.verify(bannerModule).reportHeader(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(8, 22, "Requesting VIN");
        inOrder.verify(reportFileModule).onProgress(8, 22, "Requesting VIN");
        inOrder.verify(vehicleInformationModule).reportVin(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(9, 22, "Requesting Calibration Information");
        inOrder.verify(reportFileModule).onProgress(9, 22, "Requesting Calibration Information");
        inOrder.verify(vehicleInformationModule).reportCalibrationInformation(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(10, 22, "Requesting DM21");
        inOrder.verify(reportFileModule).onProgress(10, 22, "Requesting DM21");
        inOrder.verify(reportFileModule).getMinutesSinceCodeClear();
        inOrder.verify(diagnosticReadinessModule).reportDM21(any(ResultsListener.class), eq((double) 1234));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(11, 22, "Requesting HD OBD Modules");
        inOrder.verify(reportFileModule).onProgress(11, 22, "Requesting HD OBD Modules");
        inOrder.verify(diagnosticReadinessModule).getOBDModules(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(bannerModule).reportAborted(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");

        inOrder.verify(bannerModule).getTypeName();
        inOrder.verify(listener).onProgress(22, 22, "Data Collection Aborted");
        inOrder.verify(reportFileModule).onProgress(22, 22, "Data Collection Aborted");
        inOrder.verify(listener).onComplete(false);
        inOrder.verify(reportFileModule).onComplete(false);
    }

    @Test
    public void testAbortIfVehicleMismatch() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);
        when(comparisonModule.compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2), eq(22)))
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

        inOrder.verify(listener).onProgress(0, 22, "");
        inOrder.verify(reportFileModule).onProgress(0, 22, "");

        inOrder.verify(listener).onProgress(1, 22, "Reading Engine Speed");
        inOrder.verify(reportFileModule).onProgress(1, 22, "Reading Engine Speed");
        inOrder.verify(engineSpeedModule).isEngineCommunicating();

        inOrder.verify(listener).onProgress(2, 22, "Comparing Vehicle to Report File");
        inOrder.verify(reportFileModule).onProgress(2, 22, "Comparing Vehicle to Report File");
        inOrder.verify(comparisonModule).reset();
        inOrder.verify(comparisonModule).compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(22));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(bannerModule).reportAborted(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");

        inOrder.verify(bannerModule).getTypeName();
        inOrder.verify(listener).onProgress(22, 22, "Data Collection Aborted");
        inOrder.verify(reportFileModule).onProgress(22, 22, "Data Collection Aborted");
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

        inOrder.verify(listener).onProgress(0, 22, "");
        inOrder.verify(reportFileModule).onProgress(0, 22, "");

        inOrder.verify(listener).onProgress(1, 22, "Reading Engine Speed");
        inOrder.verify(reportFileModule).onProgress(1, 22, "Reading Engine Speed");

        verify(engineSpeedModule, atLeastOnce()).isEngineCommunicating();

        inOrder.verify(listener).onMessage(
                "The engine is not communicating.  Please check the adapter connection with the vehicle and/or turn the key on/start the vehicle.",
                "Engine Not Communicating", JOptionPane.WARNING_MESSAGE);
        inOrder.verify(reportFileModule).onMessage(
                "The engine is not communicating.  Please check the adapter connection with the vehicle and/or turn the key on/start the vehicle.",
                "Engine Not Communicating", JOptionPane.WARNING_MESSAGE);
        inOrder.verify(listener).onProgress(1, 22, "Engine Not Communicating.  Please start vehicle or push Stop");
        inOrder.verify(reportFileModule).onProgress(1, 22,
                "Engine Not Communicating.  Please start vehicle or push Stop");

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");

        inOrder.verify(bannerModule).reportStopped(any(ResultsListener.class));
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(bannerModule).getTypeName();
        inOrder.verify(listener).onProgress(22, 22, "Data Collection Stopped");
        inOrder.verify(reportFileModule).onProgress(22, 22, "Data Collection Stopped");
        inOrder.verify(listener).onComplete(false);
        inOrder.verify(reportFileModule).onComplete(false);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHappyPath() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);
        when(comparisonModule.compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2), eq(22)))
                .thenReturn(true);
        when(reportFileModule.getMinutesSinceCodeClear()).thenReturn((double) 1234).thenReturn((double) 1234);

        when(dateTimeModule.getDateTime()).thenReturn("CurrentTime");

        LocalDateTime monitorTime = LocalDateTime.now().minusSeconds(2);
        when(reportFileModule.getInitialMonitorsTime()).thenReturn(monitorTime);
        when(dateTimeModule.format(monitorTime)).thenReturn("MonitorTime");

        Set<MonitoredSystem> initialSystems = new HashSet<>();
        when(reportFileModule.getInitialMonitors()).thenReturn(initialSystems);

        LocalDateTime ratioTime = LocalDateTime.now().plusSeconds(2);
        when(reportFileModule.getInitialRatiosTime()).thenReturn(ratioTime);
        when(dateTimeModule.format(ratioTime)).thenReturn("RatioTime");

        Set<PerformanceRatio> initialRatios = new HashSet<>();
        when(reportFileModule.getInitialRatios()).thenReturn(initialRatios);
        when(reportFileModule.getInitialIgnitionCycles()).thenReturn(987);
        when(reportFileModule.getInitialOBDCounts()).thenReturn(654);

        List<Integer> obdModules = Collections.singletonList(0x00);
        when(diagnosticReadinessModule.getOBDModules(any(ResultsListener.class))).thenReturn(obdModules);

        List<DM5DiagnosticReadinessPacket> dm5Packets = Collections.singletonList(new DM5DiagnosticReadinessPacket(
                Packet.create(DM5DiagnosticReadinessPacket.PGN, 0x00, 1, 2, 3, 4, 5, 6, 7, 8)));
        when(diagnosticReadinessModule.getDM5Packets(any(ResultsListener.class), eq(false))).thenReturn(dm5Packets);

        when(diagnosticReadinessModule.reportDM26(any(ResultsListener.class))).thenReturn(true);

        List<DM20MonitorPerformanceRatioPacket> dm20Packets = Collections
                .singletonList(new DM20MonitorPerformanceRatioPacket(
                        Packet.create(DM20MonitorPerformanceRatioPacket.PGN, 0x00, 1, 2, 3, 4, 5, 6, 7, 8)));
        when(diagnosticReadinessModule.getDM20Packets(any(ResultsListener.class), eq(false))).thenReturn(dm20Packets);

        instance.execute(listener, j1939, reportFileModule);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        InOrder inOrder = inOrder(listener, engineSpeedModule, comparisonModule, obdTestsModule, noxBinningGhgTrackingModule, bannerModule,
                reportFileModule, vehicleInformationModule, diagnosticReadinessModule);

        inOrder.verify(vehicleInformationModule).setJ1939(j1939);
        inOrder.verify(diagnosticReadinessModule).setJ1939(j1939);
        inOrder.verify(comparisonModule).setJ1939(j1939);
        inOrder.verify(engineSpeedModule).setJ1939(j1939);

        inOrder.verify(listener).onProgress(0, 22, "");
        inOrder.verify(reportFileModule).onProgress(0, 22, "");

        inOrder.verify(listener).onProgress(1, 22, "Reading Engine Speed");
        inOrder.verify(reportFileModule).onProgress(1, 22, "Reading Engine Speed");
        inOrder.verify(engineSpeedModule).isEngineCommunicating();

        inOrder.verify(listener).onProgress(2, 22, "Comparing Vehicle to Report File");
        inOrder.verify(reportFileModule).onProgress(2, 22, "Comparing Vehicle to Report File");
        inOrder.verify(comparisonModule).reset();
        inOrder.verify(comparisonModule).compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(22));

        // Why are these late?
        inOrder.verify(obdTestsModule).setJ1939(j1939);
        inOrder.verify(noxBinningGhgTrackingModule).setJ1939(j1939);

        inOrder.verify(listener).onProgress(7, 22, "Generating Header");
        inOrder.verify(reportFileModule).onProgress(7, 22, "Generating Header");
        inOrder.verify(bannerModule).reportHeader(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(8, 22, "Requesting VIN");
        inOrder.verify(reportFileModule).onProgress(8, 22, "Requesting VIN");
        inOrder.verify(vehicleInformationModule).reportVin(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(9, 22, "Requesting Calibration Information");
        inOrder.verify(reportFileModule).onProgress(9, 22, "Requesting Calibration Information");
        inOrder.verify(vehicleInformationModule).reportCalibrationInformation(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(10, 22, "Requesting DM21");
        inOrder.verify(reportFileModule).onProgress(10, 22, "Requesting DM21");
        inOrder.verify(reportFileModule).getMinutesSinceCodeClear();
        inOrder.verify(diagnosticReadinessModule).reportDM21(any(ResultsListener.class), eq((double) 1234));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(11, 22, "Requesting HD OBD Modules");
        inOrder.verify(reportFileModule).onProgress(11, 22, "Requesting HD OBD Modules");
        inOrder.verify(diagnosticReadinessModule).getOBDModules(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(12, 22, "Requesting OBD Test Results");
        inOrder.verify(reportFileModule).onProgress(12, 22, "Requesting OBD Test Results");
        inOrder.verify(obdTestsModule).reportOBDTests(any(ResultsListener.class), eq(obdModules));

        // NOx Binning
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(13, 22, "Requesting NOX Binning and GHG Tracking");
        inOrder.verify(reportFileModule).onProgress(13, 22, "Requesting NOX Binning and GHG Tracking");
        inOrder.verify(noxBinningGhgTrackingModule).reportInformation(any(ResultsListener.class), eq(List.of(0)));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(14, 22, "Requesting DM5");
        inOrder.verify(reportFileModule).onProgress(14, 22, "Requesting DM5");
        inOrder.verify(diagnosticReadinessModule).getDM5Packets(any(ResultsListener.class), eq(false));
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(reportFileModule).getInitialMonitorsTime();
        inOrder.verify(reportFileModule).getInitialMonitors();
        inOrder.verify(diagnosticReadinessModule).reportMonitoredSystems(any(ResultsListener.class),
                eq(initialSystems), any(Collection.class), eq("MonitorTime"), eq("CurrentTime"));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(15, 22, "Requesting DM26");
        inOrder.verify(reportFileModule).onProgress(15, 22, "Requesting DM26");
        inOrder.verify(diagnosticReadinessModule).reportDM26(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(16, 22, "Requesting DM20");
        inOrder.verify(reportFileModule).onProgress(16, 22, "Requesting DM20");
        inOrder.verify(diagnosticReadinessModule).getDM20Packets(any(ResultsListener.class), eq(false));
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(reportFileModule).getInitialRatiosTime();
        inOrder.verify(reportFileModule).getInitialRatios();
        inOrder.verify(reportFileModule).getInitialIgnitionCycles();
        inOrder.verify(reportFileModule).getInitialOBDCounts();
        inOrder.verify(diagnosticReadinessModule).reportPerformanceRatios(any(ResultsListener.class),
                eq(initialRatios), any(Collection.class), eq(987), any(int.class), eq(654),
                any(int.class), eq("RatioTime"), eq("CurrentTime"));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(17, 22, "Requesting DM21");
        inOrder.verify(reportFileModule).onProgress(17, 22, "Requesting DM21");
        inOrder.verify(reportFileModule).getMinutesSinceCodeClear();
        inOrder.verify(diagnosticReadinessModule).reportDM21(any(ResultsListener.class), eq((double) 1234));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(18, 22, "Reading Vehicle Distance");
        inOrder.verify(reportFileModule).onProgress(18, 22, "Reading Vehicle Distance");
        inOrder.verify(vehicleInformationModule).reportVehicleDistance(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(19, 22, "Requesting Engine Hours");
        inOrder.verify(reportFileModule).onProgress(19, 22, "Requesting Engine Hours");
        inOrder.verify(vehicleInformationModule).reportEngineHours(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(20, 22, "Requesting VIN");
        inOrder.verify(reportFileModule).onProgress(20, 22, "Requesting VIN");
        inOrder.verify(vehicleInformationModule).reportVin(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(21, 22, "Requesting Calibration Information");
        inOrder.verify(reportFileModule).onProgress(21, 22, "Requesting Calibration Information");
        inOrder.verify(vehicleInformationModule).reportCalibrationInformation(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(22, 22, "Generating Quality Information");
        inOrder.verify(reportFileModule).onProgress(22, 22, "Generating Quality Information");
        inOrder.verify(reportFileModule).reportAndResetCommunicationQuality(any(ResultsListener.class));
        inOrder.verify(reportFileModule).reportQuality(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(bannerModule).reportFooter(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");

        inOrder.verify(bannerModule).getTypeName();
        inOrder.verify(listener).onProgress(22, 22, "Data Collection Completed");
        inOrder.verify(reportFileModule).onProgress(22, 22, "Data Collection Completed");
        inOrder.verify(listener).onComplete(true);
        inOrder.verify(reportFileModule).onComplete(true);
    }

}
