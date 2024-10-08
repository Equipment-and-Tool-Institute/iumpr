/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.modules;

import static net.soliddesign.iumpr.IUMPR.NL;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import org.etools.j1939tools.bus.Packet;
import org.etools.j1939tools.j1939.J1939;
import org.etools.j1939tools.j1939.packets.DM11ClearActiveDTCsPacket;
import org.etools.j1939tools.j1939.packets.DM12MILOnEmissionDTCPacket;
import org.etools.j1939tools.j1939.packets.DM23PreviouslyMILOnEmissionDTCPacket;
import org.etools.j1939tools.j1939.packets.DM28PermanentEmissionDTCPacket;
import org.etools.j1939tools.j1939.packets.DM6PendingEmissionDTCPacket;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.soliddesign.iumpr.controllers.TestResultsListener;

/**
 * Unit tests for the {@link DTCModule} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", justification = "The values returned are properly ignored on verify statements.")
@RunWith(MockitoJUnitRunner.class)
public class DTCModuleTest {

    /**
     * The Bus address of the tool for testing purposes
     */
    private static final int BUS_ADDR = 0xA5;

    private DTCModule instance;

    @Mock
    private J1939 j1939;

    @Before
    public void setUp() throws Exception {
        instance = new DTCModule(new TestDateTimeModule());
        instance.setJ1939(j1939);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(j1939);
    }

    @Test
    public void testReportDM11NoResponseWithManyModules() {
        final int pgn = DM11ClearActiveDTCsPacket.PGN;

        Packet requestPacket = Packet.create(0xEAFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        when(j1939.requestMultiple(DM11ClearActiveDTCsPacket.class, requestPacket))
                .thenReturn(Stream.of());

        String expected = "";
        expected += "2007-12-03T10:15:30.000 Clearing Diagnostic Trouble Codes" + NL;
        expected += "10:15:30.000 Global DM11 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 [3] D3 FE 00 (TX)" + NL;
        expected += "Diagnostic Trouble Codes were successfully cleared." + NL;

        TestResultsListener listener = new TestResultsListener();
        assertEquals(true, instance.reportDM11(listener, Arrays.asList(new Integer[] { 0, 0x17, 0x21 })));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(DM11ClearActiveDTCsPacket.class, requestPacket);
    }

    @Test
    public void testReportDM11WithManyModules() {
        final int pgn = DM11ClearActiveDTCsPacket.PGN;

        Packet requestPacket = Packet.create(0xEAFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        DM11ClearActiveDTCsPacket packet1 = new DM11ClearActiveDTCsPacket(
                Packet.create(0xE800, 0x00, 0x00, 0xFF, 0xFF, 0xFF, 0xF9, 0xD3, 0xFE, 0x00));
        DM11ClearActiveDTCsPacket packet2 = new DM11ClearActiveDTCsPacket(
                Packet.create(0xE800, 0x17, 0x00, 0xFF, 0xFF, 0xFF, 0xF9, 0xD3, 0xFE, 0x00));
        DM11ClearActiveDTCsPacket packet3 = new DM11ClearActiveDTCsPacket(
                Packet.create(0xE800, 0x21, 0x00, 0xFF, 0xFF, 0xFF, 0xF9, 0xD3, 0xFE, 0x00));
        when(j1939.requestMultiple(DM11ClearActiveDTCsPacket.class, requestPacket))
                .thenReturn(Stream.of(packet1, packet2, packet3));

        String expected = "";
        expected += "2007-12-03T10:15:30.000 Clearing Diagnostic Trouble Codes" + NL;
        expected += "10:15:30.000 Global DM11 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 [3] D3 FE 00 (TX)" + NL;
        expected += "10:15:30.000 18E80000 [8] 00 FF FF FF F9 D3 FE 00" + NL;
        expected += "DM11 from Engine #1 (0): Response is Acknowledged" + NL;
        expected += "10:15:30.000 18E80017 [8] 00 FF FF FF F9 D3 FE 00" + NL;
        expected += "DM11 from Instrument Cluster #1 (23): Response is Acknowledged" + NL;
        expected += "10:15:30.000 18E80021 [8] 00 FF FF FF F9 D3 FE 00" + NL;
        expected += "DM11 from Body Controller (33): Response is Acknowledged" + NL;
        expected += "Diagnostic Trouble Codes were successfully cleared." + NL;

        TestResultsListener listener = new TestResultsListener();
        assertEquals(true, instance.reportDM11(listener, Arrays.asList(new Integer[] { 0, 0x17, 0x21 })));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(DM11ClearActiveDTCsPacket.class, requestPacket);
    }

    @Test
    public void testReportDM11WithManyModulesWithNack() {
        final int pgn = DM11ClearActiveDTCsPacket.PGN;

        Packet requestPacket = Packet.create(0xEAFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        DM11ClearActiveDTCsPacket packet1 = new DM11ClearActiveDTCsPacket(
                Packet.create(0xE800, 0x00, 0x01, 0xFF, 0xFF, 0xFF, 0xF9, 0xD3, 0xFE, 0x00));
        DM11ClearActiveDTCsPacket packet2 = new DM11ClearActiveDTCsPacket(
                Packet.create(0xE800, 0x17, 0x00, 0xFF, 0xFF, 0xFF, 0xF9, 0xD3, 0xFE, 0x00));
        DM11ClearActiveDTCsPacket packet3 = new DM11ClearActiveDTCsPacket(
                Packet.create(0xE800, 0x21, 0x00, 0xFF, 0xFF, 0xFF, 0xF9, 0xD3, 0xFE, 0x00));
        when(j1939.requestMultiple(DM11ClearActiveDTCsPacket.class, requestPacket))
                .thenReturn(Stream.of(packet1, packet2, packet3));

        String expected = "";
        expected += "2007-12-03T10:15:30.000 Clearing Diagnostic Trouble Codes" + NL;
        expected += "10:15:30.000 Global DM11 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 [3] D3 FE 00 (TX)" + NL;
        expected += "10:15:30.000 18E80000 [8] 01 FF FF FF F9 D3 FE 00" + NL;
        expected += "DM11 from Engine #1 (0): Response is NACK" + NL;
        expected += "10:15:30.000 18E80017 [8] 00 FF FF FF F9 D3 FE 00" + NL;
        expected += "DM11 from Instrument Cluster #1 (23): Response is Acknowledged" + NL;
        expected += "10:15:30.000 18E80021 [8] 00 FF FF FF F9 D3 FE 00" + NL;
        expected += "DM11 from Body Controller (33): Response is Acknowledged" + NL;
        expected += "ERROR: Clearing Diagnostic Trouble Codes failed." + NL;

        TestResultsListener listener = new TestResultsListener();
        assertEquals(false, instance.reportDM11(listener, Arrays.asList(new Integer[] { 0, 0x17, 0x21 })));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(DM11ClearActiveDTCsPacket.class, requestPacket);
    }

    @Test
    public void testReportDM11WithNoResponsesOneModule() {
        final int pgn = DM11ClearActiveDTCsPacket.PGN;

        Packet requestPacket = Packet.create(0xEAFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);
        when(j1939.requestMultiple(DM11ClearActiveDTCsPacket.class, requestPacket))
                .thenReturn(Stream.empty());

        String expected = "";
        expected += "2007-12-03T10:15:30.000 Clearing Diagnostic Trouble Codes" + NL;
        expected += "10:15:30.000 Global DM11 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 [3] D3 FE 00 (TX)" + NL;
        expected += "Diagnostic Trouble Codes were successfully cleared." + NL;

        TestResultsListener listener = new TestResultsListener();
        assertEquals(true, instance.reportDM11(listener, Collections.singletonList(0)));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(DM11ClearActiveDTCsPacket.class, requestPacket);
    }

    @Test
    public void testReportDM11WithOneModule() {
        final int pgn = DM11ClearActiveDTCsPacket.PGN;

        Packet requestPacket1 = Packet.create(0xEAFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket1);

        DM11ClearActiveDTCsPacket packet1 = new DM11ClearActiveDTCsPacket(
                Packet.create(0xE800, 0x00, 0x00, 0xFF, 0xFF, 0xFF, 0xF9, 0xD3, 0xFE, 0x00));

        when(j1939.requestMultiple(DM11ClearActiveDTCsPacket.class, requestPacket1))
                .thenReturn(Stream.of(packet1));

        String expected = "";
        expected += "2007-12-03T10:15:30.000 Clearing Diagnostic Trouble Codes" + NL;
        expected += "10:15:30.000 Global DM11 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 [3] D3 FE 00 (TX)" + NL;
        expected += "10:15:30.000 18E80000 [8] 00 FF FF FF F9 D3 FE 00" + NL;
        expected += "DM11 from Engine #1 (0): Response is Acknowledged" + NL;
        expected += "Diagnostic Trouble Codes were successfully cleared." + NL;
        TestResultsListener listener = new TestResultsListener();
        assertEquals(true, instance.reportDM11(listener, Arrays.asList(new Integer[] { 0 })));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(DM11ClearActiveDTCsPacket.class, requestPacket1);
    }

    @Test
    public void testReportDM11WithOneModuleWithNack() {
        final int pgn = DM11ClearActiveDTCsPacket.PGN;

        Packet requestPacket1 = Packet.create(0xEAFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket1);

        DM11ClearActiveDTCsPacket packet1 = new DM11ClearActiveDTCsPacket(
                Packet.create(0xE800, 0x00, 0x01, 0xFF, 0xFF, 0xFF, 0xF9, 0xD3, 0xFE, 0x00));

        when(j1939.requestMultiple(DM11ClearActiveDTCsPacket.class, requestPacket1))
                .thenReturn(Stream.of(packet1));

        String expected = "";
        expected += "2007-12-03T10:15:30.000 Clearing Diagnostic Trouble Codes" + NL;
        expected += "10:15:30.000 Global DM11 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 [3] D3 FE 00 (TX)" + NL;
        expected += "10:15:30.000 18E80000 [8] 01 FF FF FF F9 D3 FE 00" + NL;
        expected += "DM11 from Engine #1 (0): Response is NACK" + NL;
        expected += "ERROR: Clearing Diagnostic Trouble Codes failed." + NL;
        TestResultsListener listener = new TestResultsListener();
        assertEquals(false, instance.reportDM11(listener, Arrays.asList(new Integer[] { 0 })));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(DM11ClearActiveDTCsPacket.class, requestPacket1);
    }

    @Test
    public void testReportDM12() {
        final int pgn = DM12MILOnEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0xFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        DM12MILOnEmissionDTCPacket packet1 = new DM12MILOnEmissionDTCPacket(
                Packet.create(pgn, 0x00, 0, 0, 0, 0, 0, 0, 0, 0));
        DM12MILOnEmissionDTCPacket packet2 = new DM12MILOnEmissionDTCPacket(
                Packet.create(pgn, 0x17, 0, 0, 0, 0, 0, 0, 0, 0));
        DM12MILOnEmissionDTCPacket packet3 = new DM12MILOnEmissionDTCPacket(
                Packet.create(pgn, 0x21, 0, 0, 0, 0, 0, 0, 0, 0));
        when(j1939.requestMultiple(DM12MILOnEmissionDTCPacket.class, requestPacket))
                .thenReturn(Stream.of(packet1, packet2, packet3));

        String expected = "2007-12-03T10:15:30.000 Global DM12 Request" + NL
                + "10:15:30.000 18EAFFA5 [3] D4 FE 00 (TX)" + NL
                + "10:15:30.000 18FED400 [8] 00 00 00 00 00 00 00 00" + NL
                + "DM12 from Engine #1 (0): MIL: alternate off, RSL: alternate off, AWL: alternate off, PL: alternate off, No DTCs"
                + NL
                + "10:15:30.000 18FED417 [8] 00 00 00 00 00 00 00 00" + NL
                + "DM12 from Instrument Cluster #1 (23): MIL: alternate off, RSL: alternate off, AWL: alternate off, PL: alternate off, No DTCs"
                + NL
                + "10:15:30.000 18FED421 [8] 00 00 00 00 00 00 00 00" + NL
                + "DM12 from Body Controller (33): MIL: alternate off, RSL: alternate off, AWL: alternate off, PL: alternate off, No DTCs"
                + NL;

        TestResultsListener listener = new TestResultsListener();
        assertEquals(false, instance.reportDM12(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(DM12MILOnEmissionDTCPacket.class, requestPacket);
    }

    @Test
    public void testReportDM12WithDTCs() {
        final int pgn = DM12MILOnEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0xFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        DM12MILOnEmissionDTCPacket packet1 = new DM12MILOnEmissionDTCPacket(Packet.create(pgn, 0x00, 0x00, 0xFF, 0x61,
                0x02, 0x13, 0x00, 0x21, 0x06, 0x1F, 0x00, 0xEE, 0x10, 0x04, 0x00));
        when(j1939.requestMultiple(DM12MILOnEmissionDTCPacket.class, requestPacket))
                .thenReturn(Stream.of(packet1));

        String expected = "2007-12-03T10:15:30.000 Global DM12 Request" + NL
                + "10:15:30.000 18EAFFA5 [3] D4 FE 00 (TX)" + NL
                + "10:15:30.000 18FED400 [14] 00 FF 61 02 13 00 21 06 1F 00 EE 10 04 00" + NL
                + "DM12 from Engine #1 (0): MIL: off, RSL: off, AWL: off, PL: off" + NL
                + "DTC 609:19 - Controller #2, Received Network Data In Error - 0 times" + NL
                + "DTC 1569:31 - Engine Protection Torque Derate, Condition Exists - 0 times" + NL
                + "DTC 4334:4 - AFT 1 DEF Doser 1 Absolute Pressure, Voltage Below Normal, Or Shorted To Low Source - 0 times"
                + NL;

        TestResultsListener listener = new TestResultsListener();
        assertEquals(true, instance.reportDM12(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(DM12MILOnEmissionDTCPacket.class, requestPacket);
    }

    @Test
    public void testReportDM12WithNoResponses() {
        final int pgn = DM12MILOnEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0xFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        when(j1939.requestMultiple(DM12MILOnEmissionDTCPacket.class, requestPacket)).thenReturn(Stream.empty());

        String expected = "";
        expected += "2007-12-03T10:15:30.000 Global DM12 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 [3] D4 FE 00 (TX)" + NL;
        expected += "Error: Timeout - No Response." + NL;

        TestResultsListener listener = new TestResultsListener();
        assertEquals(false, instance.reportDM12(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(DM12MILOnEmissionDTCPacket.class, requestPacket);
    }

    @Test
    public void testReportDM23() {
        final int pgn = DM23PreviouslyMILOnEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0xFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        DM23PreviouslyMILOnEmissionDTCPacket packet1 = new DM23PreviouslyMILOnEmissionDTCPacket(
                Packet.create(pgn, 0x00, 0, 0, 0, 0, 0, 0, 0, 0));
        DM23PreviouslyMILOnEmissionDTCPacket packet2 = new DM23PreviouslyMILOnEmissionDTCPacket(
                Packet.create(pgn, 0x17, 0, 0, 0, 0, 0, 0, 0, 0));
        DM23PreviouslyMILOnEmissionDTCPacket packet3 = new DM23PreviouslyMILOnEmissionDTCPacket(
                Packet.create(pgn, 0x21, 0, 0, 0, 0, 0, 0, 0, 0));
        when(j1939.requestMultiple(DM23PreviouslyMILOnEmissionDTCPacket.class, requestPacket))
                .thenReturn(Stream.of(packet1, packet2, packet3));

        String expected = "2007-12-03T10:15:30.000 Global DM23 Request" + NL
                + "10:15:30.000 18EAFFA5 [3] B5 FD 00 (TX)" + NL
                + "10:15:30.000 18FDB500 [8] 00 00 00 00 00 00 00 00" + NL
                + "DM23 from Engine #1 (0): MIL: alternate off, RSL: alternate off, AWL: alternate off, PL: alternate off, No DTCs"
                + NL
                + "10:15:30.000 18FDB517 [8] 00 00 00 00 00 00 00 00" + NL
                + "DM23 from Instrument Cluster #1 (23): MIL: alternate off, RSL: alternate off, AWL: alternate off, PL: alternate off, No DTCs"
                + NL
                + "10:15:30.000 18FDB521 [8] 00 00 00 00 00 00 00 00" + NL
                + "DM23 from Body Controller (33): MIL: alternate off, RSL: alternate off, AWL: alternate off, PL: alternate off, No DTCs"
                + NL;

        TestResultsListener listener = new TestResultsListener();
        assertEquals(false, instance.reportDM23(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(DM23PreviouslyMILOnEmissionDTCPacket.class, requestPacket);
    }

    @Test
    public void testReportDM23WithDTCs() {
        final int pgn = DM23PreviouslyMILOnEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0xFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        DM23PreviouslyMILOnEmissionDTCPacket packet1 = new DM23PreviouslyMILOnEmissionDTCPacket(Packet.create(pgn, 0x00,
                0x00, 0xFF, 0x61, 0x02, 0x13, 0x00, 0x21, 0x06, 0x1F, 0x00, 0xEE, 0x10, 0x04, 0x00));
        when(j1939.requestMultiple(DM23PreviouslyMILOnEmissionDTCPacket.class, requestPacket))
                .thenReturn(Stream.of(packet1));

        String expected = "";
        expected += "2007-12-03T10:15:30.000 Global DM23 Request" + NL
                + "10:15:30.000 18EAFFA5 [3] B5 FD 00 (TX)" + NL
                + "10:15:30.000 18FDB500 [14] 00 FF 61 02 13 00 21 06 1F 00 EE 10 04 00" + NL
                + "DM23 from Engine #1 (0): MIL: off, RSL: off, AWL: off, PL: off" + NL
                + "DTC 609:19 - Controller #2, Received Network Data In Error - 0 times" + NL
                + "DTC 1569:31 - Engine Protection Torque Derate, Condition Exists - 0 times" + NL
                + "DTC 4334:4 - AFT 1 DEF Doser 1 Absolute Pressure, Voltage Below Normal, Or Shorted To Low Source - 0 times"
                + NL;

        TestResultsListener listener = new TestResultsListener();
        assertEquals(true, instance.reportDM23(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(DM23PreviouslyMILOnEmissionDTCPacket.class, requestPacket);
    }

    @Test
    public void testReportDM23WithNoResponses() {
        final int pgn = DM23PreviouslyMILOnEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0xFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        String expected = "";
        expected += "2007-12-03T10:15:30.000 Global DM23 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 [3] B5 FD 00 (TX)" + NL;
        expected += "Error: Timeout - No Response." + NL;

        TestResultsListener listener = new TestResultsListener();
        assertEquals(false, instance.reportDM23(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(DM23PreviouslyMILOnEmissionDTCPacket.class, requestPacket);
    }

    @Test
    public void testReportDM28() {
        final int pgn = DM28PermanentEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0xFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        DM28PermanentEmissionDTCPacket packet1 = new DM28PermanentEmissionDTCPacket(
                Packet.create(pgn, 0x00, 0, 0, 0, 0, 0, 0, 0, 0));
        DM28PermanentEmissionDTCPacket packet2 = new DM28PermanentEmissionDTCPacket(
                Packet.create(pgn, 0x17, 0, 0, 0, 0, 0, 0, 0, 0));
        DM28PermanentEmissionDTCPacket packet3 = new DM28PermanentEmissionDTCPacket(
                Packet.create(pgn, 0x21, 0, 0, 0, 0, 0, 0, 0, 0));
        when(j1939.requestMultiple(DM28PermanentEmissionDTCPacket.class, requestPacket))
                .thenReturn(Stream.of(packet1, packet2, packet3));

        String expected = "2007-12-03T10:15:30.000 Global DM28 Request" + NL
                + "10:15:30.000 18EAFFA5 [3] 80 FD 00 (TX)" + NL
                + "10:15:30.000 18FD8000 [8] 00 00 00 00 00 00 00 00" + NL
                + "DM28 from Engine #1 (0): MIL: alternate off, RSL: alternate off, AWL: alternate off, PL: alternate off, No DTCs"
                + NL
                + "10:15:30.000 18FD8017 [8] 00 00 00 00 00 00 00 00" + NL
                + "DM28 from Instrument Cluster #1 (23): MIL: alternate off, RSL: alternate off, AWL: alternate off, PL: alternate off, No DTCs"
                + NL
                + "10:15:30.000 18FD8021 [8] 00 00 00 00 00 00 00 00" + NL
                + "DM28 from Body Controller (33): MIL: alternate off, RSL: alternate off, AWL: alternate off, PL: alternate off, No DTCs"
                + NL;

        TestResultsListener listener = new TestResultsListener();
        assertEquals(false, instance.reportDM28(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(DM28PermanentEmissionDTCPacket.class, requestPacket);
    }

    @Test
    public void testReportDM28WithDTCs() {
        final int pgn = DM28PermanentEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0xFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        DM28PermanentEmissionDTCPacket packet1 = new DM28PermanentEmissionDTCPacket(Packet.create(pgn, 0x00, 0x00, 0xFF,
                0x61, 0x02, 0x13, 0x00, 0x21, 0x06, 0x1F, 0x00, 0xEE, 0x10, 0x04, 0x00));

        when(j1939.requestMultiple(DM28PermanentEmissionDTCPacket.class, requestPacket))
                .thenReturn(Stream.of(packet1));

        String expected = "2007-12-03T10:15:30.000 Global DM28 Request" + NL
                + "10:15:30.000 18EAFFA5 [3] 80 FD 00 (TX)" + NL
                + "10:15:30.000 18FD8000 [14] 00 FF 61 02 13 00 21 06 1F 00 EE 10 04 00" + NL
                + "DM28 from Engine #1 (0): MIL: off, RSL: off, AWL: off, PL: off" + NL
                + "DTC 609:19 - Controller #2, Received Network Data In Error - 0 times" + NL
                + "DTC 1569:31 - Engine Protection Torque Derate, Condition Exists - 0 times" + NL
                + "DTC 4334:4 - AFT 1 DEF Doser 1 Absolute Pressure, Voltage Below Normal, Or Shorted To Low Source - 0 times"
                + NL;
        TestResultsListener listener = new TestResultsListener();
        assertEquals(true, instance.reportDM28(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(DM28PermanentEmissionDTCPacket.class, requestPacket);
    }

    @Test
    public void testReportDM28WithNoResponses() {
        final int pgn = DM28PermanentEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0xFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        String expected = "";
        expected += "2007-12-03T10:15:30.000 Global DM28 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 [3] 80 FD 00 (TX)" + NL;
        expected += "Error: Timeout - No Response." + NL;

        TestResultsListener listener = new TestResultsListener();
        assertEquals(false, instance.reportDM28(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(DM28PermanentEmissionDTCPacket.class, requestPacket);
    }

    @Test
    public void testReportDM6() {
        final int pgn = DM6PendingEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0xFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        DM6PendingEmissionDTCPacket packet1 = new DM6PendingEmissionDTCPacket(
                Packet.create(pgn, 0x00, 0, 0, 0, 0, 0, 0, 0, 0));
        DM6PendingEmissionDTCPacket packet2 = new DM6PendingEmissionDTCPacket(
                Packet.create(pgn, 0x17, 0, 0, 0, 0, 0, 0, 0, 0));
        DM6PendingEmissionDTCPacket packet3 = new DM6PendingEmissionDTCPacket(
                Packet.create(pgn, 0x21, 0, 0, 0, 0, 0, 0, 0, 0));
        when(j1939.requestMultiple(DM6PendingEmissionDTCPacket.class, requestPacket))
                .thenReturn(Stream.of(packet1, packet2, packet3));

        String expected = "2007-12-03T10:15:30.000 Global DM6 Request" + NL
                + "10:15:30.000 18EAFFA5 [3] CF FE 00 (TX)" + NL
                + "10:15:30.000 18FECF00 [8] 00 00 00 00 00 00 00 00" + NL
                + "DM6 from Engine #1 (0): MIL: alternate off, RSL: alternate off, AWL: alternate off, PL: alternate off, No DTCs"
                + NL
                + "10:15:30.000 18FECF17 [8] 00 00 00 00 00 00 00 00" + NL
                + "DM6 from Instrument Cluster #1 (23): MIL: alternate off, RSL: alternate off, AWL: alternate off, PL: alternate off, No DTCs"
                + NL
                + "10:15:30.000 18FECF21 [8] 00 00 00 00 00 00 00 00" + NL
                + "DM6 from Body Controller (33): MIL: alternate off, RSL: alternate off, AWL: alternate off, PL: alternate off, No DTCs"
                + NL;

        TestResultsListener listener = new TestResultsListener();
        assertEquals(false, instance.reportDM6(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(DM6PendingEmissionDTCPacket.class, requestPacket);
    }

    @Test
    public void testReportDM6WithDTCs() {
        final int pgn = DM6PendingEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0xFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        DM6PendingEmissionDTCPacket packet1 = new DM6PendingEmissionDTCPacket(
                Packet.create(pgn, 0x00, 0x00, 0xFF, 0x61, 0x02, 0x13, 0x00, 0x21, 0x06, 0x1F,
                        0x00, 0xEE, 0x10, 0x04, 0x00));
        when(j1939.requestMultiple(DM6PendingEmissionDTCPacket.class, requestPacket)).thenReturn(Stream.of(packet1));

        String expected = "2007-12-03T10:15:30.000 Global DM6 Request" + NL
                + "10:15:30.000 18EAFFA5 [3] CF FE 00 (TX)" + NL
                + "10:15:30.000 18FECF00 [14] 00 FF 61 02 13 00 21 06 1F 00 EE 10 04 00" + NL
                + "DM6 from Engine #1 (0): MIL: off, RSL: off, AWL: off, PL: off" + NL
                + "DTC 609:19 - Controller #2, Received Network Data In Error - 0 times" + NL
                + "DTC 1569:31 - Engine Protection Torque Derate, Condition Exists - 0 times" + NL
                + "DTC 4334:4 - AFT 1 DEF Doser 1 Absolute Pressure, Voltage Below Normal, Or Shorted To Low Source - 0 times"
                + NL;

        TestResultsListener listener = new TestResultsListener();
        assertEquals(true, instance.reportDM6(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(DM6PendingEmissionDTCPacket.class, requestPacket);
    }

    @Test
    public void testReportDM6WithNoResponses() {
        final int pgn = DM6PendingEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0xFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        String expected = "";
        expected += "2007-12-03T10:15:30.000 Global DM6 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 [3] CF FE 00 (TX)" + NL;
        expected += "Error: Timeout - No Response." + NL;

        TestResultsListener listener = new TestResultsListener();
        assertEquals(false, instance.reportDM6(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(DM6PendingEmissionDTCPacket.class, requestPacket);
    }
}
