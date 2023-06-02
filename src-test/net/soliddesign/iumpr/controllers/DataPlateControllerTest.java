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

import org.etools.j1939tools.j1939.J1939;
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
import net.soliddesign.iumpr.modules.DTCModule;
import net.soliddesign.iumpr.modules.DiagnosticReadinessModule;
import net.soliddesign.iumpr.modules.EngineSpeedModule;
import net.soliddesign.iumpr.modules.ReportFileModule;
import net.soliddesign.iumpr.modules.VehicleInformationModule;

/**
 * Unit tests for the {@link DataPlateController} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", justification = "It's complaining the verify steps")
@RunWith(MockitoJUnitRunner.class)
public class DataPlateControllerTest {

    private static final int SKIP_STEP = 7;

    private static final int TOTAL_STEPS = 28;

    @Mock
    private BannerModule bannerModule;

    @Mock
    private ComparisonModule comparisonModule;

    @Mock
    private DateTimeModule dateTimeModule;

    @Mock
    private DiagnosticReadinessModule diagnosticReadinessModule;

    @Mock
    private DTCModule dtcModule;

    @Mock
    private EngineSpeedModule engineSpeedModule;

    @Mock
    private ScheduledExecutorService executor;

    private DataPlateController instance;

    @Mock
    private J1939 j1939;

    @Mock
    private ResultsListener listener;

    @Mock
    private ReportFileModule reportFileModule;

    @Mock
    private VehicleInformationModule vehicleInformationModule;

    @Before
    public void setUp() throws Exception {
        when(bannerModule.getTypeName()).thenReturn("Data Plate");
        instance = new DataPlateController(executor, engineSpeedModule, bannerModule, dateTimeModule,
                vehicleInformationModule, diagnosticReadinessModule, dtcModule, comparisonModule);
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(engineSpeedModule, bannerModule, vehicleInformationModule, dateTimeModule,
                diagnosticReadinessModule, dtcModule, comparisonModule, j1939, reportFileModule, listener, executor);
    }

    @Test
    public void testAbortIfDM11Fails() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);
        when(engineSpeedModule.isEngineNotRunning()).thenReturn(false).thenReturn(false)
                .thenReturn(true);
        when(comparisonModule.compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(TOTAL_STEPS))).thenReturn(true);
        when(reportFileModule.getMinutesSinceCodeClear()).thenReturn((double) 1234).thenReturn((double) 0);
        when(reportFileModule.isNewFile()).thenReturn(true);
        List<Integer> obdModules = Collections.singletonList(0x00);
        when(dtcModule.reportDM11(any(ResultsListener.class), eq(obdModules))).thenReturn(false);
        when(diagnosticReadinessModule.getOBDModules(any(ResultsListener.class))).thenReturn(obdModules);

        instance.execute(listener, j1939, reportFileModule);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        InOrder inOrder = inOrder(listener, engineSpeedModule, comparisonModule, dtcModule, bannerModule,
                reportFileModule, vehicleInformationModule, diagnosticReadinessModule);

        inOrder.verify(vehicleInformationModule).setJ1939(j1939);
        inOrder.verify(diagnosticReadinessModule).setJ1939(j1939);
        inOrder.verify(comparisonModule).setJ1939(j1939);
        inOrder.verify(engineSpeedModule).setJ1939(j1939);

        int step = 0;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "");

        step++;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Engine Speed");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Engine Speed");
        inOrder.verify(engineSpeedModule).isEngineCommunicating();

        step++;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Comparing Vehicle to Report File");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Comparing Vehicle to Report File");
        inOrder.verify(comparisonModule).reset();
        inOrder.verify(comparisonModule).compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(TOTAL_STEPS));
        inOrder.verify(dtcModule).setJ1939(j1939);

        step = SKIP_STEP;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Generating Header");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Generating Header");
        inOrder.verify(bannerModule).reportHeader(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Gathering File Information");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Gathering File Information");
        inOrder.verify(reportFileModule).reportFileInformation(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Connection Speed");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Connection Speed");
        inOrder.verify(vehicleInformationModule).reportConnectionSpeed(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Address Claim");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Address Claim");
        inOrder.verify(vehicleInformationModule).reportAddressClaim(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting VIN");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting VIN");
        inOrder.verify(vehicleInformationModule).reportVin(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting Calibration Information");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting Calibration Information");
        inOrder.verify(vehicleInformationModule).reportCalibrationInformation(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).getMinutesSinceCodeClear();
        inOrder.verify(diagnosticReadinessModule).reportDM21(any(ResultsListener.class), eq((double) 1234));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting HD OBD Modules");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting HD OBD Modules");
        inOrder.verify(diagnosticReadinessModule).getOBDModules(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting Component Identification");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting Component Identification");
        inOrder.verify(vehicleInformationModule).reportComponentIdentification(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(reportFileModule).isNewFile();
        inOrder.verify(engineSpeedModule).isEngineNotRunning();
        inOrder.verify(listener).onUrgentMessage("Please turn the Engine OFF with Key ON.", "Adjust Key Switch",
                JOptionPane.WARNING_MESSAGE);
        inOrder.verify(reportFileModule).onUrgentMessage("Please turn the Engine OFF with Key ON.", "Adjust Key Switch",
                JOptionPane.WARNING_MESSAGE);
        inOrder.verify(engineSpeedModule).isEngineNotRunning();
        inOrder.verify(listener).onProgress(step - 1, TOTAL_STEPS, "Waiting for Key ON, Engine OFF...");
        inOrder.verify(reportFileModule).onProgress(step - 1, TOTAL_STEPS, "Waiting for Key ON, Engine OFF...");
        inOrder.verify(engineSpeedModule).isEngineNotRunning();
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Clearing Active Codes");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Clearing Active Codes");
        inOrder.verify(dtcModule).reportDM11(any(ResultsListener.class), eq(obdModules));
        inOrder.verify(listener).onMessage("The Diagnostic Trouble Codes were unable to be cleared.",
                "Clearing DTCs Failed", JOptionPane.ERROR_MESSAGE);
        inOrder.verify(reportFileModule).onMessage("The Diagnostic Trouble Codes were unable to be cleared.",
                "Clearing DTCs Failed", JOptionPane.ERROR_MESSAGE);

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(bannerModule).reportAborted(any(ResultsListener.class));
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");

        inOrder.verify(bannerModule).getTypeName();
        inOrder.verify(listener).onProgress(TOTAL_STEPS, TOTAL_STEPS, "Data Plate Aborted");
        inOrder.verify(reportFileModule).onProgress(TOTAL_STEPS, TOTAL_STEPS, "Data Plate Aborted");
        inOrder.verify(listener).onComplete(false);
        inOrder.verify(reportFileModule).onComplete(false);
    }

    @Test
    public void testAbortIfDM20Fails() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);
        when(comparisonModule.compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(TOTAL_STEPS))).thenReturn(true);
        when(reportFileModule.getMinutesSinceCodeClear()).thenReturn((double) 1234).thenReturn((double) 0);
        when(reportFileModule.isNewFile()).thenReturn(false);
        List<Integer> obdModules = Collections.singletonList(0x00);
        when(diagnosticReadinessModule.getOBDModules(any(ResultsListener.class))).thenReturn(obdModules);
        when(diagnosticReadinessModule.reportDM5(any(ResultsListener.class))).thenReturn(true);
        when(diagnosticReadinessModule.reportDM26(any(ResultsListener.class))).thenReturn(true);
        when(diagnosticReadinessModule.reportDM20(any(ResultsListener.class))).thenReturn(false);

        instance.execute(listener, j1939, reportFileModule);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        InOrder inOrder = inOrder(listener, engineSpeedModule, comparisonModule, dtcModule, bannerModule,
                reportFileModule, vehicleInformationModule, diagnosticReadinessModule);

        inOrder.verify(vehicleInformationModule).setJ1939(j1939);
        inOrder.verify(diagnosticReadinessModule).setJ1939(j1939);
        inOrder.verify(comparisonModule).setJ1939(j1939);
        inOrder.verify(engineSpeedModule).setJ1939(j1939);

        int step = 0;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "");

        step++;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Engine Speed");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Engine Speed");
        inOrder.verify(engineSpeedModule).isEngineCommunicating();

        step++;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Comparing Vehicle to Report File");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Comparing Vehicle to Report File");
        inOrder.verify(comparisonModule).reset();
        inOrder.verify(comparisonModule).compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(TOTAL_STEPS));
        inOrder.verify(dtcModule).setJ1939(j1939);

        step = SKIP_STEP;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Generating Header");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Generating Header");
        inOrder.verify(bannerModule).reportHeader(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Gathering File Information");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Gathering File Information");
        inOrder.verify(reportFileModule).reportFileInformation(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Connection Speed");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Connection Speed");
        inOrder.verify(vehicleInformationModule).reportConnectionSpeed(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Address Claim");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Address Claim");
        inOrder.verify(vehicleInformationModule).reportAddressClaim(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting VIN");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting VIN");
        inOrder.verify(vehicleInformationModule).reportVin(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting Calibration Information");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting Calibration Information");
        inOrder.verify(vehicleInformationModule).reportCalibrationInformation(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).getMinutesSinceCodeClear();
        inOrder.verify(diagnosticReadinessModule).reportDM21(any(ResultsListener.class), eq((double) 1234));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting HD OBD Modules");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting HD OBD Modules");
        inOrder.verify(diagnosticReadinessModule).getOBDModules(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting Component Identification");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting Component Identification");
        inOrder.verify(vehicleInformationModule).reportComponentIdentification(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(reportFileModule).isNewFile();
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Existing file; Codes Not Cleared");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Existing file; Codes Not Cleared");
        inOrder.verify(listener).onResult("Existing file; Codes Not Cleared");
        inOrder.verify(reportFileModule).onResult("Existing file; Codes Not Cleared");

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM5");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM5");
        inOrder.verify(diagnosticReadinessModule).reportDM5(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM26");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM26");
        inOrder.verify(diagnosticReadinessModule).reportDM26(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM20");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM20");
        inOrder.verify(diagnosticReadinessModule).reportDM20(any(ResultsListener.class));

        inOrder.verify(listener).onMessage("There were no DM20s received.", "Communications Error",
                JOptionPane.ERROR_MESSAGE);
        inOrder.verify(reportFileModule).onMessage("There were no DM20s received.", "Communications Error",
                JOptionPane.ERROR_MESSAGE);

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(bannerModule).reportAborted(any(ResultsListener.class));
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");

        inOrder.verify(bannerModule).getTypeName();
        inOrder.verify(listener).onProgress(TOTAL_STEPS, TOTAL_STEPS, "Data Plate Aborted");
        inOrder.verify(reportFileModule).onProgress(TOTAL_STEPS, TOTAL_STEPS, "Data Plate Aborted");
        inOrder.verify(listener).onComplete(false);
        inOrder.verify(reportFileModule).onComplete(false);
    }

    @Test
    public void testAbortIfDM26Fails() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);
        when(comparisonModule.compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(TOTAL_STEPS))).thenReturn(true);
        when(reportFileModule.getMinutesSinceCodeClear()).thenReturn((double) 1234).thenReturn((double) 0);
        when(reportFileModule.isNewFile()).thenReturn(false);
        List<Integer> obdModules = Collections.singletonList(0x00);
        when(diagnosticReadinessModule.getOBDModules(any(ResultsListener.class))).thenReturn(obdModules);
        when(diagnosticReadinessModule.reportDM5(any(ResultsListener.class))).thenReturn(true);
        when(diagnosticReadinessModule.reportDM26(any(ResultsListener.class))).thenReturn(false);

        instance.execute(listener, j1939, reportFileModule);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        InOrder inOrder = inOrder(listener, engineSpeedModule, comparisonModule, dtcModule, bannerModule,
                reportFileModule, vehicleInformationModule, diagnosticReadinessModule);

        inOrder.verify(vehicleInformationModule).setJ1939(j1939);
        inOrder.verify(diagnosticReadinessModule).setJ1939(j1939);
        inOrder.verify(comparisonModule).setJ1939(j1939);
        inOrder.verify(engineSpeedModule).setJ1939(j1939);

        int step = 0;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "");

        step++;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Engine Speed");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Engine Speed");
        inOrder.verify(engineSpeedModule).isEngineCommunicating();

        step++;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Comparing Vehicle to Report File");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Comparing Vehicle to Report File");
        inOrder.verify(comparisonModule).reset();
        inOrder.verify(comparisonModule).compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(TOTAL_STEPS));
        inOrder.verify(dtcModule).setJ1939(j1939);

        step = SKIP_STEP;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Generating Header");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Generating Header");
        inOrder.verify(bannerModule).reportHeader(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Gathering File Information");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Gathering File Information");
        inOrder.verify(reportFileModule).reportFileInformation(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Connection Speed");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Connection Speed");
        inOrder.verify(vehicleInformationModule).reportConnectionSpeed(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Address Claim");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Address Claim");
        inOrder.verify(vehicleInformationModule).reportAddressClaim(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting VIN");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting VIN");
        inOrder.verify(vehicleInformationModule).reportVin(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting Calibration Information");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting Calibration Information");
        inOrder.verify(vehicleInformationModule).reportCalibrationInformation(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).getMinutesSinceCodeClear();
        inOrder.verify(diagnosticReadinessModule).reportDM21(any(ResultsListener.class), eq((double) 1234));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting HD OBD Modules");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting HD OBD Modules");
        inOrder.verify(diagnosticReadinessModule).getOBDModules(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting Component Identification");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting Component Identification");
        inOrder.verify(vehicleInformationModule).reportComponentIdentification(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(reportFileModule).isNewFile();
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Existing file; Codes Not Cleared");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Existing file; Codes Not Cleared");
        inOrder.verify(listener).onResult("Existing file; Codes Not Cleared");
        inOrder.verify(reportFileModule).onResult("Existing file; Codes Not Cleared");

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM5");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM5");
        inOrder.verify(diagnosticReadinessModule).reportDM5(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM26");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM26");
        inOrder.verify(diagnosticReadinessModule).reportDM26(any(ResultsListener.class));

        inOrder.verify(listener).onMessage("There were no DM26s received.", "Communications Error",
                JOptionPane.ERROR_MESSAGE);
        inOrder.verify(reportFileModule).onMessage("There were no DM26s received.", "Communications Error",
                JOptionPane.ERROR_MESSAGE);

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(bannerModule).reportAborted(any(ResultsListener.class));
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");

        inOrder.verify(bannerModule).getTypeName();
        inOrder.verify(listener).onProgress(TOTAL_STEPS, TOTAL_STEPS, "Data Plate Aborted");
        inOrder.verify(reportFileModule).onProgress(TOTAL_STEPS, TOTAL_STEPS, "Data Plate Aborted");
        inOrder.verify(listener).onComplete(false);
        inOrder.verify(reportFileModule).onComplete(false);
    }

    @Test
    public void testAbortIfDM5Fails() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);
        when(comparisonModule.compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(TOTAL_STEPS))).thenReturn(true);
        when(reportFileModule.getMinutesSinceCodeClear()).thenReturn((double) 1234).thenReturn((double) 0);
        when(reportFileModule.isNewFile()).thenReturn(false);
        List<Integer> obdModules = Collections.singletonList(0x00);
        when(diagnosticReadinessModule.getOBDModules(any(ResultsListener.class))).thenReturn(obdModules);
        when(diagnosticReadinessModule.reportDM5(any(ResultsListener.class))).thenReturn(false);

        instance.execute(listener, j1939, reportFileModule);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        InOrder inOrder = inOrder(listener, engineSpeedModule, comparisonModule, dtcModule, bannerModule,
                reportFileModule, vehicleInformationModule, diagnosticReadinessModule);

        inOrder.verify(vehicleInformationModule).setJ1939(j1939);
        inOrder.verify(diagnosticReadinessModule).setJ1939(j1939);
        inOrder.verify(comparisonModule).setJ1939(j1939);
        inOrder.verify(engineSpeedModule).setJ1939(j1939);

        int step = 0;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "");

        step++;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Engine Speed");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Engine Speed");
        inOrder.verify(engineSpeedModule).isEngineCommunicating();

        step++;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Comparing Vehicle to Report File");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Comparing Vehicle to Report File");
        inOrder.verify(comparisonModule).reset();
        inOrder.verify(comparisonModule).compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(TOTAL_STEPS));
        inOrder.verify(dtcModule).setJ1939(j1939);

        step = SKIP_STEP;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Generating Header");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Generating Header");
        inOrder.verify(bannerModule).reportHeader(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Gathering File Information");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Gathering File Information");
        inOrder.verify(reportFileModule).reportFileInformation(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Connection Speed");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Connection Speed");
        inOrder.verify(vehicleInformationModule).reportConnectionSpeed(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Address Claim");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Address Claim");
        inOrder.verify(vehicleInformationModule).reportAddressClaim(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting VIN");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting VIN");
        inOrder.verify(vehicleInformationModule).reportVin(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting Calibration Information");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting Calibration Information");
        inOrder.verify(vehicleInformationModule).reportCalibrationInformation(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).getMinutesSinceCodeClear();
        inOrder.verify(diagnosticReadinessModule).reportDM21(any(ResultsListener.class), eq((double) 1234));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting HD OBD Modules");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting HD OBD Modules");
        inOrder.verify(diagnosticReadinessModule).getOBDModules(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting Component Identification");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting Component Identification");
        inOrder.verify(vehicleInformationModule).reportComponentIdentification(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(reportFileModule).isNewFile();
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Existing file; Codes Not Cleared");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Existing file; Codes Not Cleared");
        inOrder.verify(listener).onResult("Existing file; Codes Not Cleared");
        inOrder.verify(reportFileModule).onResult("Existing file; Codes Not Cleared");

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM5");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM5");
        inOrder.verify(diagnosticReadinessModule).reportDM5(any(ResultsListener.class));

        inOrder.verify(listener).onMessage("There were no DM5s received.", "Communications Error",
                JOptionPane.ERROR_MESSAGE);
        inOrder.verify(reportFileModule).onMessage("There were no DM5s received.", "Communications Error",
                JOptionPane.ERROR_MESSAGE);

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(bannerModule).reportAborted(any(ResultsListener.class));
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");

        inOrder.verify(bannerModule).getTypeName();
        inOrder.verify(listener).onProgress(TOTAL_STEPS, TOTAL_STEPS, "Data Plate Aborted");
        inOrder.verify(reportFileModule).onProgress(TOTAL_STEPS, TOTAL_STEPS, "Data Plate Aborted");
        inOrder.verify(listener).onComplete(false);
        inOrder.verify(reportFileModule).onComplete(false);
    }

    @Test
    public void testAbortIfNoOBDModules() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);
        when(comparisonModule.compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(TOTAL_STEPS))).thenReturn(true);
        when(reportFileModule.getMinutesSinceCodeClear()).thenReturn((double) 1234).thenReturn((double) 0);
        List<Integer> obdModules = Collections.emptyList();
        when(diagnosticReadinessModule.getOBDModules(any(ResultsListener.class))).thenReturn(obdModules);

        instance.execute(listener, j1939, reportFileModule);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        InOrder inOrder = inOrder(listener, engineSpeedModule, comparisonModule, dtcModule, bannerModule,
                reportFileModule, vehicleInformationModule, diagnosticReadinessModule);

        inOrder.verify(vehicleInformationModule).setJ1939(j1939);
        inOrder.verify(diagnosticReadinessModule).setJ1939(j1939);
        inOrder.verify(comparisonModule).setJ1939(j1939);
        inOrder.verify(engineSpeedModule).setJ1939(j1939);

        int step = 0;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "");

        step++;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Engine Speed");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Engine Speed");
        inOrder.verify(engineSpeedModule).isEngineCommunicating();

        step++;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Comparing Vehicle to Report File");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Comparing Vehicle to Report File");
        inOrder.verify(comparisonModule).reset();
        inOrder.verify(comparisonModule).compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(TOTAL_STEPS));
        inOrder.verify(dtcModule).setJ1939(j1939);

        step = SKIP_STEP;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Generating Header");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Generating Header");
        inOrder.verify(bannerModule).reportHeader(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Gathering File Information");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Gathering File Information");
        inOrder.verify(reportFileModule).reportFileInformation(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Connection Speed");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Connection Speed");
        inOrder.verify(vehicleInformationModule).reportConnectionSpeed(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Address Claim");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Address Claim");
        inOrder.verify(vehicleInformationModule).reportAddressClaim(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting VIN");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting VIN");
        inOrder.verify(vehicleInformationModule).reportVin(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting Calibration Information");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting Calibration Information");
        inOrder.verify(vehicleInformationModule).reportCalibrationInformation(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).getMinutesSinceCodeClear();
        inOrder.verify(diagnosticReadinessModule).reportDM21(any(ResultsListener.class), eq((double) 1234));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting HD OBD Modules");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting HD OBD Modules");
        inOrder.verify(diagnosticReadinessModule).getOBDModules(any(ResultsListener.class));
        inOrder.verify(listener).onMessage("No HD OBD Modules were detected.", "No HD OBD Modules",
                JOptionPane.ERROR_MESSAGE);
        inOrder.verify(reportFileModule).onMessage("No HD OBD Modules were detected.", "No HD OBD Modules",
                JOptionPane.ERROR_MESSAGE);

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(bannerModule).reportAborted(any(ResultsListener.class));
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");

        inOrder.verify(bannerModule).getTypeName();
        inOrder.verify(listener).onProgress(TOTAL_STEPS, TOTAL_STEPS, "Data Plate Aborted");
        inOrder.verify(reportFileModule).onProgress(TOTAL_STEPS, TOTAL_STEPS, "Data Plate Aborted");
        inOrder.verify(listener).onComplete(false);
        inOrder.verify(reportFileModule).onComplete(false);
    }

    @Test
    public void testAbortIfVehicleMismatch() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);
        when(comparisonModule.compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(TOTAL_STEPS)))
                        .thenReturn(false);

        instance.execute(listener, j1939, reportFileModule);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        InOrder inOrder = inOrder(listener, engineSpeedModule, comparisonModule, dtcModule, bannerModule,
                reportFileModule, vehicleInformationModule, diagnosticReadinessModule);

        inOrder.verify(vehicleInformationModule).setJ1939(j1939);
        inOrder.verify(diagnosticReadinessModule).setJ1939(j1939);
        inOrder.verify(comparisonModule).setJ1939(j1939);
        inOrder.verify(engineSpeedModule).setJ1939(j1939);

        int step = 0;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "");

        step++;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Engine Speed");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Engine Speed");
        inOrder.verify(engineSpeedModule).isEngineCommunicating();

        step++;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Comparing Vehicle to Report File");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Comparing Vehicle to Report File");
        inOrder.verify(comparisonModule).reset();
        inOrder.verify(comparisonModule).compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(TOTAL_STEPS));

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(bannerModule).reportAborted(any(ResultsListener.class));
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");

        inOrder.verify(bannerModule).getTypeName();
        inOrder.verify(listener).onProgress(TOTAL_STEPS, TOTAL_STEPS, "Data Plate Aborted");
        inOrder.verify(reportFileModule).onProgress(TOTAL_STEPS, TOTAL_STEPS, "Data Plate Aborted");
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
        InOrder inOrder = inOrder(listener, engineSpeedModule, comparisonModule, dtcModule, bannerModule,
                reportFileModule, vehicleInformationModule, diagnosticReadinessModule);

        inOrder.verify(vehicleInformationModule).setJ1939(j1939);
        inOrder.verify(diagnosticReadinessModule).setJ1939(j1939);
        inOrder.verify(comparisonModule).setJ1939(j1939);
        inOrder.verify(engineSpeedModule).setJ1939(j1939);

        int step = 0;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "");

        step++;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Engine Speed");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Engine Speed");

        verify(engineSpeedModule, atLeastOnce()).isEngineCommunicating();

        inOrder.verify(listener).onMessage(
                "The engine is not communicating.  Please check the adapter connection with the vehicle and/or turn the key on/start the vehicle.",
                "Engine Not Communicating", JOptionPane.WARNING_MESSAGE);
        inOrder.verify(reportFileModule).onMessage(
                "The engine is not communicating.  Please check the adapter connection with the vehicle and/or turn the key on/start the vehicle.",
                "Engine Not Communicating", JOptionPane.WARNING_MESSAGE);
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS,
                "Engine Not Communicating.  Please start vehicle or push Stop");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS,
                "Engine Not Communicating.  Please start vehicle or push Stop");

        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(bannerModule).reportStopped(any(ResultsListener.class));
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");

        inOrder.verify(bannerModule).getTypeName();
        inOrder.verify(listener).onProgress(TOTAL_STEPS, TOTAL_STEPS, "Data Plate Stopped");
        inOrder.verify(reportFileModule).onProgress(TOTAL_STEPS, TOTAL_STEPS, "Data Plate Stopped");
        inOrder.verify(listener).onComplete(false);
        inOrder.verify(reportFileModule).onComplete(false);
    }

    @Test
    public void testDisplayWarningForDM12s() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);
        when(comparisonModule.compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(TOTAL_STEPS))).thenReturn(true);
        when(reportFileModule.getMinutesSinceCodeClear()).thenReturn((double) 1234).thenReturn((double) 0);
        when(reportFileModule.isNewFile()).thenReturn(false);
        List<Integer> obdModules = Collections.singletonList(0x00);
        when(diagnosticReadinessModule.getOBDModules(any(ResultsListener.class))).thenReturn(obdModules);
        when(diagnosticReadinessModule.reportDM5(any(ResultsListener.class))).thenReturn(true);
        when(diagnosticReadinessModule.reportDM26(any(ResultsListener.class))).thenReturn(true);
        when(diagnosticReadinessModule.reportDM20(any(ResultsListener.class))).thenReturn(true);
        when(dtcModule.reportDM6(any(ResultsListener.class))).thenReturn(false);
        when(dtcModule.reportDM12(any(ResultsListener.class))).thenReturn(true);
        when(dtcModule.reportDM23(any(ResultsListener.class))).thenReturn(false);
        when(dtcModule.reportDM28(any(ResultsListener.class))).thenReturn(false);

        instance.execute(listener, j1939, reportFileModule);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        InOrder inOrder = inOrder(listener, engineSpeedModule, comparisonModule, dtcModule, bannerModule,
                reportFileModule, vehicleInformationModule, diagnosticReadinessModule);

        inOrder.verify(vehicleInformationModule).setJ1939(j1939);
        inOrder.verify(diagnosticReadinessModule).setJ1939(j1939);
        inOrder.verify(comparisonModule).setJ1939(j1939);
        inOrder.verify(engineSpeedModule).setJ1939(j1939);

        int step = 0;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "");

        step++;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Engine Speed");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Engine Speed");
        inOrder.verify(engineSpeedModule).isEngineCommunicating();

        step++;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Comparing Vehicle to Report File");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Comparing Vehicle to Report File");
        inOrder.verify(comparisonModule).reset();
        inOrder.verify(comparisonModule).compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(TOTAL_STEPS));
        inOrder.verify(dtcModule).setJ1939(j1939);

        step = SKIP_STEP;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Generating Header");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Generating Header");
        inOrder.verify(bannerModule).reportHeader(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Gathering File Information");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Gathering File Information");
        inOrder.verify(reportFileModule).reportFileInformation(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Connection Speed");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Connection Speed");
        inOrder.verify(vehicleInformationModule).reportConnectionSpeed(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Address Claim");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Address Claim");
        inOrder.verify(vehicleInformationModule).reportAddressClaim(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting VIN");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting VIN");
        inOrder.verify(vehicleInformationModule).reportVin(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting Calibration Information");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting Calibration Information");
        inOrder.verify(vehicleInformationModule).reportCalibrationInformation(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).getMinutesSinceCodeClear();
        inOrder.verify(diagnosticReadinessModule).reportDM21(any(ResultsListener.class), eq((double) 1234));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting HD OBD Modules");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting HD OBD Modules");
        inOrder.verify(diagnosticReadinessModule).getOBDModules(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting Component Identification");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting Component Identification");
        inOrder.verify(vehicleInformationModule).reportComponentIdentification(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(reportFileModule).isNewFile();
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Existing file; Codes Not Cleared");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Existing file; Codes Not Cleared");
        inOrder.verify(listener).onResult("Existing file; Codes Not Cleared");
        inOrder.verify(reportFileModule).onResult("Existing file; Codes Not Cleared");

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM5");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM5");
        inOrder.verify(diagnosticReadinessModule).reportDM5(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM26");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM26");
        inOrder.verify(diagnosticReadinessModule).reportDM26(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM20");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM20");
        inOrder.verify(diagnosticReadinessModule).reportDM20(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM6");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM6");
        inOrder.verify(dtcModule).reportDM6(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM12");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM12");
        inOrder.verify(dtcModule).reportDM12(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM23");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM23");
        inOrder.verify(dtcModule).reportDM23(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM28");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM28");
        inOrder.verify(dtcModule).reportDM28(any(ResultsListener.class));

        inOrder.verify(listener).onMessage("There were Diagnostic Trouble Codes reported.", "DTCs Exist",
                JOptionPane.WARNING_MESSAGE);
        inOrder.verify(reportFileModule).onMessage("There were Diagnostic Trouble Codes reported.", "DTCs Exist",
                JOptionPane.WARNING_MESSAGE);

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).getMinutesSinceCodeClear();
        inOrder.verify(diagnosticReadinessModule).reportDM21(any(ResultsListener.class), eq((double) 0));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Vehicle Distance");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Vehicle Distance");
        inOrder.verify(vehicleInformationModule).reportVehicleDistance(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting Engine Hours");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting Engine Hours");
        inOrder.verify(vehicleInformationModule).reportEngineHours(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Generating Quality Information");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Generating Quality Information");
        inOrder.verify(reportFileModule).reportAndResetCommunicationQuality(any(ResultsListener.class));

        inOrder.verify(reportFileModule).setNewFile(false);
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(bannerModule).reportFooter(any(ResultsListener.class));
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");

        inOrder.verify(bannerModule).getTypeName();
        inOrder.verify(listener).onProgress(TOTAL_STEPS, TOTAL_STEPS, "Data Plate Completed");
        inOrder.verify(reportFileModule).onProgress(TOTAL_STEPS, TOTAL_STEPS, "Data Plate Completed");
        inOrder.verify(listener).onComplete(true);
        inOrder.verify(reportFileModule).onComplete(true);
    }

    @Test
    public void testDisplayWarningForDM23s() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);
        when(comparisonModule.compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(TOTAL_STEPS))).thenReturn(true);
        when(reportFileModule.getMinutesSinceCodeClear()).thenReturn((double) 1234).thenReturn((double) 0);
        when(reportFileModule.isNewFile()).thenReturn(false);
        List<Integer> obdModules = Collections.singletonList(0x00);
        when(diagnosticReadinessModule.getOBDModules(any(ResultsListener.class))).thenReturn(obdModules);
        when(diagnosticReadinessModule.reportDM5(any(ResultsListener.class))).thenReturn(true);
        when(diagnosticReadinessModule.reportDM26(any(ResultsListener.class))).thenReturn(true);
        when(diagnosticReadinessModule.reportDM20(any(ResultsListener.class))).thenReturn(true);
        when(dtcModule.reportDM6(any(ResultsListener.class))).thenReturn(false);
        when(dtcModule.reportDM12(any(ResultsListener.class))).thenReturn(false);
        when(dtcModule.reportDM23(any(ResultsListener.class))).thenReturn(true);
        when(dtcModule.reportDM28(any(ResultsListener.class))).thenReturn(false);

        instance.execute(listener, j1939, reportFileModule);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        InOrder inOrder = inOrder(listener, engineSpeedModule, comparisonModule, dtcModule, bannerModule,
                reportFileModule, vehicleInformationModule, diagnosticReadinessModule);

        inOrder.verify(vehicleInformationModule).setJ1939(j1939);
        inOrder.verify(diagnosticReadinessModule).setJ1939(j1939);
        inOrder.verify(comparisonModule).setJ1939(j1939);
        inOrder.verify(engineSpeedModule).setJ1939(j1939);

        int step = 0;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "");

        step++;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Engine Speed");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Engine Speed");
        inOrder.verify(engineSpeedModule).isEngineCommunicating();

        step++;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Comparing Vehicle to Report File");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Comparing Vehicle to Report File");
        inOrder.verify(comparisonModule).reset();
        inOrder.verify(comparisonModule).compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(TOTAL_STEPS));
        inOrder.verify(dtcModule).setJ1939(j1939);

        step = SKIP_STEP;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Generating Header");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Generating Header");
        inOrder.verify(bannerModule).reportHeader(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Gathering File Information");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Gathering File Information");
        inOrder.verify(reportFileModule).reportFileInformation(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Connection Speed");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Connection Speed");
        inOrder.verify(vehicleInformationModule).reportConnectionSpeed(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Address Claim");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Address Claim");
        inOrder.verify(vehicleInformationModule).reportAddressClaim(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting VIN");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting VIN");
        inOrder.verify(vehicleInformationModule).reportVin(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting Calibration Information");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting Calibration Information");
        inOrder.verify(vehicleInformationModule).reportCalibrationInformation(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).getMinutesSinceCodeClear();
        inOrder.verify(diagnosticReadinessModule).reportDM21(any(ResultsListener.class), eq((double) 1234));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting HD OBD Modules");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting HD OBD Modules");
        inOrder.verify(diagnosticReadinessModule).getOBDModules(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting Component Identification");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting Component Identification");
        inOrder.verify(vehicleInformationModule).reportComponentIdentification(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(reportFileModule).isNewFile();
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Existing file; Codes Not Cleared");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Existing file; Codes Not Cleared");
        inOrder.verify(listener).onResult("Existing file; Codes Not Cleared");
        inOrder.verify(reportFileModule).onResult("Existing file; Codes Not Cleared");

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM5");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM5");
        inOrder.verify(diagnosticReadinessModule).reportDM5(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM26");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM26");
        inOrder.verify(diagnosticReadinessModule).reportDM26(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM20");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM20");
        inOrder.verify(diagnosticReadinessModule).reportDM20(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM6");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM6");
        inOrder.verify(dtcModule).reportDM6(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM12");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM12");
        inOrder.verify(dtcModule).reportDM12(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM23");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM23");
        inOrder.verify(dtcModule).reportDM23(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM28");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM28");
        inOrder.verify(dtcModule).reportDM28(any(ResultsListener.class));

        inOrder.verify(listener).onMessage("There were Diagnostic Trouble Codes reported.", "DTCs Exist",
                JOptionPane.WARNING_MESSAGE);
        inOrder.verify(reportFileModule).onMessage("There were Diagnostic Trouble Codes reported.", "DTCs Exist",
                JOptionPane.WARNING_MESSAGE);

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).getMinutesSinceCodeClear();
        inOrder.verify(diagnosticReadinessModule).reportDM21(any(ResultsListener.class), eq((double) 0));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Vehicle Distance");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Vehicle Distance");
        inOrder.verify(vehicleInformationModule).reportVehicleDistance(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting Engine Hours");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting Engine Hours");
        inOrder.verify(vehicleInformationModule).reportEngineHours(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Generating Quality Information");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Generating Quality Information");
        inOrder.verify(reportFileModule).reportAndResetCommunicationQuality(any(ResultsListener.class));

        inOrder.verify(reportFileModule).setNewFile(false);
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(bannerModule).reportFooter(any(ResultsListener.class));
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");

        inOrder.verify(bannerModule).getTypeName();
        inOrder.verify(listener).onProgress(TOTAL_STEPS, TOTAL_STEPS, "Data Plate Completed");
        inOrder.verify(reportFileModule).onProgress(TOTAL_STEPS, TOTAL_STEPS, "Data Plate Completed");
        inOrder.verify(listener).onComplete(true);
        inOrder.verify(reportFileModule).onComplete(true);
    }

    @Test
    public void testDisplayWarningForDM28s() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);
        when(comparisonModule.compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(TOTAL_STEPS))).thenReturn(true);
        when(reportFileModule.getMinutesSinceCodeClear()).thenReturn((double) 1234).thenReturn((double) 0);
        when(reportFileModule.isNewFile()).thenReturn(false);
        List<Integer> obdModules = Collections.singletonList(0x00);
        when(diagnosticReadinessModule.getOBDModules(any(ResultsListener.class))).thenReturn(obdModules);
        when(diagnosticReadinessModule.reportDM5(any(ResultsListener.class))).thenReturn(true);
        when(diagnosticReadinessModule.reportDM26(any(ResultsListener.class))).thenReturn(true);
        when(diagnosticReadinessModule.reportDM20(any(ResultsListener.class))).thenReturn(true);
        when(dtcModule.reportDM6(any(ResultsListener.class))).thenReturn(false);
        when(dtcModule.reportDM12(any(ResultsListener.class))).thenReturn(false);
        when(dtcModule.reportDM23(any(ResultsListener.class))).thenReturn(false);
        when(dtcModule.reportDM28(any(ResultsListener.class))).thenReturn(true);

        instance.execute(listener, j1939, reportFileModule);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        InOrder inOrder = inOrder(listener, engineSpeedModule, comparisonModule, dtcModule, bannerModule,
                reportFileModule, vehicleInformationModule, diagnosticReadinessModule);

        inOrder.verify(vehicleInformationModule).setJ1939(j1939);
        inOrder.verify(diagnosticReadinessModule).setJ1939(j1939);
        inOrder.verify(comparisonModule).setJ1939(j1939);
        inOrder.verify(engineSpeedModule).setJ1939(j1939);

        int step = 0;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "");

        step++;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Engine Speed");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Engine Speed");
        inOrder.verify(engineSpeedModule).isEngineCommunicating();

        step++;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Comparing Vehicle to Report File");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Comparing Vehicle to Report File");
        inOrder.verify(comparisonModule).reset();
        inOrder.verify(comparisonModule).compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(TOTAL_STEPS));
        inOrder.verify(dtcModule).setJ1939(j1939);

        step = SKIP_STEP;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Generating Header");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Generating Header");
        inOrder.verify(bannerModule).reportHeader(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Gathering File Information");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Gathering File Information");
        inOrder.verify(reportFileModule).reportFileInformation(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Connection Speed");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Connection Speed");
        inOrder.verify(vehicleInformationModule).reportConnectionSpeed(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Address Claim");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Address Claim");
        inOrder.verify(vehicleInformationModule).reportAddressClaim(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting VIN");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting VIN");
        inOrder.verify(vehicleInformationModule).reportVin(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting Calibration Information");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting Calibration Information");
        inOrder.verify(vehicleInformationModule).reportCalibrationInformation(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).getMinutesSinceCodeClear();
        inOrder.verify(diagnosticReadinessModule).reportDM21(any(ResultsListener.class), eq((double) 1234));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting HD OBD Modules");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting HD OBD Modules");
        inOrder.verify(diagnosticReadinessModule).getOBDModules(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting Component Identification");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting Component Identification");
        inOrder.verify(vehicleInformationModule).reportComponentIdentification(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(reportFileModule).isNewFile();
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Existing file; Codes Not Cleared");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Existing file; Codes Not Cleared");
        inOrder.verify(listener).onResult("Existing file; Codes Not Cleared");
        inOrder.verify(reportFileModule).onResult("Existing file; Codes Not Cleared");

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM5");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM5");
        inOrder.verify(diagnosticReadinessModule).reportDM5(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM26");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM26");
        inOrder.verify(diagnosticReadinessModule).reportDM26(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM20");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM20");
        inOrder.verify(diagnosticReadinessModule).reportDM20(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM6");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM6");
        inOrder.verify(dtcModule).reportDM6(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM12");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM12");
        inOrder.verify(dtcModule).reportDM12(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM23");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM23");
        inOrder.verify(dtcModule).reportDM23(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM28");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM28");
        inOrder.verify(dtcModule).reportDM28(any(ResultsListener.class));
        inOrder.verify(listener).onMessage("There were Diagnostic Trouble Codes reported.", "DTCs Exist",
                JOptionPane.WARNING_MESSAGE);
        inOrder.verify(reportFileModule).onMessage("There were Diagnostic Trouble Codes reported.", "DTCs Exist",
                JOptionPane.WARNING_MESSAGE);

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).getMinutesSinceCodeClear();
        inOrder.verify(diagnosticReadinessModule).reportDM21(any(ResultsListener.class), eq((double) 0));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Vehicle Distance");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Vehicle Distance");
        inOrder.verify(vehicleInformationModule).reportVehicleDistance(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting Engine Hours");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting Engine Hours");
        inOrder.verify(vehicleInformationModule).reportEngineHours(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Generating Quality Information");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Generating Quality Information");
        inOrder.verify(reportFileModule).reportAndResetCommunicationQuality(any(ResultsListener.class));

        inOrder.verify(reportFileModule).setNewFile(false);
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(bannerModule).reportFooter(any(ResultsListener.class));
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");

        inOrder.verify(bannerModule).getTypeName();
        inOrder.verify(listener).onProgress(TOTAL_STEPS, TOTAL_STEPS, "Data Plate Completed");
        inOrder.verify(reportFileModule).onProgress(TOTAL_STEPS, TOTAL_STEPS, "Data Plate Completed");
        inOrder.verify(listener).onComplete(true);
        inOrder.verify(reportFileModule).onComplete(true);
    }

    @Test
    public void testDisplayWarningForDM6s() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);
        when(comparisonModule.compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(TOTAL_STEPS))).thenReturn(true);
        when(reportFileModule.getMinutesSinceCodeClear()).thenReturn((double) 1234).thenReturn((double) 0);
        when(reportFileModule.isNewFile()).thenReturn(false);
        List<Integer> obdModules = Collections.singletonList(0x00);
        when(diagnosticReadinessModule.getOBDModules(any(ResultsListener.class))).thenReturn(obdModules);
        when(diagnosticReadinessModule.reportDM5(any(ResultsListener.class))).thenReturn(true);
        when(diagnosticReadinessModule.reportDM26(any(ResultsListener.class))).thenReturn(true);
        when(diagnosticReadinessModule.reportDM20(any(ResultsListener.class))).thenReturn(true);
        when(dtcModule.reportDM6(any(ResultsListener.class))).thenReturn(true);
        when(dtcModule.reportDM12(any(ResultsListener.class))).thenReturn(false);
        when(dtcModule.reportDM23(any(ResultsListener.class))).thenReturn(false);
        when(dtcModule.reportDM28(any(ResultsListener.class))).thenReturn(false);

        instance.execute(listener, j1939, reportFileModule);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        InOrder inOrder = inOrder(listener, engineSpeedModule, comparisonModule, dtcModule, bannerModule,
                reportFileModule, vehicleInformationModule, diagnosticReadinessModule);

        inOrder.verify(vehicleInformationModule).setJ1939(j1939);
        inOrder.verify(diagnosticReadinessModule).setJ1939(j1939);
        inOrder.verify(comparisonModule).setJ1939(j1939);
        inOrder.verify(engineSpeedModule).setJ1939(j1939);

        int step = 0;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "");

        step++;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Engine Speed");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Engine Speed");
        inOrder.verify(engineSpeedModule).isEngineCommunicating();

        step++;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Comparing Vehicle to Report File");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Comparing Vehicle to Report File");
        inOrder.verify(comparisonModule).reset();
        inOrder.verify(comparisonModule).compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(TOTAL_STEPS));
        inOrder.verify(dtcModule).setJ1939(j1939);

        step = SKIP_STEP;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Generating Header");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Generating Header");
        inOrder.verify(bannerModule).reportHeader(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Gathering File Information");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Gathering File Information");
        inOrder.verify(reportFileModule).reportFileInformation(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Connection Speed");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Connection Speed");
        inOrder.verify(vehicleInformationModule).reportConnectionSpeed(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Address Claim");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Address Claim");
        inOrder.verify(vehicleInformationModule).reportAddressClaim(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting VIN");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting VIN");
        inOrder.verify(vehicleInformationModule).reportVin(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting Calibration Information");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting Calibration Information");
        inOrder.verify(vehicleInformationModule).reportCalibrationInformation(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).getMinutesSinceCodeClear();
        inOrder.verify(diagnosticReadinessModule).reportDM21(any(ResultsListener.class), eq((double) 1234));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting HD OBD Modules");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting HD OBD Modules");
        inOrder.verify(diagnosticReadinessModule).getOBDModules(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting Component Identification");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting Component Identification");
        inOrder.verify(vehicleInformationModule).reportComponentIdentification(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(reportFileModule).isNewFile();
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Existing file; Codes Not Cleared");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Existing file; Codes Not Cleared");
        inOrder.verify(listener).onResult("Existing file; Codes Not Cleared");
        inOrder.verify(reportFileModule).onResult("Existing file; Codes Not Cleared");

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM5");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM5");
        inOrder.verify(diagnosticReadinessModule).reportDM5(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM26");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM26");
        inOrder.verify(diagnosticReadinessModule).reportDM26(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM20");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM20");
        inOrder.verify(diagnosticReadinessModule).reportDM20(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM6");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM6");
        inOrder.verify(dtcModule).reportDM6(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM12");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM12");
        inOrder.verify(dtcModule).reportDM12(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM23");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM23");
        inOrder.verify(dtcModule).reportDM23(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM28");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM28");
        inOrder.verify(dtcModule).reportDM28(any(ResultsListener.class));

        inOrder.verify(listener).onMessage("There were Diagnostic Trouble Codes reported.", "DTCs Exist",
                JOptionPane.WARNING_MESSAGE);
        inOrder.verify(reportFileModule).onMessage("There were Diagnostic Trouble Codes reported.", "DTCs Exist",
                JOptionPane.WARNING_MESSAGE);

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).getMinutesSinceCodeClear();
        inOrder.verify(diagnosticReadinessModule).reportDM21(any(ResultsListener.class), eq((double) 0));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Vehicle Distance");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Vehicle Distance");
        inOrder.verify(vehicleInformationModule).reportVehicleDistance(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting Engine Hours");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting Engine Hours");
        inOrder.verify(vehicleInformationModule).reportEngineHours(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Generating Quality Information");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Generating Quality Information");
        inOrder.verify(reportFileModule).reportAndResetCommunicationQuality(any(ResultsListener.class));

        inOrder.verify(reportFileModule).setNewFile(false);
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(bannerModule).reportFooter(any(ResultsListener.class));
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");

        inOrder.verify(bannerModule).getTypeName();
        inOrder.verify(listener).onProgress(TOTAL_STEPS, TOTAL_STEPS, "Data Plate Completed");
        inOrder.verify(reportFileModule).onProgress(TOTAL_STEPS, TOTAL_STEPS, "Data Plate Completed");
        inOrder.verify(listener).onComplete(true);
        inOrder.verify(reportFileModule).onComplete(true);
    }

    @Test
    public void testHappyPathWithExitingFile() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);
        when(comparisonModule.compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(TOTAL_STEPS))).thenReturn(true);
        when(reportFileModule.getMinutesSinceCodeClear()).thenReturn((double) 1234).thenReturn((double) 0);
        when(reportFileModule.isNewFile()).thenReturn(false);
        List<Integer> obdModules = Collections.singletonList(0x00);
        when(diagnosticReadinessModule.getOBDModules(any(ResultsListener.class))).thenReturn(obdModules);
        when(diagnosticReadinessModule.reportDM5(any(ResultsListener.class))).thenReturn(true);
        when(diagnosticReadinessModule.reportDM26(any(ResultsListener.class))).thenReturn(true);
        when(diagnosticReadinessModule.reportDM20(any(ResultsListener.class))).thenReturn(true);
        when(dtcModule.reportDM6(any(ResultsListener.class))).thenReturn(false);
        when(dtcModule.reportDM12(any(ResultsListener.class))).thenReturn(false);
        when(dtcModule.reportDM23(any(ResultsListener.class))).thenReturn(false);
        when(dtcModule.reportDM28(any(ResultsListener.class))).thenReturn(false);

        instance.execute(listener, j1939, reportFileModule);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        InOrder inOrder = inOrder(listener, engineSpeedModule, comparisonModule, dtcModule, bannerModule,
                reportFileModule, vehicleInformationModule, diagnosticReadinessModule);

        inOrder.verify(vehicleInformationModule).setJ1939(j1939);
        inOrder.verify(diagnosticReadinessModule).setJ1939(j1939);
        inOrder.verify(comparisonModule).setJ1939(j1939);
        inOrder.verify(engineSpeedModule).setJ1939(j1939);

        int step = 0;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "");

        step++;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Engine Speed");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Engine Speed");
        inOrder.verify(engineSpeedModule).isEngineCommunicating();

        step++;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Comparing Vehicle to Report File");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Comparing Vehicle to Report File");
        inOrder.verify(comparisonModule).reset();
        inOrder.verify(comparisonModule).compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(TOTAL_STEPS));
        inOrder.verify(dtcModule).setJ1939(j1939);

        step = SKIP_STEP;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Generating Header");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Generating Header");
        inOrder.verify(bannerModule).reportHeader(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Gathering File Information");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Gathering File Information");
        inOrder.verify(reportFileModule).reportFileInformation(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Connection Speed");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Connection Speed");
        inOrder.verify(vehicleInformationModule).reportConnectionSpeed(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Address Claim");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Address Claim");
        inOrder.verify(vehicleInformationModule).reportAddressClaim(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting VIN");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting VIN");
        inOrder.verify(vehicleInformationModule).reportVin(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting Calibration Information");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting Calibration Information");
        inOrder.verify(vehicleInformationModule).reportCalibrationInformation(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).getMinutesSinceCodeClear();
        inOrder.verify(diagnosticReadinessModule).reportDM21(any(ResultsListener.class), eq((double) 1234));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting HD OBD Modules");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting HD OBD Modules");
        inOrder.verify(diagnosticReadinessModule).getOBDModules(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting Component Identification");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting Component Identification");
        inOrder.verify(vehicleInformationModule).reportComponentIdentification(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(reportFileModule).isNewFile();
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Existing file; Codes Not Cleared");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Existing file; Codes Not Cleared");
        inOrder.verify(listener).onResult("Existing file; Codes Not Cleared");
        inOrder.verify(reportFileModule).onResult("Existing file; Codes Not Cleared");

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM5");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM5");
        inOrder.verify(diagnosticReadinessModule).reportDM5(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM26");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM26");
        inOrder.verify(diagnosticReadinessModule).reportDM26(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM20");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM20");
        inOrder.verify(diagnosticReadinessModule).reportDM20(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM6");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM6");
        inOrder.verify(dtcModule).reportDM6(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM12");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM12");
        inOrder.verify(dtcModule).reportDM12(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM23");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM23");
        inOrder.verify(dtcModule).reportDM23(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM28");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM28");
        inOrder.verify(dtcModule).reportDM28(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).getMinutesSinceCodeClear();
        inOrder.verify(diagnosticReadinessModule).reportDM21(any(ResultsListener.class), eq((double) 0));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Vehicle Distance");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Vehicle Distance");
        inOrder.verify(vehicleInformationModule).reportVehicleDistance(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting Engine Hours");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting Engine Hours");
        inOrder.verify(vehicleInformationModule).reportEngineHours(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Generating Quality Information");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Generating Quality Information");
        inOrder.verify(reportFileModule).reportAndResetCommunicationQuality(any(ResultsListener.class));

        inOrder.verify(reportFileModule).setNewFile(false);
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(bannerModule).reportFooter(any(ResultsListener.class));
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");

        inOrder.verify(bannerModule).getTypeName();
        inOrder.verify(listener).onProgress(TOTAL_STEPS, TOTAL_STEPS, "Data Plate Completed");
        inOrder.verify(reportFileModule).onProgress(TOTAL_STEPS, TOTAL_STEPS, "Data Plate Completed");
        inOrder.verify(listener).onComplete(true);
        inOrder.verify(reportFileModule).onComplete(true);
    }

    @Test
    public void testHappyPathWithNewFile() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);
        when(engineSpeedModule.isEngineNotRunning()).thenReturn(true);
        when(comparisonModule.compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(TOTAL_STEPS))).thenReturn(true);
        when(reportFileModule.getMinutesSinceCodeClear()).thenReturn((double) 1234).thenReturn((double) 0);
        when(reportFileModule.isNewFile()).thenReturn(true);
        List<Integer> obdModules = Collections.singletonList(0x00);
        when(dtcModule.reportDM11(any(ResultsListener.class), eq(obdModules))).thenReturn(true);
        when(diagnosticReadinessModule.getOBDModules(any(ResultsListener.class))).thenReturn(obdModules);
        when(diagnosticReadinessModule.reportDM5(any(ResultsListener.class))).thenReturn(true);
        when(diagnosticReadinessModule.reportDM26(any(ResultsListener.class))).thenReturn(true);
        when(diagnosticReadinessModule.reportDM20(any(ResultsListener.class))).thenReturn(true);
        when(dtcModule.reportDM6(any(ResultsListener.class))).thenReturn(false);
        when(dtcModule.reportDM12(any(ResultsListener.class))).thenReturn(false);
        when(dtcModule.reportDM23(any(ResultsListener.class))).thenReturn(false);
        when(dtcModule.reportDM28(any(ResultsListener.class))).thenReturn(false);

        instance.execute(listener, j1939, reportFileModule);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        InOrder inOrder = inOrder(listener, engineSpeedModule, comparisonModule, dtcModule, bannerModule,
                reportFileModule, vehicleInformationModule, diagnosticReadinessModule);

        inOrder.verify(vehicleInformationModule).setJ1939(j1939);
        inOrder.verify(diagnosticReadinessModule).setJ1939(j1939);
        inOrder.verify(comparisonModule).setJ1939(j1939);
        inOrder.verify(engineSpeedModule).setJ1939(j1939);

        int step = 0;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "");

        step++;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Engine Speed");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Engine Speed");
        inOrder.verify(engineSpeedModule).isEngineCommunicating();

        step++;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Comparing Vehicle to Report File");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Comparing Vehicle to Report File");
        inOrder.verify(comparisonModule).reset();
        inOrder.verify(comparisonModule).compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2),
                eq(TOTAL_STEPS));
        inOrder.verify(dtcModule).setJ1939(j1939);

        step = SKIP_STEP;
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Generating Header");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Generating Header");
        inOrder.verify(bannerModule).reportHeader(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Gathering File Information");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Gathering File Information");
        inOrder.verify(reportFileModule).reportFileInformation(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Connection Speed");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Connection Speed");
        inOrder.verify(vehicleInformationModule).reportConnectionSpeed(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Address Claim");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Address Claim");
        inOrder.verify(vehicleInformationModule).reportAddressClaim(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting VIN");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting VIN");
        inOrder.verify(vehicleInformationModule).reportVin(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting Calibration Information");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting Calibration Information");
        inOrder.verify(vehicleInformationModule).reportCalibrationInformation(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).getMinutesSinceCodeClear();
        inOrder.verify(diagnosticReadinessModule).reportDM21(any(ResultsListener.class), eq((double) 1234));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting HD OBD Modules");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting HD OBD Modules");
        inOrder.verify(diagnosticReadinessModule).getOBDModules(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting Component Identification");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting Component Identification");
        inOrder.verify(vehicleInformationModule).reportComponentIdentification(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(reportFileModule).isNewFile();
        inOrder.verify(engineSpeedModule).isEngineNotRunning();
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Clearing Active Codes");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Clearing Active Codes");
        inOrder.verify(dtcModule).reportDM11(any(ResultsListener.class), eq(obdModules));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM5");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM5");
        inOrder.verify(diagnosticReadinessModule).reportDM5(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM26");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM26");
        inOrder.verify(diagnosticReadinessModule).reportDM26(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM20");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM20");
        inOrder.verify(diagnosticReadinessModule).reportDM20(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM6");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM6");
        inOrder.verify(dtcModule).reportDM6(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM12");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM12");
        inOrder.verify(dtcModule).reportDM12(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM23");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM23");
        inOrder.verify(dtcModule).reportDM23(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM28");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM28");
        inOrder.verify(dtcModule).reportDM28(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting DM21");
        inOrder.verify(reportFileModule).getMinutesSinceCodeClear();
        inOrder.verify(diagnosticReadinessModule).reportDM21(any(ResultsListener.class), eq((double) 0));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Reading Vehicle Distance");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Reading Vehicle Distance");
        inOrder.verify(vehicleInformationModule).reportVehicleDistance(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Requesting Engine Hours");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Requesting Engine Hours");
        inOrder.verify(vehicleInformationModule).reportEngineHours(any(ResultsListener.class));

        step++;
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(listener).onProgress(step, TOTAL_STEPS, "Generating Quality Information");
        inOrder.verify(reportFileModule).onProgress(step, TOTAL_STEPS, "Generating Quality Information");
        inOrder.verify(reportFileModule).reportAndResetCommunicationQuality(any(ResultsListener.class));

        inOrder.verify(reportFileModule).setNewFile(false);
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");
        inOrder.verify(bannerModule).reportFooter(any(ResultsListener.class));
        inOrder.verify(listener).onResult("");
        inOrder.verify(reportFileModule).onResult("");

        inOrder.verify(bannerModule).getTypeName();
        inOrder.verify(listener).onProgress(TOTAL_STEPS, TOTAL_STEPS, "Data Plate Completed");
        inOrder.verify(reportFileModule).onProgress(TOTAL_STEPS, TOTAL_STEPS, "Data Plate Completed");
        inOrder.verify(listener).onComplete(true);
        inOrder.verify(reportFileModule).onComplete(true);
    }
}
