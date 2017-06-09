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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.JOptionPane;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.soliddesign.iumpr.bus.j1939.J1939;
import net.soliddesign.iumpr.modules.BannerModule;
import net.soliddesign.iumpr.modules.ComparisonModule;
import net.soliddesign.iumpr.modules.DateTimeModule;
import net.soliddesign.iumpr.modules.DiagnosticReadinessModule;
import net.soliddesign.iumpr.modules.EngineSpeedModule;
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
    private OBDTestsModule obdTestsModule;

    @Mock
    private ReportFileModule reportFileModule;

    @Mock
    private VehicleInformationModule vehicleInformationModule;

    @Before
    public void setUp() throws Exception {
        when(bannerModule.getTypeName()).thenReturn("Data Collection");

        instance = new CollectResultsController(executor, engineSpeedModule, bannerModule, dateTimeModule,
                vehicleInformationModule, diagnosticReadinessModule, obdTestsModule, comparisonModule);
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(engineSpeedModule, bannerModule, vehicleInformationModule, diagnosticReadinessModule,
                dateTimeModule, comparisonModule, j1939, reportFileModule, listener, obdTestsModule, executor);
    }

    @Test
    public void testAbortIfNoOBDModules() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);
        when(comparisonModule.compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2), eq(21)))
                .thenReturn(true);
        when(reportFileModule.getMinutesSinceCodeClear()).thenReturn(1234).thenReturn(1234);
        List<Integer> obdModules = Collections.singletonList(0x00);
        when(diagnosticReadinessModule.getOBDModules(any(ResultsListener.class))).thenReturn(obdModules);
        when(diagnosticReadinessModule.reportDM5(any(ResultsListener.class))).thenReturn(true);
        when(diagnosticReadinessModule.reportDM26(any(ResultsListener.class))).thenReturn(true);
        when(diagnosticReadinessModule.reportDM20(any(ResultsListener.class))).thenReturn(true);

        instance.execute(listener, j1939, reportFileModule);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        InOrder inOrder = inOrder(listener, engineSpeedModule, comparisonModule, obdTestsModule, bannerModule,
                reportFileModule, vehicleInformationModule, diagnosticReadinessModule);

        inOrder.verify(vehicleInformationModule).setJ1939(j1939);
        inOrder.verify(diagnosticReadinessModule).setJ1939(j1939);
        inOrder.verify(comparisonModule).setJ1939(j1939);
        inOrder.verify(engineSpeedModule).setJ1939(j1939);

        inOrder.verify(listener).onProgress(0, 21, "");
        inOrder.verify(reportFileModule).onProgress(0, 21, "");

        inOrder.verify(listener).onProgress(1, 21, "Reading Engine Speed");
        inOrder.verify(reportFileModule).onProgress(1, 21, "Reading Engine Speed");
        inOrder.verify(engineSpeedModule).isEngineCommunicating();

        inOrder.verify(listener).onProgress(2, 21, "Comparing Vehicle to Report File");
        inOrder.verify(reportFileModule).onProgress(2, 21, "Comparing Vehicle to Report File");
        inOrder.verify(comparisonModule).reset();
        inOrder.verify(comparisonModule).compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(21));
        inOrder.verify(obdTestsModule).setJ1939(j1939);

        inOrder.verify(listener).onProgress(7, 21, "Generating Header");
        inOrder.verify(reportFileModule).onProgress(7, 21, "Generating Header");
        inOrder.verify(bannerModule).reportHeader(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(8, 21, "Requesting VIN");
        inOrder.verify(reportFileModule).onProgress(8, 21, "Requesting VIN");
        inOrder.verify(vehicleInformationModule).reportVin(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(9, 21, "Requesting Calibration Information");
        inOrder.verify(reportFileModule).onProgress(9, 21, "Requesting Calibration Information");
        inOrder.verify(vehicleInformationModule).reportCalibrationInformation(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(10, 21, "Requesting DM21");
        inOrder.verify(reportFileModule).onProgress(10, 21, "Requesting DM21");
        inOrder.verify(reportFileModule).getMinutesSinceCodeClear();
        inOrder.verify(diagnosticReadinessModule).reportDM21(any(ResultsListener.class), eq(1234));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(11, 21, "Requesting HD OBD Modules");
        inOrder.verify(reportFileModule).onProgress(11, 21, "Requesting HD OBD Modules");
        inOrder.verify(diagnosticReadinessModule).getOBDModules(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(12, 21, "Requesting OBD Test Results");
        inOrder.verify(reportFileModule).onProgress(12, 21, "Requesting OBD Test Results");
        inOrder.verify(obdTestsModule).reportOBDTests(any(ResultsListener.class), eq(obdModules));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(13, 21, "Requesting DM5");
        inOrder.verify(reportFileModule).onProgress(13, 21, "Requesting DM5");
        inOrder.verify(diagnosticReadinessModule).reportDM5(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(14, 21, "Requesting DM26");
        inOrder.verify(reportFileModule).onProgress(14, 21, "Requesting DM26");
        inOrder.verify(diagnosticReadinessModule).reportDM26(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(15, 21, "Requesting DM20");
        inOrder.verify(reportFileModule).onProgress(15, 21, "Requesting DM20");
        inOrder.verify(diagnosticReadinessModule).reportDM20(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(16, 21, "Requesting DM21");
        inOrder.verify(reportFileModule).onProgress(16, 21, "Requesting DM21");
        inOrder.verify(reportFileModule).getMinutesSinceCodeClear();
        inOrder.verify(diagnosticReadinessModule).reportDM21(any(ResultsListener.class), eq(1234));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(17, 21, "Reading Vehicle Distance");
        inOrder.verify(reportFileModule).onProgress(17, 21, "Reading Vehicle Distance");
        inOrder.verify(vehicleInformationModule).reportVehicleDistance(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(18, 21, "Requesting Engine Hours");
        inOrder.verify(reportFileModule).onProgress(18, 21, "Requesting Engine Hours");
        inOrder.verify(vehicleInformationModule).reportEngineHours(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(19, 21, "Requesting VIN");
        inOrder.verify(reportFileModule).onProgress(19, 21, "Requesting VIN");
        inOrder.verify(vehicleInformationModule).reportVin(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(20, 21, "Requesting Calibration Information");
        inOrder.verify(reportFileModule).onProgress(20, 21, "Requesting Calibration Information");
        inOrder.verify(vehicleInformationModule).reportCalibrationInformation(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(21, 21, "Generating Quality Information");
        inOrder.verify(reportFileModule).onProgress(21, 21, "Generating Quality Information");
        inOrder.verify(reportFileModule).reportAndResetCommunicationQuality(any(ResultsListener.class));
        inOrder.verify(reportFileModule).reportQuality(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(bannerModule).reportFooter(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");

        inOrder.verify(bannerModule).getTypeName();
        inOrder.verify(listener).onProgress(21, 21, "Data Collection Completed");
        inOrder.verify(reportFileModule).onProgress(21, 21, "Data Collection Completed");
        inOrder.verify(listener).onComplete(true);
        inOrder.verify(reportFileModule).onComplete(true);
    }

    @Test
    public void testAbortIfVehicleMismatch() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);
        when(comparisonModule.compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2), eq(21)))
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

        inOrder.verify(listener).onProgress(0, 21, "");
        inOrder.verify(reportFileModule).onProgress(0, 21, "");

        inOrder.verify(listener).onProgress(1, 21, "Reading Engine Speed");
        inOrder.verify(reportFileModule).onProgress(1, 21, "Reading Engine Speed");
        inOrder.verify(engineSpeedModule).isEngineCommunicating();

        inOrder.verify(listener).onProgress(2, 21, "Comparing Vehicle to Report File");
        inOrder.verify(reportFileModule).onProgress(2, 21, "Comparing Vehicle to Report File");
        inOrder.verify(comparisonModule).reset();
        inOrder.verify(comparisonModule).compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(21));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(bannerModule).reportAborted(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");

        inOrder.verify(bannerModule).getTypeName();
        inOrder.verify(listener).onProgress(21, 21, "Data Collection Aborted");
        inOrder.verify(reportFileModule).onProgress(21, 21, "Data Collection Aborted");
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

        inOrder.verify(listener).onProgress(0, 21, "");
        inOrder.verify(reportFileModule).onProgress(0, 21, "");

        inOrder.verify(listener).onProgress(1, 21, "Reading Engine Speed");
        inOrder.verify(reportFileModule).onProgress(1, 21, "Reading Engine Speed");

        verify(engineSpeedModule, atLeastOnce()).isEngineCommunicating();

        inOrder.verify(listener).onUrgentMessage(
                "The engine is not communicating.  Please check the adapter connection with the vehicle and/or turn the key on/start the vehicle.",
                "Engine Not Communicating", JOptionPane.WARNING_MESSAGE);
        inOrder.verify(reportFileModule).onUrgentMessage(
                "The engine is not communicating.  Please check the adapter connection with the vehicle and/or turn the key on/start the vehicle.",
                "Engine Not Communicating", JOptionPane.WARNING_MESSAGE);
        inOrder.verify(listener).onProgress(1, 21, "Engine Not Communicating.  Please start vehicle or push Stop");
        inOrder.verify(reportFileModule).onProgress(1, 21,
                "Engine Not Communicating.  Please start vehicle or push Stop");

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");

        inOrder.verify(bannerModule).reportStopped(any(ResultsListener.class));
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(bannerModule).getTypeName();
        inOrder.verify(listener).onProgress(21, 21, "Data Collection Stopped");
        inOrder.verify(reportFileModule).onProgress(21, 21, "Data Collection Stopped");
        inOrder.verify(listener).onComplete(false);
        inOrder.verify(reportFileModule).onComplete(false);
    }

    @Test
    public void testHappyPath() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);
        when(comparisonModule.compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2), eq(21)))
                .thenReturn(true);
        when(reportFileModule.getMinutesSinceCodeClear()).thenReturn(1234);
        List<Integer> obdModules = Collections.emptyList();
        when(diagnosticReadinessModule.getOBDModules(any(ResultsListener.class))).thenReturn(obdModules);

        instance.execute(listener, j1939, reportFileModule);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        InOrder inOrder = inOrder(listener, engineSpeedModule, comparisonModule, obdTestsModule, bannerModule,
                reportFileModule, vehicleInformationModule, diagnosticReadinessModule);

        inOrder.verify(vehicleInformationModule).setJ1939(j1939);
        inOrder.verify(diagnosticReadinessModule).setJ1939(j1939);
        inOrder.verify(comparisonModule).setJ1939(j1939);
        inOrder.verify(engineSpeedModule).setJ1939(j1939);

        inOrder.verify(listener).onProgress(0, 21, "");
        inOrder.verify(reportFileModule).onProgress(0, 21, "");

        inOrder.verify(listener).onProgress(1, 21, "Reading Engine Speed");
        inOrder.verify(reportFileModule).onProgress(1, 21, "Reading Engine Speed");
        inOrder.verify(engineSpeedModule).isEngineCommunicating();

        inOrder.verify(listener).onProgress(2, 21, "Comparing Vehicle to Report File");
        inOrder.verify(reportFileModule).onProgress(2, 21, "Comparing Vehicle to Report File");
        inOrder.verify(comparisonModule).reset();
        inOrder.verify(comparisonModule).compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(21));
        inOrder.verify(obdTestsModule).setJ1939(j1939);

        inOrder.verify(listener).onProgress(7, 21, "Generating Header");
        inOrder.verify(reportFileModule).onProgress(7, 21, "Generating Header");
        inOrder.verify(bannerModule).reportHeader(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(8, 21, "Requesting VIN");
        inOrder.verify(reportFileModule).onProgress(8, 21, "Requesting VIN");
        inOrder.verify(vehicleInformationModule).reportVin(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(9, 21, "Requesting Calibration Information");
        inOrder.verify(reportFileModule).onProgress(9, 21, "Requesting Calibration Information");
        inOrder.verify(vehicleInformationModule).reportCalibrationInformation(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(10, 21, "Requesting DM21");
        inOrder.verify(reportFileModule).onProgress(10, 21, "Requesting DM21");
        inOrder.verify(reportFileModule).getMinutesSinceCodeClear();
        inOrder.verify(diagnosticReadinessModule).reportDM21(any(ResultsListener.class), eq(1234));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(11, 21, "Requesting HD OBD Modules");
        inOrder.verify(reportFileModule).onProgress(11, 21, "Requesting HD OBD Modules");
        inOrder.verify(diagnosticReadinessModule).getOBDModules(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(bannerModule).reportAborted(any(ResultsListener.class));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");

        inOrder.verify(bannerModule).getTypeName();
        inOrder.verify(listener).onProgress(21, 21, "Data Collection Aborted");
        inOrder.verify(reportFileModule).onProgress(21, 21, "Data Collection Aborted");
        inOrder.verify(listener).onComplete(false);
        inOrder.verify(reportFileModule).onComplete(false);
    }

}
