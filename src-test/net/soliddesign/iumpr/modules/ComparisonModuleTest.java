/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.modules;

import static net.soliddesign.iumpr.IUMPR.NL;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.swing.JOptionPane;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.soliddesign.iumpr.bus.j1939.J1939;
import net.soliddesign.iumpr.bus.j1939.packets.DM19CalibrationInformationPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM19CalibrationInformationPacket.CalibrationInformation;
import net.soliddesign.iumpr.bus.j1939.packets.DM21DiagnosticReadinessPacket;
import net.soliddesign.iumpr.bus.j1939.packets.VehicleIdentificationPacket;
import net.soliddesign.iumpr.controllers.ResultsListener;

/**
 * Unit tests for the {@link ComparisonModule} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", justification = "The verify method don't need checked")
public class ComparisonModuleTest {

    private ComparisonModule instance;

    @Mock
    private J1939 j1939;

    @Mock
    private ResultsListener listener;
    @Mock
    private ReportFileModule reportFileModule;

    @Before
    public void setUp() throws Exception {
        instance = new ComparisonModule();
        instance.setJ1939(j1939);
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(j1939, reportFileModule, listener);
    }

    @Test
    public void testCompareFileToVehicle() throws Exception {
        DM19CalibrationInformationPacket packet1 = mock(DM19CalibrationInformationPacket.class);
        List<CalibrationInformation> list1 = Collections.singletonList(new CalibrationInformation("id1", "cvn1"));
        when(packet1.getCalibrationInformation()).thenReturn(list1);

        DM19CalibrationInformationPacket packet2 = mock(DM19CalibrationInformationPacket.class);
        List<CalibrationInformation> list2 = Collections.singletonList(new CalibrationInformation("id2", "cvn2"));
        when(packet2.getCalibrationInformation()).thenReturn(list2);
        when(j1939.requestMultiple(DM19CalibrationInformationPacket.class)).thenReturn(Stream.of(packet1, packet2));

        Set<CalibrationInformation> fileCals = new HashSet<>();
        fileCals.add(new CalibrationInformation("id1", "cvn1"));
        fileCals.add(new CalibrationInformation("id2", "cvn2"));
        when(reportFileModule.getCalibrations()).thenReturn(fileCals);

        DM21DiagnosticReadinessPacket packet = mock(DM21DiagnosticReadinessPacket.class);
        when(packet.getMinutesSinceDTCsCleared()).thenReturn((double) 314);
        when(j1939.requestMultiple(DM21DiagnosticReadinessPacket.class)).thenReturn(Stream.of(packet));

        when(reportFileModule.getMinutesSinceCodeClear()).thenReturn((double) 314);

        VehicleIdentificationPacket vinPacket = mock(VehicleIdentificationPacket.class);
        when(vinPacket.getVin()).thenReturn("12345678901234567890");
        when(j1939.requestMultiple(VehicleIdentificationPacket.class)).thenReturn(Stream.of(vinPacket));

        when(reportFileModule.getFileVin()).thenReturn("12345678901234567890");

        when(reportFileModule.isNewFile()).thenReturn(false);

        assertEquals(true, instance.compareFileToVehicle(listener, reportFileModule, 0, 4));

        verify(j1939).requestMultiple(DM19CalibrationInformationPacket.class);
        verify(j1939).requestMultiple(DM21DiagnosticReadinessPacket.class);
        verify(j1939).requestMultiple(VehicleIdentificationPacket.class);

        verify(reportFileModule).isNewFile();
        verify(reportFileModule).getCalibrations();
        verify(reportFileModule).getMinutesSinceCodeClear();
        verify(reportFileModule).getFileVin();

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onProgress(1, 4, "Reading VIN from Vehicle");
        inOrder.verify(listener).onProgress(2, 4, "Reading Calibrations from Vehicle");
        inOrder.verify(listener).onProgress(3, 4, "Reading Time Since Code Cleared from Vehicle");
        inOrder.verify(listener).onProgress(4, 4, "Selected Report File Matches Connected Vehicle");
    }

    @Test
    public void testCompareFileToVehicleThenResetThenCompareFileToVehicle() throws Exception {
        DM19CalibrationInformationPacket packet1 = mock(DM19CalibrationInformationPacket.class);
        List<CalibrationInformation> list1 = Collections.singletonList(new CalibrationInformation("id1", "cvn1"));
        when(packet1.getCalibrationInformation()).thenReturn(list1).thenReturn(list1);

        DM19CalibrationInformationPacket packet2 = mock(DM19CalibrationInformationPacket.class);
        List<CalibrationInformation> list2 = Collections.singletonList(new CalibrationInformation("id2", "cvn2"));
        when(packet2.getCalibrationInformation()).thenReturn(list2).thenReturn(list2);
        when(j1939.requestMultiple(DM19CalibrationInformationPacket.class)).thenReturn(Stream.of(packet1, packet2))
                .thenReturn(Stream.of(packet1, packet2));

        Set<CalibrationInformation> fileCals = new HashSet<>();
        fileCals.add(new CalibrationInformation("id1", "cvn1"));
        fileCals.add(new CalibrationInformation("id2", "cvn2"));
        when(reportFileModule.getCalibrations()).thenReturn(fileCals).thenReturn(fileCals);

        DM21DiagnosticReadinessPacket packet = mock(DM21DiagnosticReadinessPacket.class);
        when(packet.getMinutesSinceDTCsCleared()).thenReturn((double) 314);
        when(j1939.requestMultiple(DM21DiagnosticReadinessPacket.class)).thenReturn(Stream.of(packet))
                .thenReturn(Stream.of(packet));

        when(reportFileModule.getMinutesSinceCodeClear()).thenReturn((double) 314);

        VehicleIdentificationPacket vinPacket = mock(VehicleIdentificationPacket.class);
        when(vinPacket.getVin()).thenReturn("12345678901234567890");
        when(j1939.requestMultiple(VehicleIdentificationPacket.class)).thenReturn(Stream.of(vinPacket))
                .thenReturn(Stream.of(vinPacket));

        when(reportFileModule.getFileVin()).thenReturn("12345678901234567890");
        when(reportFileModule.isNewFile()).thenReturn(false);

        assertEquals(true, instance.compareFileToVehicle(listener, reportFileModule, 0, 4));
        instance.reset();
        assertEquals(true, instance.compareFileToVehicle(listener, reportFileModule, 0, 4));

        verify(j1939, times(2)).requestMultiple(DM19CalibrationInformationPacket.class);
        verify(j1939, times(2)).requestMultiple(DM21DiagnosticReadinessPacket.class);
        verify(j1939, times(2)).requestMultiple(VehicleIdentificationPacket.class);

        verify(reportFileModule, times(2)).isNewFile();
        verify(reportFileModule, times(2)).getCalibrations();
        verify(reportFileModule, times(2)).getMinutesSinceCodeClear();
        verify(reportFileModule, times(2)).getFileVin();

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onProgress(1, 4, "Reading VIN from Vehicle");
        inOrder.verify(listener).onProgress(2, 4, "Reading Calibrations from Vehicle");
        inOrder.verify(listener).onProgress(3, 4, "Reading Time Since Code Cleared from Vehicle");
        inOrder.verify(listener).onProgress(4, 4, "Selected Report File Matches Connected Vehicle");
        inOrder.verify(listener).onProgress(1, 4, "Reading VIN from Vehicle");
        inOrder.verify(listener).onProgress(2, 4, "Reading Calibrations from Vehicle");
        inOrder.verify(listener).onProgress(3, 4, "Reading Time Since Code Cleared from Vehicle");
        inOrder.verify(listener).onProgress(4, 4, "Selected Report File Matches Connected Vehicle");
    }

    @Test
    public void testCompareFileToVehicleWithCalMismatch() throws Exception {
        DM19CalibrationInformationPacket packet1 = mock(DM19CalibrationInformationPacket.class);
        List<CalibrationInformation> list1 = Collections.singletonList(new CalibrationInformation("id1", "cvn1"));
        when(packet1.getCalibrationInformation()).thenReturn(list1);

        DM19CalibrationInformationPacket packet2 = mock(DM19CalibrationInformationPacket.class);
        List<CalibrationInformation> list2 = Collections.singletonList(new CalibrationInformation("id2", "cvn2"));
        when(packet2.getCalibrationInformation()).thenReturn(list2);
        when(j1939.requestMultiple(DM19CalibrationInformationPacket.class)).thenReturn(Stream.of(packet1, packet2));

        Set<CalibrationInformation> fileCals = new HashSet<>();
        fileCals.add(new CalibrationInformation("id1", "cvn1"));
        fileCals.add(new CalibrationInformation("id3", "cvn3"));
        when(reportFileModule.getCalibrations()).thenReturn(fileCals);

        VehicleIdentificationPacket vinPacket = mock(VehicleIdentificationPacket.class);
        when(vinPacket.getVin()).thenReturn("12345678901234567890");
        when(j1939.requestMultiple(VehicleIdentificationPacket.class)).thenReturn(Stream.of(vinPacket));

        when(reportFileModule.getFileVin()).thenReturn("12345678901234567890");

        when(reportFileModule.isNewFile()).thenReturn(false);
        assertEquals(false, instance.compareFileToVehicle(listener, reportFileModule, 0, 4));

        verify(j1939).requestMultiple(DM19CalibrationInformationPacket.class);
        verify(j1939).requestMultiple(VehicleIdentificationPacket.class);

        verify(reportFileModule).isNewFile();
        verify(reportFileModule, times(2)).getCalibrations();
        verify(reportFileModule).getFileVin();

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onProgress(1, 4, "Reading VIN from Vehicle");
        inOrder.verify(listener).onProgress(2, 4, "Reading Calibrations from Vehicle");
        inOrder.verify(listener).onProgress(4, 4, "Calibration Mismatch");

        String expected = "";
        expected += "The selected report file calibrations do not match the vehicle calibrations." + NL + NL;
        expected += "The Report Calibrations:" + NL;
        expected += "CAL ID of id1 and CVN of cvn1" + NL;
        expected += "CAL ID of id3 and CVN of cvn3" + NL + NL;

        expected += "The Vehicle Calibrations:" + NL;
        expected += "CAL ID of id1 and CVN of cvn1" + NL;
        expected += "CAL ID of id2 and CVN of cvn2";
        verify(listener).onMessage(expected, "Calibrations Mismatch", JOptionPane.ERROR_MESSAGE);
    }

    @Test
    public void testCompareFileToVehicleWithCalTimeout() throws Exception {
        when(j1939.requestMultiple(DM19CalibrationInformationPacket.class)).thenReturn(Stream.empty());

        Set<CalibrationInformation> fileCals = new HashSet<>();
        fileCals.add(new CalibrationInformation("id1", "cvn1"));
        fileCals.add(new CalibrationInformation("id3", "cvn3"));
        when(reportFileModule.getCalibrations()).thenReturn(fileCals);

        VehicleIdentificationPacket vinPacket = mock(VehicleIdentificationPacket.class);
        when(vinPacket.getVin()).thenReturn("12345678901234567890");
        when(j1939.requestMultiple(VehicleIdentificationPacket.class)).thenReturn(Stream.of(vinPacket));

        when(reportFileModule.getFileVin()).thenReturn("12345678901234567890");

        when(reportFileModule.isNewFile()).thenReturn(false);

        try {
            instance.compareFileToVehicle(listener, reportFileModule, 0, 4);
        } catch (IOException e) {
            assertEquals("Timeout Error Reading Calibrations", e.getMessage());
        }

        verify(j1939).requestMultiple(DM19CalibrationInformationPacket.class);
        verify(j1939).requestMultiple(VehicleIdentificationPacket.class);

        verify(reportFileModule).isNewFile();
        verify(reportFileModule).getCalibrations();
        verify(reportFileModule).getFileVin();

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onProgress(1, 4, "Reading VIN from Vehicle");
        inOrder.verify(listener).onProgress(2, 4, "Reading Calibrations from Vehicle");
    }

    @Test
    public void testCompareFileToVehicleWithNewFile() throws Exception {
        when(reportFileModule.isNewFile()).thenReturn(true);
        assertEquals(true, instance.compareFileToVehicle(listener, reportFileModule, 0, 4));
        verify(reportFileModule).isNewFile();
        verify(listener).onProgress(4, 4, "Selected Report File is new - not checked with vehicle.");
    }

    @Test
    public void testCompareFileToVehicleWithTSCCGap() throws Exception {
        DM19CalibrationInformationPacket packet1 = mock(DM19CalibrationInformationPacket.class);
        List<CalibrationInformation> list1 = Collections.singletonList(new CalibrationInformation("id1", "cvn1"));
        when(packet1.getCalibrationInformation()).thenReturn(list1);

        DM19CalibrationInformationPacket packet2 = mock(DM19CalibrationInformationPacket.class);
        List<CalibrationInformation> list2 = Collections.singletonList(new CalibrationInformation("id2", "cvn2"));
        when(packet2.getCalibrationInformation()).thenReturn(list2);
        when(j1939.requestMultiple(DM19CalibrationInformationPacket.class)).thenReturn(Stream.of(packet1, packet2));

        Set<CalibrationInformation> fileCals = new HashSet<>();
        fileCals.add(new CalibrationInformation("id1", "cvn1"));
        fileCals.add(new CalibrationInformation("id2", "cvn2"));
        when(reportFileModule.getCalibrations()).thenReturn(fileCals);

        DM21DiagnosticReadinessPacket packet21 = mock(DM21DiagnosticReadinessPacket.class);
        when(packet21.getMinutesSinceDTCsCleared()).thenReturn((double) 314);
        DM21DiagnosticReadinessPacket packet22 = mock(DM21DiagnosticReadinessPacket.class);
        when(packet22.getMinutesSinceDTCsCleared()).thenReturn((double) 99);
        when(j1939.requestMultiple(DM21DiagnosticReadinessPacket.class)).thenReturn(Stream.of(packet21, packet22));

        when(reportFileModule.getMinutesSinceCodeClear()).thenReturn((double) 200);

        VehicleIdentificationPacket vinPacket = mock(VehicleIdentificationPacket.class);
        when(vinPacket.getVin()).thenReturn("12345678901234567890");
        when(j1939.requestMultiple(VehicleIdentificationPacket.class)).thenReturn(Stream.of(vinPacket));

        when(reportFileModule.getFileVin()).thenReturn("12345678901234567890");
        when(reportFileModule.isNewFile()).thenReturn(false);

        assertEquals(true, instance.compareFileToVehicle(listener, reportFileModule, 0, 4));

        verify(j1939).requestMultiple(DM19CalibrationInformationPacket.class);
        verify(j1939).requestMultiple(DM21DiagnosticReadinessPacket.class);
        verify(j1939).requestMultiple(VehicleIdentificationPacket.class);

        verify(reportFileModule).isNewFile();
        verify(reportFileModule).getCalibrations();
        verify(reportFileModule).getMinutesSinceCodeClear();
        verify(reportFileModule).getFileVin();

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onProgress(1, 4, "Reading VIN from Vehicle");
        inOrder.verify(listener).onProgress(2, 4, "Reading Calibrations from Vehicle");
        inOrder.verify(listener).onProgress(3, 4, "Reading Time Since Code Cleared from Vehicle");
        inOrder.verify(listener).onProgress(4, 4, "Selected Report File Matches Connected Vehicle");

        verify(listener).onMessage("The Time Since Code Cleared has an excessive gap of 114 minutes.",
                "Time SCC Excess Gap Error", JOptionPane.WARNING_MESSAGE);
    }

    @Test
    public void testCompareFileToVehicleWithTSCCReset() throws Exception {
        DM19CalibrationInformationPacket packet1 = mock(DM19CalibrationInformationPacket.class);
        List<CalibrationInformation> list1 = Collections.singletonList(new CalibrationInformation("id1", "cvn1"));
        when(packet1.getCalibrationInformation()).thenReturn(list1);

        DM19CalibrationInformationPacket packet2 = mock(DM19CalibrationInformationPacket.class);
        List<CalibrationInformation> list2 = Collections.singletonList(new CalibrationInformation("id2", "cvn2"));
        when(packet2.getCalibrationInformation()).thenReturn(list2);
        when(j1939.requestMultiple(DM19CalibrationInformationPacket.class)).thenReturn(Stream.of(packet1, packet2));

        Set<CalibrationInformation> fileCals = new HashSet<>();
        fileCals.add(new CalibrationInformation("id1", "cvn1"));
        fileCals.add(new CalibrationInformation("id2", "cvn2"));
        when(reportFileModule.getCalibrations()).thenReturn(fileCals);

        DM21DiagnosticReadinessPacket packet = mock(DM21DiagnosticReadinessPacket.class);
        when(packet.getMinutesSinceDTCsCleared()).thenReturn((double) 0);
        when(j1939.requestMultiple(DM21DiagnosticReadinessPacket.class)).thenReturn(Stream.of(packet));

        when(reportFileModule.getMinutesSinceCodeClear()).thenReturn((double) 200);

        VehicleIdentificationPacket vinPacket = mock(VehicleIdentificationPacket.class);
        when(vinPacket.getVin()).thenReturn("12345678901234567890");
        when(j1939.requestMultiple(VehicleIdentificationPacket.class)).thenReturn(Stream.of(vinPacket));

        when(reportFileModule.getFileVin()).thenReturn("12345678901234567890");
        when(reportFileModule.isNewFile()).thenReturn(false);

        assertEquals(true, instance.compareFileToVehicle(listener, reportFileModule, 0, 4));

        verify(j1939).requestMultiple(DM19CalibrationInformationPacket.class);
        verify(j1939).requestMultiple(DM21DiagnosticReadinessPacket.class);
        verify(j1939).requestMultiple(VehicleIdentificationPacket.class);

        verify(reportFileModule).isNewFile();
        verify(reportFileModule).getCalibrations();
        verify(reportFileModule).getMinutesSinceCodeClear();
        verify(reportFileModule).getFileVin();

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onProgress(1, 4, "Reading VIN from Vehicle");
        inOrder.verify(listener).onProgress(2, 4, "Reading Calibrations from Vehicle");
        inOrder.verify(listener).onProgress(3, 4, "Reading Time Since Code Cleared from Vehicle");
        inOrder.verify(listener).onProgress(4, 4, "Selected Report File Matches Connected Vehicle");

        verify(listener).onMessage("The Time Since Code Cleared was reset. The difference is 200 minutes.",
                "Time SCC Reset Error", JOptionPane.WARNING_MESSAGE);
    }

    @Test
    public void testCompareFileToVehicleWithTSCCTimeout() throws Exception {
        DM19CalibrationInformationPacket packet1 = mock(DM19CalibrationInformationPacket.class);
        List<CalibrationInformation> list1 = Collections.singletonList(new CalibrationInformation("id1", "cvn1"));
        when(packet1.getCalibrationInformation()).thenReturn(list1);

        DM19CalibrationInformationPacket packet2 = mock(DM19CalibrationInformationPacket.class);
        List<CalibrationInformation> list2 = Collections.singletonList(new CalibrationInformation("id2", "cvn2"));
        when(packet2.getCalibrationInformation()).thenReturn(list2);
        when(j1939.requestMultiple(DM19CalibrationInformationPacket.class)).thenReturn(Stream.of(packet1, packet2));

        Set<CalibrationInformation> fileCals = new HashSet<>();
        fileCals.add(new CalibrationInformation("id1", "cvn1"));
        fileCals.add(new CalibrationInformation("id2", "cvn2"));
        when(reportFileModule.getCalibrations()).thenReturn(fileCals);

        when(j1939.requestMultiple(DM21DiagnosticReadinessPacket.class)).thenReturn(Stream.empty());

        VehicleIdentificationPacket vinPacket = mock(VehicleIdentificationPacket.class);
        when(vinPacket.getVin()).thenReturn("12345678901234567890");
        when(j1939.requestMultiple(VehicleIdentificationPacket.class)).thenReturn(Stream.of(vinPacket));

        when(reportFileModule.getFileVin()).thenReturn("12345678901234567890");
        when(reportFileModule.isNewFile()).thenReturn(false);

        try {
            instance.compareFileToVehicle(listener, reportFileModule, 0, 4);
        } catch (IOException e) {
            assertEquals("Timeout Error Reading Time Since Code Cleared", e.getMessage());
        }

        verify(j1939).requestMultiple(DM19CalibrationInformationPacket.class);
        verify(j1939).requestMultiple(DM21DiagnosticReadinessPacket.class);
        verify(j1939).requestMultiple(VehicleIdentificationPacket.class);

        verify(reportFileModule).isNewFile();
        verify(reportFileModule).getCalibrations();
        verify(reportFileModule).getFileVin();

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onProgress(1, 4, "Reading VIN from Vehicle");
        inOrder.verify(listener).onProgress(2, 4, "Reading Calibrations from Vehicle");
        inOrder.verify(listener).onProgress(3, 4, "Reading Time Since Code Cleared from Vehicle");
    }

    @Test
    public void testCompareFileToVehicleWithVinMismatch() throws Exception {
        VehicleIdentificationPacket vinPacket = mock(VehicleIdentificationPacket.class);
        when(vinPacket.getVin()).thenReturn("12345678901234567890");
        when(j1939.requestMultiple(VehicleIdentificationPacket.class)).thenReturn(Stream.of(vinPacket));

        when(reportFileModule.getFileVin()).thenReturn("09876543210987654321");
        when(reportFileModule.isNewFile()).thenReturn(false);

        assertEquals(false, instance.compareFileToVehicle(listener, reportFileModule, 0, 4));

        verify(j1939).requestMultiple(VehicleIdentificationPacket.class);

        verify(reportFileModule).isNewFile();
        verify(reportFileModule, times(2)).getFileVin();

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onProgress(1, 4, "Reading VIN from Vehicle");
        inOrder.verify(listener).onProgress(4, 4, "VIN Mismatch");

        String expected = "The VIN found in the selected report file (09876543210987654321) does not match the VIN read from the vehicle (12345678901234567890).";
        verify(listener).onMessage(expected, "VIN Mismatch", JOptionPane.ERROR_MESSAGE);
    }

    @Test
    public void testCompareFileToVehicleWithVinTimeout() throws Exception {
        when(j1939.requestMultiple(VehicleIdentificationPacket.class)).thenReturn(Stream.empty());

        when(reportFileModule.getFileVin()).thenReturn("09876543210987654321");
        when(reportFileModule.isNewFile()).thenReturn(false);

        try {
            instance.compareFileToVehicle(listener, reportFileModule, 0, 4);
        } catch (IOException e) {
            assertEquals("Timeout Error Reading VIN", e.getMessage());
        }

        verify(j1939).requestMultiple(VehicleIdentificationPacket.class);

        verify(reportFileModule).isNewFile();
        verify(reportFileModule).getFileVin();

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onProgress(1, 4, "Reading VIN from Vehicle");
    }

    @Test
    public void testGetCalibrationsAsString() throws Exception {
        DM19CalibrationInformationPacket packet1 = mock(DM19CalibrationInformationPacket.class);
        List<CalibrationInformation> list1 = Collections.singletonList(new CalibrationInformation("id1", "cvn1"));
        when(packet1.getCalibrationInformation()).thenReturn(list1);

        DM19CalibrationInformationPacket packet2 = mock(DM19CalibrationInformationPacket.class);
        List<CalibrationInformation> list2 = Collections.singletonList(new CalibrationInformation("id2", "cvn2"));
        when(packet2.getCalibrationInformation()).thenReturn(list2);

        when(j1939.requestMultiple(DM19CalibrationInformationPacket.class)).thenReturn(Stream.of(packet1, packet2));

        String expected = "CAL ID of id1 and CVN of cvn1" + NL + "CAL ID of id2 and CVN of cvn2";

        assertEquals(expected, instance.getCalibrationsAsString());
        verify(j1939).requestMultiple(DM19CalibrationInformationPacket.class);

        // Make sure it's cached
        assertEquals(expected, instance.getCalibrationsAsString());
    }

    @Test
    public void testGetCalibrationsAsStringWithNoResponse() throws Exception {
        when(j1939.requestMultiple(DM19CalibrationInformationPacket.class)).thenReturn(Stream.empty());
        try {
            instance.getCalibrationsAsString();
        } catch (IOException e) {
            assertEquals("Timeout Error Reading Calibrations", e.getMessage());
        }
        verify(j1939).requestMultiple(DM19CalibrationInformationPacket.class);
    }

    @Test
    public void testGetMinutesSinceCodeClear() throws Exception {
        DM21DiagnosticReadinessPacket packet = mock(DM21DiagnosticReadinessPacket.class);
        when(packet.getMinutesSinceDTCsCleared()).thenReturn((double) 314);
        when(j1939.requestMultiple(DM21DiagnosticReadinessPacket.class)).thenReturn(Stream.of(packet));
        assertEquals(314, instance.getMinutesSinceCodeClear(), 0.0001);
        verify(j1939).requestMultiple(DM21DiagnosticReadinessPacket.class);
        // Make sure it's cached
        assertEquals(314, instance.getMinutesSinceCodeClear(), 0.0001);
    }

    @Test
    public void testGetMinutesSinceCodeClearWithNoResponse() throws Exception {
        when(j1939.requestMultiple(DM21DiagnosticReadinessPacket.class)).thenReturn(Stream.empty());
        try {
            instance.getMinutesSinceCodeClear();
        } catch (IOException e) {
            assertEquals("Timeout Error Reading Time Since Code Cleared", e.getMessage());
        }
        verify(j1939).requestMultiple(DM21DiagnosticReadinessPacket.class);
    }

    @Test
    public void testGetVin() throws Exception {
        VehicleIdentificationPacket vinPacket1 = mock(VehicleIdentificationPacket.class);
        when(vinPacket1.getVin()).thenReturn("12345678901234567890");
        VehicleIdentificationPacket vinPacket2 = mock(VehicleIdentificationPacket.class);
        when(vinPacket2.getVin()).thenReturn("12345678901234567890");
        when(j1939.requestMultiple(VehicleIdentificationPacket.class)).thenReturn(Stream.of(vinPacket1, vinPacket2));
        assertEquals("12345678901234567890", instance.getVin());
        verify(j1939).requestMultiple(VehicleIdentificationPacket.class);
        // Make sure it's cached
        assertEquals("12345678901234567890", instance.getVin());
    }

    @Test
    public void testGetVinWithDifferentResponses() throws Exception {
        VehicleIdentificationPacket vinPacket1 = mock(VehicleIdentificationPacket.class);
        when(vinPacket1.getVin()).thenReturn("12345678901234567890");
        VehicleIdentificationPacket vinPacket2 = mock(VehicleIdentificationPacket.class);
        when(vinPacket2.getVin()).thenReturn("01234567890123456789");
        when(j1939.requestMultiple(VehicleIdentificationPacket.class)).thenReturn(Stream.of(vinPacket1, vinPacket2));
        try {
            instance.getVin();
        } catch (IOException e) {
            assertEquals("Different VINs Received", e.getMessage());
        }
        verify(j1939).requestMultiple(VehicleIdentificationPacket.class);
    }

    @Test
    public void testGetVinWithNoResponse() throws Exception {
        when(j1939.requestMultiple(VehicleIdentificationPacket.class)).thenReturn(Stream.empty());
        try {
            instance.getVin();
        } catch (IOException e) {
            assertEquals("Timeout Error Reading VIN", e.getMessage());
        }
        verify(j1939).requestMultiple(VehicleIdentificationPacket.class);
    }
}
