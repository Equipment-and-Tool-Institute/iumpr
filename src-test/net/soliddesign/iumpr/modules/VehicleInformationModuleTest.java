/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.modules;

import static net.soliddesign.iumpr.IUMPR.NL;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.etools.j1939tools.bus.Bus;
import org.etools.j1939tools.bus.BusException;
import org.etools.j1939tools.bus.Either;
import org.etools.j1939tools.bus.Packet;
import org.etools.j1939tools.j1939.J1939;
import org.etools.j1939tools.j1939.packets.AddressClaimPacket;
import org.etools.j1939tools.j1939.packets.ComponentIdentificationPacket;
import org.etools.j1939tools.j1939.packets.DM19CalibrationInformationPacket;
import org.etools.j1939tools.j1939.packets.EngineHoursPacket;
import org.etools.j1939tools.j1939.packets.HighResVehicleDistancePacket;
import org.etools.j1939tools.j1939.packets.TotalVehicleDistancePacket;
import org.etools.j1939tools.j1939.packets.VehicleIdentificationPacket;
import org.etools.j1939tools.modules.DateTimeModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.soliddesign.iumpr.controllers.TestResultsListener;

/**
 * Unit tests for the {@link VehicleInformationModule} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", justification = "The values returned are properly ignored on verify statements.")
@RunWith(MockitoJUnitRunner.class)
public class VehicleInformationModuleTest {

    private static final int BUS_ADDR = 0xA5;

    private static final Charset UTF8 = StandardCharsets.UTF_8;

    private final DateTimeModule dateTimeModule = new TestDateTimeModule();

    private VehicleInformationModule instance;

    @Mock
    private J1939 j1939;

    @Before
    public void setup() {
        instance = new VehicleInformationModule(dateTimeModule);
        instance.setJ1939(j1939);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(j1939);
    }

    @Test
    public void testReportAddressClaim() {
        final int pgn = AddressClaimPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0xFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        AddressClaimPacket packet1 = new AddressClaimPacket(Packet.parse("18EEFF55 10 F7 45 01 00 45 00 01"));
        AddressClaimPacket packet2 = new AddressClaimPacket(Packet.parse("18EEFF3D 00 00 00 00 00 00 00 00"));
        AddressClaimPacket packet3 = new AddressClaimPacket(Packet.parse("18EEFF00 00 00 40 05 00 00 65 14"));
        when(j1939.requestMultiple(AddressClaimPacket.class, requestPacket))
                .thenReturn(Stream.of(packet1, packet2, packet3));

        String expected = "2007-12-03T10:15:30.000 Global Request for Address Claim" + NL
                + "10:15:30.000 18EAFFA5 [3] 00 EE 00 (TX)" + NL
                + "10:15:30.000 18EEFF55 [8] 10 F7 45 01 00 45 00 01" + NL
                + "DPF Controller (85) reported as: {" + NL
                + "  Industry Group: Global" + NL
                + "  Vehicle System: Non-specific System, System Instance: 1" + NL
                + "  Function: Engine Emission Aftertreatment System, Functional Instance: 0, ECU Instance: 0" + NL
                + "  Manufactured by: Cummins Inc (formerly Cummins Engine Co), Identity Number: 390928" + NL
                + "  Is not arbitrary address capable." + NL
                + "}" + NL
                + "10:15:30.000 18EEFF3D [8] 00 00 00 00 00 00 00 00" + NL
                + "Exhaust Emission Controller (61) reported as: {" + NL
                + "  Industry Group: Global" + NL
                + "  Vehicle System: Non-specific System, System Instance: 0" + NL
                + "  Function: Engine, Functional Instance: 0, ECU Instance: 0" + NL
                + "  Manufactured by: Reserved, Identity Number: 0" + NL
                + "  Is not arbitrary address capable." + NL
                + "}" + NL
                + "10:15:30.000 18EEFF00 [8] 00 00 40 05 00 00 65 14" + NL
                + "Engine #1 (0) reported as: {" + NL
                + "  Industry Group: On-Highway Equipment" + NL
                + "  Vehicle System: Unknown System (50), System Instance: 4" + NL
                + "  Function: Unknown Function (0), Functional Instance: 0, ECU Instance: 0" + NL
                + "  Manufactured by: International Truck and Engine Corporation - Engine Electronics  (formerly Navistar Intl Trans Co., Engine Electronics), Identity Number: 0"
                + NL
                + "  Is not arbitrary address capable." + NL
                + "}" + NL;
        TestResultsListener listener = new TestResultsListener();
        instance.reportAddressClaim(listener);
        assertEquals(expected, listener.getResults());
        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(AddressClaimPacket.class, requestPacket);
    }

    @Test
    public void testReportAddressClaimNoFunction0() {
        final int pgn = AddressClaimPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0xFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        AddressClaimPacket packet1 = new AddressClaimPacket(Packet.parse("18EEFF55 10 F7 45 01 00 45 00 01"));
        when(j1939.requestMultiple(AddressClaimPacket.class, requestPacket))
                .thenReturn(Stream.of(packet1));

        String expected = "2007-12-03T10:15:30.000 Global Request for Address Claim" + NL
                + "10:15:30.000 18EAFFA5 [3] 00 EE 00 (TX)" + NL
                + "10:15:30.000 18EEFF55 [8] 10 F7 45 01 00 45 00 01" + NL
                + "DPF Controller (85) reported as: {" + NL
                + "  Industry Group: Global" + NL
                + "  Vehicle System: Non-specific System, System Instance: 1" + NL
                + "  Function: Engine Emission Aftertreatment System, Functional Instance: 0, ECU Instance: 0" + NL
                + "  Manufactured by: Cummins Inc (formerly Cummins Engine Co), Identity Number: 390928" + NL
                + "  Is not arbitrary address capable." + NL
                + "}" + NL
                + "Error: No module reported Function 0" + NL;
        TestResultsListener listener = new TestResultsListener();
        instance.reportAddressClaim(listener);
        assertEquals(expected, listener.getResults());
        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(AddressClaimPacket.class, requestPacket);
    }

    @Test
    public void testReportAddressClaimNoResponse() {
        final int pgn = AddressClaimPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0xFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        when(j1939.requestMultiple(AddressClaimPacket.class, requestPacket))
                .thenReturn(Stream.empty()).thenReturn(Stream.empty()).thenReturn(Stream.empty());

        String expected = "";
        expected += "2007-12-03T10:15:30.000 Global Request for Address Claim" + NL;
        expected += "10:15:30.000 18EAFFA5 [3] 00 EE 00 (TX)" + NL;
        expected += "Error: Timeout - No Response." + NL;
        TestResultsListener listener = new TestResultsListener();
        instance.reportAddressClaim(listener);
        assertEquals(expected, listener.getResults());
        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(AddressClaimPacket.class, requestPacket);
    }

    @Test
    public void testReportCalibrationInformation() {
        final int pgn = DM19CalibrationInformationPacket.PGN;
        final byte[] calBytes1 = "ABCD1234567890123456".getBytes(UTF8);
        final byte[] calBytes2 = "EFGH1234567890123456".getBytes(UTF8);
        final byte[] calBytes3 = "IJKL1234567890123456".getBytes(UTF8);

        Packet requestPacket = Packet.create(0xEA00 | 0xFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        DM19CalibrationInformationPacket packet1 = new DM19CalibrationInformationPacket(
                Packet.create(pgn, 0x00, calBytes1));
        DM19CalibrationInformationPacket packet2 = new DM19CalibrationInformationPacket(
                Packet.create(pgn, 0x17, calBytes2));
        DM19CalibrationInformationPacket packet3 = new DM19CalibrationInformationPacket(
                Packet.create(pgn, 0x21, calBytes3));
        when(j1939.requestMultiple(DM19CalibrationInformationPacket.class, requestPacket))
                .thenReturn(Stream.of(packet1, packet2, packet3));

        String expected = "";
        expected += "2007-12-03T10:15:30.000 Global DM19 (Calibration Information) Request" + NL;
        expected += "10:15:30.000 18EAFFA5 [3] 00 D3 00 (TX)" + NL;
        expected += "10:15:30.000 18D30000 [20] 41 42 43 44 31 32 33 34 35 36 37 38 39 30 31 32 33 34 35 36"
                + NL;
        expected += "DM19 from Engine #1 (0): CAL ID of 1234567890123456 and CVN of 0x44434241" + NL;
        expected += "10:15:30.000 18D30017 [20] 45 46 47 48 31 32 33 34 35 36 37 38 39 30 31 32 33 34 35 36"
                + NL;
        expected += "DM19 from Instrument Cluster #1 (23): CAL ID of 1234567890123456 and CVN of 0x48474645" + NL;
        expected += "10:15:30.000 18D30021 [20] 49 4A 4B 4C 31 32 33 34 35 36 37 38 39 30 31 32 33 34 35 36"
                + NL;
        expected += "DM19 from Body Controller (33): CAL ID of 1234567890123456 and CVN of 0x4C4B4A49" + NL;
        TestResultsListener listener = new TestResultsListener();
        instance.reportCalibrationInformation(listener);
        assertEquals(expected, listener.getResults());
        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(DM19CalibrationInformationPacket.class, requestPacket);
    }

    @Test
    public void testReportCalibrationInformationWithNoResponses() {
        final int pgn = DM19CalibrationInformationPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0xFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        when(j1939.requestMultiple(DM19CalibrationInformationPacket.class, requestPacket)).thenReturn(Stream.empty());

        String expected = "";
        expected += "2007-12-03T10:15:30.000 Global DM19 (Calibration Information) Request" + NL;
        expected += "10:15:30.000 18EAFFA5 [3] 00 D3 00 (TX)" + NL;
        expected += "Error: Timeout - No Response." + NL;
        TestResultsListener listener = new TestResultsListener();
        instance.reportCalibrationInformation(listener);
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(DM19CalibrationInformationPacket.class, requestPacket);
    }

    @Test
    public void testReportComponentIdentification() {
        final int pgn = ComponentIdentificationPacket.PGN;
        final byte[] bytes1 = "Make1*Model1*SerialNumber1**".getBytes(UTF8);
        final byte[] bytes2 = "****".getBytes(UTF8);
        final byte[] bytes3 = "Make3*Model3***".getBytes(UTF8);

        Packet requestPacket = Packet.create(0xEA00 | 0xFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        ComponentIdentificationPacket packet1 = new ComponentIdentificationPacket(Packet.create(pgn, 0x00, bytes1));
        ComponentIdentificationPacket packet2 = new ComponentIdentificationPacket(Packet.create(pgn, 0x17, bytes2));
        ComponentIdentificationPacket packet3 = new ComponentIdentificationPacket(Packet.create(pgn, 0x21, bytes3));
        when(j1939.requestMultiple(ComponentIdentificationPacket.class, requestPacket))
                .thenReturn(Stream.of(packet1, packet2, packet3));

        String expected = "2007-12-03T10:15:30.000 Global Component Identification Request" + NL
                + "10:15:30.000 18EAFFA5 [3] EB FE 00 (TX)" + NL
                + "10:15:30.000 18FEEB00 [28] 4D 61 6B 65 31 2A 4D 6F 64 65 6C 31 2A 53 65 72 69 61 6C 4E 75 6D 62 65 72 31 2A 2A"
                + NL
                + "Component Identification from Engine #1 (0): {" + NL
                + "  Make: Make1" + NL
                + "  Model: Model1" + NL
                + "  Serial: SerialNumber1" + NL
                + "  Unit: " + NL
                + "}" + NL
                + "" + NL
                + "10:15:30.000 18FEEB17 [4] 2A 2A 2A 2A" + NL
                + "Component Identification from Instrument Cluster #1 (23): {" + NL
                + "  Make: " + NL
                + "  Model: " + NL
                + "  Serial: " + NL
                + "  Unit: " + NL
                + "}" + NL
                + "" + NL
                + "10:15:30.000 18FEEB21 [15] 4D 61 6B 65 33 2A 4D 6F 64 65 6C 33 2A 2A 2A" + NL
                + "Component Identification from Body Controller (33): {" + NL
                + "  Make: Make3" + NL
                + "  Model: Model3" + NL
                + "  Serial: " + NL
                + "  Unit: " + NL
                + "}" + NL
                + "" + NL;

        TestResultsListener listener = new TestResultsListener();
        instance.reportComponentIdentification(listener);
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(ComponentIdentificationPacket.class, requestPacket);
    }

    @Test
    public void testReportComponentIdentificationWithNoResponse() {
        final int pgn = ComponentIdentificationPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0xFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        when(j1939.requestMultiple(ComponentIdentificationPacket.class, requestPacket)).thenReturn(Stream.empty());

        String expected = "";
        expected += "2007-12-03T10:15:30.000 Global Component Identification Request" + NL;
        expected += "10:15:30.000 18EAFFA5 [3] EB FE 00 (TX)" + NL;
        expected += "Error: Timeout - No Response." + NL;

        TestResultsListener listener = new TestResultsListener();
        instance.reportComponentIdentification(listener);
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(ComponentIdentificationPacket.class, requestPacket);
    }

    @Test
    public void testReportConnectionSpeed() throws Exception {
        Bus bus = mock(Bus.class);
        when(j1939.getBus()).thenReturn(bus);
        when(bus.getConnectionSpeed()).thenReturn(250000);

        TestResultsListener listener = new TestResultsListener();
        instance.reportConnectionSpeed(listener);

        String expected = "2007-12-03T10:15:30.000 Baud Rate: 250,000 bps" + NL;
        assertEquals(expected, listener.getResults());
        verify(j1939).getBus();
    }

    @Test
    public void testReportConnectionSpeedWithException() throws Exception {
        Bus bus = mock(Bus.class);
        when(j1939.getBus()).thenReturn(bus);
        when(bus.getConnectionSpeed()).thenThrow(new BusException("Surprise"));

        TestResultsListener listener = new TestResultsListener();
        instance.reportConnectionSpeed(listener);

        String expected = "2007-12-03T10:15:30.000 Baud Rate: Could not be determined" + NL;
        assertEquals(expected, listener.getResults());
        verify(j1939).getBus();
    }

    @Test
    public void testReportEngineHours() {
        final int pgn = EngineHoursPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0xFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        EngineHoursPacket packet1 = new EngineHoursPacket(Packet.create(pgn, 0x00, 1, 2, 3, 4, 5, 6, 7, 8));
        EngineHoursPacket packet2 = new EngineHoursPacket(Packet.create(pgn, 0x01, 8, 7, 6, 5, 4, 3, 2, 1));
        when(j1939.requestMultiple(EngineHoursPacket.class, requestPacket)).thenReturn(Stream.of(packet1, packet2));

        String expected = "2007-12-03T10:15:30.000 Engine Hours Request" + NL
                + "10:15:30.000 18EAFFA5 [3] E5 FE 00 (TX)" + NL
                + "10:15:30.000 18FEE500 [8] 01 02 03 04 05 06 07 08" + NL
                + "Engine Hours, Revolutions from Engine #1 (0): " + NL
                + "  SPN   247, Engine Total Hours of Operation: 3365299.250 h" + NL
                + "  SPN   249, Engine Total Revolutions: 134678021000.000 r" + NL
                + "" + NL
                + "10:15:30.000 18FEE501 [8] 08 07 06 05 04 03 02 01" + NL
                + "Engine Hours, Revolutions from Engine #2 (1): " + NL
                + "  SPN   247, Engine Total Hours of Operation: 4214054.800 h" + NL
                + "  SPN   249, Engine Total Revolutions: 16909060000.000 r" + NL
                + "" + NL;

        TestResultsListener listener = new TestResultsListener();
        instance.reportEngineHours(listener);
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(EngineHoursPacket.class, requestPacket);
    }

    @Test
    public void testReportEngineHoursWithNoResponse() {
        final int pgn = EngineHoursPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0xFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        when(j1939.requestMultiple(EngineHoursPacket.class, requestPacket)).thenReturn(Stream.empty());

        String expected = "";
        expected += "2007-12-03T10:15:30.000 Engine Hours Request" + NL;
        expected += "10:15:30.000 18EAFFA5 [3] E5 FE 00 (TX)" + NL;
        expected += "Error: Timeout - No Response." + NL;

        TestResultsListener listener = new TestResultsListener();
        instance.reportEngineHours(listener);
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(EngineHoursPacket.class, requestPacket);
    }

    @Test
    public void testReportVehicleDistanceWithHiRes() {
        final int pgn = HighResVehicleDistancePacket.PGN;
        HighResVehicleDistancePacket packet0 = new HighResVehicleDistancePacket(
                Packet.create(pgn, 0x00, 0, 0, 0, 0, 0, 0, 0, 0));
        HighResVehicleDistancePacket packet1 = new HighResVehicleDistancePacket(
                Packet.create(pgn, 0x01, 1, 1, 1, 1, 1, 1, 1, 1));
        HighResVehicleDistancePacket packet2 = new HighResVehicleDistancePacket(
                Packet.create(pgn, 0x02, 2, 2, 2, 2, 2, 2, 2, 2));
        HighResVehicleDistancePacket packetFF = new HighResVehicleDistancePacket(
                Packet.create(pgn, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF));

        when(j1939.read(HighResVehicleDistancePacket.class, 3, TimeUnit.SECONDS))
                .thenReturn(Stream.of(packet0, packet1, packet2, packetFF).map(p -> Either.nullable(p, null)));

        String expected = "2007-12-03T10:15:30.000 Vehicle Distance" + NL
                + "10:15:30.000 18FEC102 [8] 02 02 02 02 02 02 02 02" + NL
                + "High Resolution Vehicle Distance from Turbocharger (2): " + NL
                + "  SPN   917, Total Vehicle Distance (High Resolution): 168430090.000 m" + NL
                + "  SPN   918, Trip Distance (High Resolution): 168430090.000 m" + NL
                + "" + NL;

        TestResultsListener listener = new TestResultsListener();
        instance.reportVehicleDistance(listener);
        assertEquals(expected, listener.getResults());

        verify(j1939).read(HighResVehicleDistancePacket.class, 3, TimeUnit.SECONDS);
    }

    @Test
    public void testReportVehicleDistanceWithLoRes() {
        when(j1939.read(HighResVehicleDistancePacket.class, 3, TimeUnit.SECONDS)).thenReturn(Stream.empty());
        final int pgn = TotalVehicleDistancePacket.PGN;
        TotalVehicleDistancePacket packet0 = new TotalVehicleDistancePacket(
                Packet.create(pgn, 0x00, 0, 0, 0, 0, 0, 0, 0, 0));
        TotalVehicleDistancePacket packet1 = new TotalVehicleDistancePacket(
                Packet.create(pgn, 0x01, 1, 1, 1, 1, 1, 1, 1, 1));
        TotalVehicleDistancePacket packet2 = new TotalVehicleDistancePacket(
                Packet.create(pgn, 0x02, 2, 2, 2, 2, 2, 2, 2, 2));
        TotalVehicleDistancePacket packetFF = new TotalVehicleDistancePacket(
                Packet.create(pgn, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF));

        when(j1939.read(TotalVehicleDistancePacket.class, 300, TimeUnit.MILLISECONDS))
                .thenReturn(Stream.of(packet2, packet1, packet0, packetFF).map(p -> Either.nullable(p, null)));

        String expected = "2007-12-03T10:15:30.000 Vehicle Distance" + NL
                + "10:15:30.000 18FEE002 [8] 02 02 02 02 02 02 02 02" + NL
                + "Total Vehicle Distance from Turbocharger (2): " + NL
                + "  SPN   244, Trip Distance: 4210752.250 km" + NL
                + "  SPN   245, Total Vehicle Distance: 4210752.250 km" + NL
                + "" + NL;

        TestResultsListener listener = new TestResultsListener();
        instance.reportVehicleDistance(listener);
        assertEquals(expected, listener.getResults());

        verify(j1939).read(HighResVehicleDistancePacket.class, 3, TimeUnit.SECONDS);
        verify(j1939).read(TotalVehicleDistancePacket.class, 300, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testReportVehicleDistanceWithNoResponse() {
        when(j1939.read(HighResVehicleDistancePacket.class, 3, TimeUnit.SECONDS)).thenReturn(Stream.empty());
        when(j1939.read(TotalVehicleDistancePacket.class, 300, TimeUnit.MILLISECONDS)).thenReturn(Stream.empty());

        String expected = "";
        expected += "2007-12-03T10:15:30.000 Vehicle Distance" + NL;
        expected += "Error: Timeout - No Response." + NL;

        TestResultsListener listener = new TestResultsListener();
        instance.reportVehicleDistance(listener);
        assertEquals(expected, listener.getResults());

        verify(j1939).read(HighResVehicleDistancePacket.class, 3, TimeUnit.SECONDS);
        verify(j1939).read(TotalVehicleDistancePacket.class, 300, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testReportVin() {
        final int pgn = VehicleIdentificationPacket.PGN;
        final byte[] vinBytes = "12345678901234567890*".getBytes(UTF8);

        Packet requestPacket = Packet.create(0xEA00 | 0xFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        VehicleIdentificationPacket packet1 = new VehicleIdentificationPacket(Packet.create(pgn, 0x00, vinBytes));
        VehicleIdentificationPacket packet2 = new VehicleIdentificationPacket(Packet.create(pgn, 0x17, vinBytes));
        VehicleIdentificationPacket packet3 = new VehicleIdentificationPacket(Packet.create(pgn, 0x21, vinBytes));
        when(j1939.requestMultiple(VehicleIdentificationPacket.class, requestPacket))
                .thenReturn(Stream.of(packet1, packet2, packet3));

        String expected = "";
        expected += "2007-12-03T10:15:30.000 Global VIN Request" + NL;
        expected += "10:15:30.000 18EAFFA5 [3] EC FE 00 (TX)" + NL;
        expected += "10:15:30.000 18FEEC00 [21] 31 32 33 34 35 36 37 38 39 30 31 32 33 34 35 36 37 38 39 30 2A"
                + NL;
        expected += "Vehicle Identification from Engine #1 (0): 12345678901234567890" + NL;
        expected += "10:15:30.000 18FEEC17 [21] 31 32 33 34 35 36 37 38 39 30 31 32 33 34 35 36 37 38 39 30 2A"
                + NL;
        expected += "Vehicle Identification from Instrument Cluster #1 (23): 12345678901234567890" + NL;
        expected += "10:15:30.000 18FEEC21 [21] 31 32 33 34 35 36 37 38 39 30 31 32 33 34 35 36 37 38 39 30 2A"
                + NL;
        expected += "Vehicle Identification from Body Controller (33): 12345678901234567890" + NL;

        TestResultsListener listener = new TestResultsListener();
        instance.reportVin(listener);
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(VehicleIdentificationPacket.class, requestPacket);
    }

    @Test
    public void testReportVinWithNoResponses() {
        final int pgn = VehicleIdentificationPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0xFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        when(j1939.requestMultiple(VehicleIdentificationPacket.class, requestPacket)).thenReturn(Stream.empty());

        String expected = "";
        expected += "2007-12-03T10:15:30.000 Global VIN Request" + NL;
        expected += "10:15:30.000 18EAFFA5 [3] EC FE 00 (TX)" + NL;
        expected += "Error: Timeout - No Response." + NL;

        TestResultsListener listener = new TestResultsListener();
        instance.reportVin(listener);
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(VehicleIdentificationPacket.class, requestPacket);
    }
}
