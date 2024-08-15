/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.modules;

import static net.soliddesign.iumpr.IUMPR.NL;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.etools.j1939tools.bus.Packet;
import org.etools.j1939tools.j1939.J1939;
import org.etools.j1939tools.j1939.packets.DM24SPNSupportPacket;
import org.etools.j1939tools.j1939.packets.DM30ScaledTestResultsPacket;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.soliddesign.iumpr.controllers.TestResultsListener;

/**
 * Unit tests for {@link OBDTestsModule}
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", justification = "The values returned are properly ignored on verify statements.")
@RunWith(MockitoJUnitRunner.class)
public class OBDTestsModuleTest {

    private static final int BUS_ADDR = 0xA5;

    private OBDTestsModule instance;

    @Mock
    private J1939 j1939;

    private TestResultsListener listener;

    @Before
    public void setUp() throws Exception {
        // when(j1939.getBusAddress()).thenReturn(0xA5);
        instance = new OBDTestsModule(new TestDateTimeModule());
        instance.setJ1939(j1939);
        listener = new TestResultsListener();
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(j1939);
    }

    @Test
    public void testReportOBDTestsMultipleModulesMultipleScaledTestResultsRequestsOnlyScaledTests() {
        when(j1939.getBusAddress()).thenReturn(BUS_ADDR);
        Packet dm24RequestPacket1 = Packet.create(0xEA00, BUS_ADDR, true, 0xB6, 0xFD, 0x00);
        when(j1939.createRequestPacket(64950, 0x00)).thenReturn(dm24RequestPacket1);

        DM24SPNSupportPacket engineDm24Packet = new DM24SPNSupportPacket(
                Packet.create(64950, 0x00,
                        0x66, 0x00, 0x1B, 0x01,
                        0x5C, 0x00, 0x1F, 0x01,
                        0x00, 0x02, 0x1B, 0x01,
                        0x9C, 0xF0, 0xFB, 0x00));

        when(j1939.requestPacket(dm24RequestPacket1, DM24SPNSupportPacket.class, 0x00, 3, 15000))
                .thenReturn(Optional.of(engineDm24Packet));

        DM30ScaledTestResultsPacket engineDm30PacketSpn102 = new DM30ScaledTestResultsPacket(
                Packet.create(0xA400, 0x00, 0xF7, 0x66, 0x00, 0x12, 0xD0, 0x00, 0x00, 0xFB, 0xFF, 0xFF, 0xFF, 0xFF));
        when(j1939.requestPacket(Packet.create(0xE300, BUS_ADDR, true, 0xF7, 0x66, 0x00, 0x1F, 0xFF, 0xFF, 0xFF, 0xFF),
                DM30ScaledTestResultsPacket.class, 0x0, 3)).thenReturn(Optional.of(engineDm30PacketSpn102));

        DM30ScaledTestResultsPacket engineDm30PacketSpn512 = new DM30ScaledTestResultsPacket(
                Packet.create(0xA400, 0x00, 0xF7, 0x00, 0x02, 0x12, 0xD0, 0x00, 0x00, 0xFB, 0xFF, 0xFF, 0xFF, 0xFF));
        when(j1939.requestPacket(Packet.create(0xE300, BUS_ADDR, true, 0xF7, 0x00, 0x02, 0x1F, 0xFF, 0xFF, 0xFF, 0xFF),
                DM30ScaledTestResultsPacket.class, 0x00, 3)).thenReturn(Optional.of(engineDm30PacketSpn512));

        DM30ScaledTestResultsPacket engineDm30PacketSpn520348 = new DM30ScaledTestResultsPacket(
                Packet.create(0xA400, 0x00, 0xF7, 0x9C, 0xF0, 0xFF, 0xD0, 0x00, 0x00, 0xFB, 0xFF, 0xFF, 0xFF, 0xFF));
        when(j1939.requestPacket(Packet.create(0xE300, BUS_ADDR, true, 0xF7, 0x9C, 0xF0, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF),
                DM30ScaledTestResultsPacket.class, 0x00, 3)).thenReturn(Optional.of(engineDm30PacketSpn520348));

        Packet dm24RequestPacket2 = Packet.create(0xEA55, BUS_ADDR, true, 0xB6, 0xFD, 0x00);
        when(j1939.createRequestPacket(64950, 0x55)).thenReturn(dm24RequestPacket2);
        DM24SPNSupportPacket atDm24Packet = new DM24SPNSupportPacket(
                Packet.create(64950, 0x55,
                        0xA7, 0x13, 0x1C, 0x00,
                        0x0C, 0x11, 0x18, 0x00,
                        0x9A, 0x0C, 0x18, 0x00));
        when(j1939.requestPacket(dm24RequestPacket2, DM24SPNSupportPacket.class, 0x55, 3, 15000))
                .thenReturn(Optional.of(atDm24Packet));

        DM30ScaledTestResultsPacket atDm30PacketSpn4364 = new DM30ScaledTestResultsPacket(
                Packet.create(0xA400, 0x55, 0xF7, 0x0C, 0x11, 0x00, 0xFB, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF));
        when(j1939.requestPacket(Packet.create(0xE355, BUS_ADDR, true, 0xF7, 0x0C, 0x11, 0x1F, 0xFF, 0xFF, 0xFF, 0xFF),
                DM30ScaledTestResultsPacket.class, 0x55, 3)).thenReturn(Optional.of(atDm30PacketSpn4364));

        DM30ScaledTestResultsPacket atDm30PacketSpn3226 = new DM30ScaledTestResultsPacket(
                Packet.create(0xA400, 0x55, 0xF7, 0x9A, 0x0C, 0x0A, 0x00, 0x01, 0x00, 0xFB, 0xFF, 0xFF, 0xFF, 0xFF));
        when(j1939.requestPacket(Packet.create(0xE355, BUS_ADDR, true, 0xF7, 0x9A, 0x0C, 0x1F, 0xFF, 0xFF, 0xFF, 0xFF),
                DM30ScaledTestResultsPacket.class, 0x55, 3)).thenReturn(Optional.of(atDm30PacketSpn3226));

        List<Integer> obdModules = Arrays.asList(new Integer[] { 0x00, 0x55 });
        instance.reportOBDTests(listener, obdModules);

        String expected = "2007-12-03T10:15:30.000 Direct DM24 Request to Engine #1 (0)" + NL
                + "10:15:30.000 18EA00A5 [3] B6 FD 00 (TX)" + NL
                + "10:15:30.000 18FDB600 [16] 66 00 1B 01 5C 00 1F 01 00 02 1B 01 9C F0 FB 00" + NL
                + "DM24 from Engine #1 (0): [" + NL
                + "  D F T D F" + NL
                + "  a r e M F" + NL
                + "  t F s 5 l" + NL
                + "  a r t 8 n  SPN — SP Name" + NL
                + "  ------------------------" + NL
                + "          1  SPN 92 - Engine Percent Load At Current Speed" + NL
                + "      T   1  SPN 102 - Engine Intake Manifold #1 Pressure" + NL
                + "      T   1  SPN 512 - Driver's Demand Engine - Percent Torque" + NL
                + "      T   0  SPN 520348 - Manufacturer Assignable SPN" + NL
                + "]" + NL
                + "Freeze Frame data length = 0 bytes" + NL
                + "" + NL
                + "" + NL
                + "2007-12-03T10:15:30.000 Direct DM24 Request to DPF Controller (85)" + NL
                + "10:15:30.000 18EA55A5 [3] B6 FD 00 (TX)" + NL
                + "10:15:30.000 18FDB655 [12] A7 13 1C 00 0C 11 18 00 9A 0C 18 00" + NL
                + "DM24 from DPF Controller (85): [" + NL
                + "  D F T D F" + NL
                + "  a r e M F" + NL
                + "  t F s 5 l" + NL
                + "  a r t 8 n  SPN — SP Name" + NL
                + "  ------------------------" + NL
                + "  D F T   0  SPN 3226 - AFT 1 Outlet NOx 1" + NL
                + "  D F T   0  SPN 4364 - AFT 1 SCR Conversion Efficiency" + NL
                + "  D F     0  SPN 5031 - AFT 1 Outlet NOx Sensor Heater Ratio" + NL
                + "]" + NL
                + "Freeze Frame data length = 0 bytes" + NL
                + "" + NL
                + "" + NL
                + "2007-12-03T10:15:30.000 Direct DM30 Requests to Engine #1 (0)" + NL
                + "10:15:30.000 18E300A5 [8] F7 66 00 1F FF FF FF FF (TX)" + NL
                + "10:15:30.000 18A40000 [12] F7 66 00 12 D0 00 00 FB FF FF FF FF" + NL
                + "DM30 from Engine #1 (0): SPN 102 FMI 18 (SLOT 208) Result: Test Not Complete." + NL
                + "" + NL
                + "10:15:30.000 18E300A5 [8] F7 00 02 1F FF FF FF FF (TX)" + NL
                + "10:15:30.000 18A40000 [12] F7 00 02 12 D0 00 00 FB FF FF FF FF" + NL
                + "DM30 from Engine #1 (0): SPN 512 FMI 18 (SLOT 208) Result: Test Not Complete." + NL
                + "" + NL
                + "10:15:30.000 18E300A5 [8] F7 9C F0 FF FF FF FF FF (TX)" + NL
                + "10:15:30.000 18A40000 [12] F7 9C F0 FF D0 00 00 FB FF FF FF FF" + NL
                + "DM30 from Engine #1 (0): SPN 520348 FMI 31 (SLOT 208) Result: Test Not Complete." + NL
                + "" + NL
                + "2007-12-03T10:15:30.000 Direct DM30 Requests to DPF Controller (85)" + NL
                + "10:15:30.000 18E355A5 [8] F7 9A 0C 1F FF FF FF FF (TX)" + NL
                + "10:15:30.000 18A40055 [12] F7 9A 0C 0A 00 01 00 FB FF FF FF FF" + NL
                + "DM30 from DPF Controller (85): SPN 3226 FMI 10 (SLOT 256) Result: Test Not Complete." + NL
                + "" + NL
                + "10:15:30.000 18E355A5 [8] F7 0C 11 1F FF FF FF FF (TX)" + NL
                + "10:15:30.000 18A40055 [12] F7 0C 11 00 FB FF FF FF FF FF FF FF" + NL
                + "DM30 from DPF Controller (85): SPN 4364 FMI 0 (SLOT 65531) Result: Test Passed. Min: N/A, Value: 65,535, Max: N/A"
                + NL
                + "" + NL
                + "Incomplete Tests: [" + NL
                + "  DPF Controller (85): SPN 3226 FMI 10 (SLOT 256) Result: Test Not Complete." + NL
                + "  Engine #1 (0): SPN 102 FMI 18 (SLOT 208) Result: Test Not Complete." + NL
                + "  Engine #1 (0): SPN 512 FMI 18 (SLOT 208) Result: Test Not Complete." + NL
                + "  Engine #1 (0): SPN 520348 FMI 31 (SLOT 208) Result: Test Not Complete." + NL
                + "]" + NL
                + "4 Incomplete Tests" + NL;
        assertEquals(expected, listener.getResults());

        verify(j1939, times(5)).getBusAddress();
        verify(j1939).createRequestPacket(64950, 0x00);
        verify(j1939).createRequestPacket(64950, 0x55);
        verify(j1939).requestPacket(dm24RequestPacket1, DM24SPNSupportPacket.class, 0x00, 3, 15000);
        verify(j1939).requestPacket(dm24RequestPacket2, DM24SPNSupportPacket.class, 0x55, 3, 15000);
        verify(j1939, times(5)).requestPacket(any(Packet.class), eq(DM30ScaledTestResultsPacket.class), any(int.class),
                eq(3));
    }

    @Test
    public void testReportOBDTestsNoResponse() {
        final Packet requestPacket = Packet.create(0xEA00, BUS_ADDR, true, 0xB6, 0xFD, 0x00);
        when(j1939.createRequestPacket(64950, 0x00)).thenReturn(requestPacket);
        List<Integer> obdModules = Arrays.asList(new Integer[] { 0x00 });

        instance.reportOBDTests(listener, obdModules);
        String expected = "";
        expected += "2007-12-03T10:15:30.000 Direct DM24 Request to Engine #1 (0)" + NL;
        expected += "10:15:30.000 18EA00A5 [3] B6 FD 00 (TX)" + NL;
        expected += "Error: Timeout - No Response." + NL;
        expected += "" + NL;
        expected += "ERROR No tests results returned" + NL;

        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(64950, 0x00);
        verify(j1939).requestPacket(requestPacket, DM24SPNSupportPacket.class, 0, 3, 15000);
    }

    @Test
    public void testReportOBDTestsOneScaledTestResults() {
        when(j1939.getBusAddress()).thenReturn(BUS_ADDR);
        final Packet dm24RequestPacket = Packet.create(0xEA00, BUS_ADDR, true, 0xB6, 0xFD, 0x00);
        when(j1939.createRequestPacket(64950, 0x00)).thenReturn(dm24RequestPacket);

        DM30ScaledTestResultsPacket engineDm30Packet = new DM30ScaledTestResultsPacket(
                Packet.create(0xA400, 0x00, 0xF7, 0x66, 0x00, 0x12, 0xD0, 0x00, 0x00, 0xFA, 0xFF, 0xFF, 0xFF, 0xFF));

        when(j1939.requestPacket(any(Packet.class), eq(DM30ScaledTestResultsPacket.class), eq(0x00), eq(3)))
                .thenReturn(Optional.of(engineDm30Packet));

        DM24SPNSupportPacket engineDm24Packet = new DM24SPNSupportPacket(
                Packet.create(64950, 0x00, 0x66, 0x00, 0x1B, 0x01));
        when(j1939.requestPacket(dm24RequestPacket, DM24SPNSupportPacket.class, 0, 3, 15000))
                .thenReturn(Optional.of(engineDm24Packet));
        List<Integer> obdModules = Arrays.asList(new Integer[] { 0x00 });
        instance.reportOBDTests(listener, obdModules);

        String expected = "2007-12-03T10:15:30.000 Direct DM24 Request to Engine #1 (0)" + NL
                + "10:15:30.000 18EA00A5 [3] B6 FD 00 (TX)" + NL
                + "10:15:30.000 18FDB600 [4] 66 00 1B 01" + NL
                + "DM24 from Engine #1 (0): [" + NL
                + "  D F T D F" + NL
                + "  a r e M F" + NL
                + "  t F s 5 l" + NL
                + "  a r t 8 n  SPN — SP Name" + NL
                + "  ------------------------" + NL
                + "      T   1  SPN 102 - Engine Intake Manifold #1 Pressure" + NL
                + "]" + NL
                + "Freeze Frame data length = 0 bytes" + NL
                + "" + NL
                + "" + NL
                + "2007-12-03T10:15:30.000 Direct DM30 Requests to Engine #1 (0)" + NL
                + "10:15:30.000 18E300A5 [8] F7 66 00 1F FF FF FF FF (TX)" + NL
                + "10:15:30.000 18A40000 [12] F7 66 00 12 D0 00 00 FA FF FF FF FF" + NL
                + "DM30 from Engine #1 (0): SPN 102 FMI 18 (SLOT 208) Result: Test Passed. Min: N/A, Value: 64,000, Max: N/A count"
                + NL
                + "" + NL
                + "All Tests Complete" + NL;

        assertEquals(expected, listener.getResults());

        verify(j1939).getBusAddress();
        verify(j1939).createRequestPacket(64950, 0x00);
        verify(j1939).requestPacket(dm24RequestPacket, DM24SPNSupportPacket.class, 0, 3, 15000);
        verify(j1939).requestPacket(any(Packet.class), eq(DM30ScaledTestResultsPacket.class), eq(0x00), eq(3));
    }

    @Test
    public void testReportOBDTestsScaledTestResultsTimeout() {
        when(j1939.getBusAddress()).thenReturn(BUS_ADDR);
        final Packet requestPacket = Packet.create(0xEA00, BUS_ADDR, true, 0xB6, 0xFD, 0x00);
        when(j1939.createRequestPacket(64950, 0x00)).thenReturn(requestPacket);
        DM24SPNSupportPacket engineDm24Packet = new DM24SPNSupportPacket(
                Packet.create(64950, 0x00, 0x66, 0x00, 0x1B, 0x01));
        when(j1939.requestPacket(requestPacket, DM24SPNSupportPacket.class, 0, 3, 15000))
                .thenReturn(Optional.of(engineDm24Packet));

        when(j1939.requestPacket(any(Packet.class), eq(DM30ScaledTestResultsPacket.class), eq(0x00), eq(3)))
                .thenReturn(Optional.empty());

        List<Integer> obdModules = Arrays.asList(new Integer[] { 0x00 });
        instance.reportOBDTests(listener, obdModules);

        String expected = "2007-12-03T10:15:30.000 Direct DM24 Request to Engine #1 (0)" + NL
                + "10:15:30.000 18EA00A5 [3] B6 FD 00 (TX)" + NL
                + "10:15:30.000 18FDB600 [4] 66 00 1B 01" + NL
                + "DM24 from Engine #1 (0): [" + NL
                + "  D F T D F" + NL
                + "  a r e M F" + NL
                + "  t F s 5 l" + NL
                + "  a r t 8 n  SPN — SP Name" + NL
                + "  ------------------------" + NL
                + "      T   1  SPN 102 - Engine Intake Manifold #1 Pressure" + NL
                + "]" + NL
                + "Freeze Frame data length = 0 bytes" + NL
                + "" + NL
                + "" + NL
                + "2007-12-03T10:15:30.000 Direct DM30 Requests to Engine #1 (0)" + NL
                + "10:15:30.000 18E300A5 [8] F7 66 00 1F FF FF FF FF (TX)" + NL
                + "Error: Timeout - No Response." + NL
                + "" + NL
                + "No Scaled Tests Results from Engine #1 (0)" + NL
                + "" + NL
                + "ERROR No tests results returned" + NL;

        assertEquals(expected, listener.getResults());

        verify(j1939).getBusAddress();
        verify(j1939).createRequestPacket(64950, 0x00);
        verify(j1939).requestPacket(requestPacket, DM24SPNSupportPacket.class, 0, 3, 15000);
        verify(j1939).requestPacket(any(Packet.class), eq(DM30ScaledTestResultsPacket.class), eq(0x00), eq(3));
    }

    @Test
    public void testReportOBDTestsWithNoScaledTestResults() {
        Packet requestPacket = Packet.create(0xEA00, BUS_ADDR, true, 0xB6, 0xFD, 0x00);
        when(j1939.createRequestPacket(64950, 0x00)).thenReturn(requestPacket);
        DM24SPNSupportPacket engineDm24Packet = new DM24SPNSupportPacket(
                Packet.create(64950, 0x00, 0x66, 0x00, 0x1C, 0x01));
        when(j1939.requestPacket(requestPacket, DM24SPNSupportPacket.class, 0, 3, 15000))
                .thenReturn(Optional.of(engineDm24Packet));

        List<Integer> obdModules = Arrays.asList(new Integer[] { 0x00 });
        instance.reportOBDTests(listener, obdModules);

        String expected = "2007-12-03T10:15:30.000 Direct DM24 Request to Engine #1 (0)" + NL
                + "10:15:30.000 18EA00A5 [3] B6 FD 00 (TX)" + NL
                + "10:15:30.000 18FDB600 [4] 66 00 1C 01" + NL
                + "DM24 from Engine #1 (0): [" + NL
                + "  D F T D F" + NL
                + "  a r e M F" + NL
                + "  t F s 5 l" + NL
                + "  a r t 8 n  SPN — SP Name" + NL
                + "  ------------------------" + NL
                + "  D F     1  SPN 102 - Engine Intake Manifold #1 Pressure" + NL
                + "]" + NL
                + "Freeze Frame data length = 1 bytes" + NL
                + "" + NL
                + "" + NL
                + "Engine #1 (0) does not have any tests that support scaled tests results" + NL
                + "" + NL
                + "ERROR No tests results returned" + NL;

        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(64950, 0x00);
        verify(j1939).requestPacket(requestPacket, DM24SPNSupportPacket.class, 0, 3, 15000);
    }

}
