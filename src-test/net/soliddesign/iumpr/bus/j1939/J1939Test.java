/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.bus.j1939;

import static net.soliddesign.iumpr.bus.j1939.J1939.GLOBAL_ADDR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import net.soliddesign.iumpr.bus.Bus;
import net.soliddesign.iumpr.bus.BusException;
import net.soliddesign.iumpr.bus.EchoBus;
import net.soliddesign.iumpr.bus.Packet;
import net.soliddesign.iumpr.bus.j1939.packets.ComponentIdentificationPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM11ClearActiveDTCsPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM12MILOnEmissionDTCPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM19CalibrationInformationPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM20MonitorPerformanceRatioPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM21DiagnosticReadinessPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM23PreviouslyMILOnEmissionDTCPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM24SPNSupportPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM26TripDiagnosticReadinessPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM28PermanentEmissionDTCPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM30ScaledTestResultsPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM5DiagnosticReadinessPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM6PendingEmissionDTCPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM7CommandTestsPacket;
import net.soliddesign.iumpr.bus.j1939.packets.EngineHoursPacket;
import net.soliddesign.iumpr.bus.j1939.packets.EngineSpeedPacket;
import net.soliddesign.iumpr.bus.j1939.packets.ParsedPacket;
import net.soliddesign.iumpr.bus.j1939.packets.TotalVehicleDistancePacket;
import net.soliddesign.iumpr.bus.j1939.packets.VehicleIdentificationPacket;

/**
 * Unit test for the {@link J1939} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class J1939Test {

    private static class TestPacket extends ParsedPacket {

        public TestPacket(Packet packet) {
            super(packet);
        }
    }

    /**
     * The address of the tool on the bus - for testing. This is NOT the right
     * service tool address to confirm it's not improperly hard-coded (because
     * it was)
     *
     */
    private static final int BUS_ADDR = 0xA5;

    private static final Charset UTF8 = StandardCharsets.UTF_8;

    @Mock
    private Bus bus;

    private J1939 instance;

    private ArgumentCaptor<Packet> sendPacketCaptor;

    @Before
    public void setup() {
        when(bus.getAddress()).thenReturn(BUS_ADDR);

        sendPacketCaptor = ArgumentCaptor.forClass(Packet.class);
        instance = new J1939(bus);
    }

    /**
     * The purpose of this test is to verify that processing doesn't hang on any
     * possible PGN
     *
     * @throws Exception
     */
    @Test
    public void testAllPgns() throws Exception {
        EchoBus echoBus = new EchoBus(BUS_ADDR);
        J1939 j1939 = new J1939(echoBus);
        for (int id = 0; id < 0x1FFFFF; id++) {
            Packet packet = Packet.create(id, 0x17, 11, 22, 33, 44, 55, 66, 77, 88);
            Stream<ParsedPacket> stream = j1939.read();
            echoBus.send(packet);
            assertTrue("Failed on id " + id, stream.findFirst().isPresent());
        }
    }

    @Test
    public void testCreateRequestPacket() {
        Packet actual = instance.createRequestPacket(12345, 0x99);
        assertEquals(0xEA99, actual.getId());
        assertEquals(BUS_ADDR, actual.getSource());
        assertEquals(12345, actual.get24(0));
    }

    @Test
    public void testRead() throws Exception {
        when(bus.read(365, TimeUnit.DAYS)).thenReturn(Stream.empty());
        instance.read();
        verify(bus).read(365, TimeUnit.DAYS);
    }

    @Test
    public void testReadEngineSpeed() throws Exception {
        Packet packet1 = Packet.create(EngineSpeedPacket.PGN, 0x00, 1, 2, 3, 4, 5, 6, 7, 8);
        when(bus.read(2500, TimeUnit.MILLISECONDS)).thenReturn(Stream.of(packet1));

        Optional<EngineSpeedPacket> response = instance.read(EngineSpeedPacket.class, 0x00);
        assertTrue(response.isPresent());
        EngineSpeedPacket result = response.get();
        assertEquals(160.5, result.getEngineSpeed(), 0.0);
    }

    @Test
    public void testReadHandlesBusException() throws Exception {
        when(bus.read(2500, TimeUnit.MILLISECONDS)).thenThrow(new BusException("Testing"));

        Optional<EngineSpeedPacket> response = instance.read(EngineSpeedPacket.class, 0x00);
        assertFalse(response.isPresent());
    }

    @Test
    public void testReadHandlesException() throws Exception {
        Optional<TestPacket> response = instance.read(TestPacket.class, 0x00);
        assertFalse(response.isPresent());
    }

    @Test
    public void testReadHandlesTimeout() throws Exception {
        when(bus.read(2500, TimeUnit.MILLISECONDS)).thenReturn(Stream.empty());

        Optional<EngineSpeedPacket> response = instance.read(EngineSpeedPacket.class, 0x00);
        assertFalse(response.isPresent());
    }

    @Test
    public void testReadIgnoresOtherPGNs() throws Exception {
        Packet packet1 = Packet.create(EngineSpeedPacket.PGN - 1, 0x00, 1, 1, 1, 1, 1, 1, 1, 1);
        Packet packet2 = Packet.create(EngineSpeedPacket.PGN, 0x00, 1, 2, 3, 4, 5, 6, 7, 8);
        Packet packet3 = Packet.create(EngineSpeedPacket.PGN + 1, 0x00, 2, 2, 2, 2, 2, 2, 2, 2);
        when(bus.read(2500, TimeUnit.MILLISECONDS)).thenReturn(Stream.of(packet1, packet2, packet3));

        Optional<EngineSpeedPacket> response = instance.read(EngineSpeedPacket.class, 0x00);
        assertTrue(response.isPresent());
        EngineSpeedPacket result = response.get();
        assertEquals(160.5, result.getEngineSpeed(), 0.0);
    }

    @Test
    public void testReadIgnoresOtherSources() throws Exception {
        Packet packet1 = Packet.create(EngineSpeedPacket.PGN, 0x01, 1, 1, 1, 1, 1, 1, 1, 1);
        Packet packet2 = Packet.create(EngineSpeedPacket.PGN, 0x00, 1, 2, 3, 4, 5, 6, 7, 8);
        Packet packet3 = Packet.create(EngineSpeedPacket.PGN, 0x02, 2, 2, 2, 2, 2, 2, 2, 2);
        when(bus.read(2500, TimeUnit.MILLISECONDS)).thenReturn(Stream.of(packet1, packet2, packet3));

        Optional<EngineSpeedPacket> response = instance.read(EngineSpeedPacket.class, 0x00);
        assertTrue(response.isPresent());
        EngineSpeedPacket result = response.get();
        assertEquals(160.5, result.getEngineSpeed(), 0.0);
    }

    @Test
    public void testReadTotalVehicleDistance() throws Exception {
        Packet packet1 = Packet.create(TotalVehicleDistancePacket.PGN, 0x00, 1, 2, 3, 4, 5, 6, 7, 8);
        when(bus.read(2500, TimeUnit.MILLISECONDS)).thenReturn(Stream.of(packet1));

        Optional<TotalVehicleDistancePacket> response = instance.read(TotalVehicleDistancePacket.class, 0x00);
        assertTrue(response.isPresent());
        TotalVehicleDistancePacket result = response.get();
        assertEquals(16834752.625, result.getTotalVehicleDistance(), 0.0);
    }

    @Test
    public void testRequestAllPackets() throws Exception {
        List<Class<? extends ParsedPacket>> testCases = new ArrayList<>();
        testCases.add(DM5DiagnosticReadinessPacket.class);
        testCases.add(DM6PendingEmissionDTCPacket.class);
        testCases.add(DM12MILOnEmissionDTCPacket.class);
        testCases.add(DM23PreviouslyMILOnEmissionDTCPacket.class);
        testCases.add(DM26TripDiagnosticReadinessPacket.class);
        testCases.add(DM28PermanentEmissionDTCPacket.class);
        testCases.add(DM19CalibrationInformationPacket.class);
        testCases.add(ComponentIdentificationPacket.class);
        testCases.add(DM21DiagnosticReadinessPacket.class);
        testCases.add(DM24SPNSupportPacket.class);
        testCases.add(DM20MonitorPerformanceRatioPacket.class);
        testCases.add(EngineHoursPacket.class);

        for (Class<? extends ParsedPacket> clazz : testCases) {
            int id = clazz.getField("PGN").getInt(null);
            String message = "Class " + clazz.getSimpleName() + " failed";

            Packet packet = Packet.create(id, 0x17, 11, 22, 33, 44, 55, 66, 77, 88);
            when(bus.read(2500, TimeUnit.MILLISECONDS)).thenReturn(Stream.of(packet));

            assertTrue(message, instance.request(clazz, 0x17).isPresent());
        }
    }

    @Test
    public void testRequestByPGN() throws Exception {
        final int pgn = VehicleIdentificationPacket.PGN;
        Packet packet1 = Packet.create(pgn, 0x00, "12345678901234567890*".getBytes(UTF8));
        when(bus.read(2500, TimeUnit.MILLISECONDS)).thenReturn(Stream.of(packet1));

        Optional<VehicleIdentificationPacket> response = instance.request(VehicleIdentificationPacket.class, 0x0);
        assertTrue(response.isPresent());
        VehicleIdentificationPacket result = response.get();
        assertEquals("12345678901234567890", result.getVin());

        verify(bus).send(sendPacketCaptor.capture());
        List<Packet> packets = sendPacketCaptor.getAllValues();
        assertEquals(1, packets.size());
        Packet request = packets.get(0);
        assertEquals(0xEA00, request.getId());
        assertEquals(BUS_ADDR, request.getSource());
        assertEquals(pgn, request.get24(0));
    }

    /**
     * This sends request for DM7 but gets back a DM30
     *
     * @throws Exception
     */
    @Test
    public void testRequestDM7() throws Exception {
        Packet packet1 = Packet.create(DM30ScaledTestResultsPacket.PGN | 0xA5, 0x00, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0x0A,
                0x0B, 0x0C, 0x0D);
        when(bus.read(2500, TimeUnit.MILLISECONDS)).thenReturn(Stream.of(packet1));

        int spn = 1024;

        Packet requestPacket = Packet.create(DM7CommandTestsPacket.PGN | 0x00, BUS_ADDR, 247, spn & 0xFF,
                (spn >> 8) & 0xFF, (spn >> 16) & 0xFF | 31, 0xFF, 0xFF, 0xFF, 0xFF);

        DM30ScaledTestResultsPacket packet = instance
                .requestPacket(requestPacket, DM30ScaledTestResultsPacket.class, 0x00, 4)
                .orElse(null);
        assertNotNull(packet);

        verify(bus).send(sendPacketCaptor.capture());
        Packet request = sendPacketCaptor.getValue();
        assertEquals(DM7CommandTestsPacket.PGN, request.getId());
        assertEquals(BUS_ADDR, request.getSource());
    }

    /**
     * This sends request for DM7 but times out
     *
     * @throws Exception
     */
    @Test
    public void testRequestDM7Timesout() throws Exception {
        when(bus.read(2500, TimeUnit.MILLISECONDS)).thenReturn(Stream.of()).thenReturn(Stream.of())
                .thenReturn(Stream.of());

        int spn = 1024;

        Packet requestPacket = Packet.create(DM7CommandTestsPacket.PGN | 0x00, BUS_ADDR, 247, spn & 0xFF,
                (spn >> 8) & 0xFF, (spn >> 16) & 0xFF | 31, 0xFF, 0xFF, 0xFF, 0xFF);

        DM30ScaledTestResultsPacket packet = instance
                .requestPacket(requestPacket, DM30ScaledTestResultsPacket.class, 0x00, 3)
                .orElse(null);
        assertNull(packet);

        verify(bus, times(3)).send(sendPacketCaptor.capture());
        Packet request = sendPacketCaptor.getValue();
        assertEquals(DM7CommandTestsPacket.PGN, request.getId());
        assertEquals(BUS_ADDR, request.getSource());
    }

    /**
     * This sends request for DM7 and eventually gets back a DM30
     *
     * @throws Exception
     */
    @Test
    public void testRequestDM7WillTryThreeTimes() throws Exception {
        Packet packet1 = Packet.create(DM30ScaledTestResultsPacket.PGN | 0xA5, 0x00, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0x0A,
                0x0B, 0x0C, 0x0D);
        when(bus.read(2500, TimeUnit.MILLISECONDS)).thenReturn(Stream.of()).thenReturn(Stream.of())
                .thenReturn(Stream.of(packet1));

        int spn = 1024;

        Packet requestPacket = Packet.create(DM7CommandTestsPacket.PGN | 0x00, BUS_ADDR, 247, spn & 0xFF,
                (spn >> 8) & 0xFF, (spn >> 16) & 0xFF | 31, 0xFF, 0xFF, 0xFF, 0xFF);

        DM30ScaledTestResultsPacket packet = instance
                .requestPacket(requestPacket, DM30ScaledTestResultsPacket.class, 0x00, 3)
                .orElse(null);
        assertNotNull(packet);

        verify(bus, times(3)).send(sendPacketCaptor.capture());
        Packet request = sendPacketCaptor.getValue();
        assertEquals(DM7CommandTestsPacket.PGN, request.getId());
        assertEquals(BUS_ADDR, request.getSource());
    }

    @Test
    public void testRequestHandlesBusException() throws Exception {
        when(bus.read(2500, TimeUnit.MILLISECONDS)).thenThrow(new BusException("Testing"));

        Optional<DM11ClearActiveDTCsPacket> response = instance.request(DM11ClearActiveDTCsPacket.class, 0x17);
        assertFalse(response.isPresent());

        verify(bus, never()).send(sendPacketCaptor.capture());
    }

    @Test
    public void testRequestHandlesBusExceptionOnSecondAttempt() throws Exception {
        when(bus.read(2500, TimeUnit.MILLISECONDS)).thenReturn(Stream.empty()).thenThrow(new BusException("Testing"));

        Optional<VehicleIdentificationPacket> response = instance.request(VehicleIdentificationPacket.class, 0x00);
        assertFalse(response.isPresent());

        verify(bus).send(sendPacketCaptor.capture());
        List<Packet> packets = sendPacketCaptor.getAllValues();
        assertEquals(1, packets.size());
    }

    @Test
    public void testRequestHandlesBusy() throws Exception {
        instance.setBusyRetryTime(50, TimeUnit.MILLISECONDS);
        final Packet busyPacket = Packet.create(0xE8FF, 0x17, 0x03, 0xFF, 0xFF, 0xFF, BUS_ADDR, 0xD3, 0xFE, 0x00);
        final Packet realPacket = Packet.create(0xE8FF, 0x17, 0x00, 0xFF, 0xFF, 0xFF, BUS_ADDR, 0xD3, 0xFE, 0x00);
        when(bus.read(2500, TimeUnit.MILLISECONDS)).thenReturn(Stream.of(busyPacket)).thenReturn(Stream.of(busyPacket))
                .thenReturn(Stream.of(busyPacket)).thenReturn(Stream.of(busyPacket)).thenReturn(Stream.of(busyPacket))
                .thenReturn(Stream.of(realPacket));

        Optional<DM11ClearActiveDTCsPacket> response = instance.request(DM11ClearActiveDTCsPacket.class, 0x17);
        assertTrue(response.isPresent());
        DM11ClearActiveDTCsPacket packet = response.get();
        assertEquals("Acknowledged", packet.getResponse().toString());

        verify(bus, times(6)).send(sendPacketCaptor.capture());
        List<Packet> packets = sendPacketCaptor.getAllValues();
        assertEquals(6, packets.size());
        Packet request = packets.get(0);
        assertEquals(0xEA17, request.getId());
        assertEquals(BUS_ADDR, request.getSource());
        assertEquals(DM11ClearActiveDTCsPacket.PGN, request.get24(0));
    }

    @Test
    public void testRequestHandlesException() throws Exception {
        Optional<TestPacket> response = instance.request(TestPacket.class, 0x17);
        assertFalse(response.isPresent());
        verify(bus, never()).read(2500, TimeUnit.MILLISECONDS);
        verify(bus, never()).send(sendPacketCaptor.capture());
    }

    @Test
    public void testRequestHandlesTimeouts() throws Exception {
        when(bus.read(2500, TimeUnit.MILLISECONDS)).thenReturn(Stream.empty()).thenReturn(Stream.empty())
                .thenReturn(Stream.empty());

        Optional<VehicleIdentificationPacket> response = instance.request(VehicleIdentificationPacket.class, 0x00);
        assertFalse(response.isPresent());

        verify(bus, times(3)).send(sendPacketCaptor.capture());
        List<Packet> packets = sendPacketCaptor.getAllValues();
        assertEquals(3, packets.size());
    }

    @Test
    public void testRequestIgnoresAcksToOthersAddresses() throws Exception {
        final Packet packet1 = Packet.create(0xE8FF, 0x17, 0x01, 0xFF, 0xFF, 0xFF, BUS_ADDR + 1, 0xD3, 0xFE, 0x00);
        final Packet packet2 = Packet.create(0xE8FF, 0x17, 0x00, 0xFF, 0xFF, 0xFF, BUS_ADDR, 0xD3, 0xFE, 0x00);
        final Packet packet3 = Packet.create(0xE8FF, 0x17, 0x02, 0xFF, 0xFF, 0xFF, BUS_ADDR + 2, 0xD3, 0xFE, 0x00);
        when(bus.read(2500, TimeUnit.MILLISECONDS)).thenReturn(Stream.of(packet1, packet2, packet3));

        Optional<DM11ClearActiveDTCsPacket> response = instance.request(DM11ClearActiveDTCsPacket.class, 0x17);
        assertTrue(response.isPresent());
        DM11ClearActiveDTCsPacket packet = response.get();
        assertEquals("Acknowledged", packet.getResponse().toString());

        verify(bus).send(sendPacketCaptor.capture());
        List<Packet> packets = sendPacketCaptor.getAllValues();
        assertEquals(1, packets.size());
    }

    @Test
    public void testRequestIgnoresAcksToOthersPGNs() throws Exception {
        final Packet packet1 = Packet.create(0xE8FF, 0x17, 0x01, 0xFF, 0xFF, 0xFF, BUS_ADDR, 0xD2, 0xFE, 0x00);
        final Packet packet2 = Packet.create(0xE8FF, 0x17, 0x00, 0xFF, 0xFF, 0xFF, BUS_ADDR, 0xD3, 0xFE, 0x00);
        final Packet packet3 = Packet.create(0xE8FF, 0x17, 0x02, 0xFF, 0xFF, 0xFF, BUS_ADDR, 0xD4, 0xFE, 0x00);
        when(bus.read(2500, TimeUnit.MILLISECONDS)).thenReturn(Stream.of(packet1, packet2, packet3));

        Optional<DM11ClearActiveDTCsPacket> response = instance.request(DM11ClearActiveDTCsPacket.class, 0x17);
        assertTrue(response.isPresent());
        DM11ClearActiveDTCsPacket packet = response.get();
        assertEquals("Acknowledged", packet.getResponse().toString());

        verify(bus).send(sendPacketCaptor.capture());
        List<Packet> packets = sendPacketCaptor.getAllValues();
        assertEquals(1, packets.size());
    }

    @Test
    public void testRequestIgnoresOthersByAddress() throws Exception {
        String expected = "12345678901234567890";
        Packet packet1 = Packet.create(VehicleIdentificationPacket.PGN, 0x00, ("09876543210987654321*").getBytes(UTF8));
        Packet packet2 = Packet.create(VehicleIdentificationPacket.PGN, 0x17, (expected + "*").getBytes(UTF8));
        Packet packet3 = Packet.create(VehicleIdentificationPacket.PGN, 0x21, ("alksdfjlasdjflkajsdf*").getBytes(UTF8));
        when(bus.read(2500, TimeUnit.MILLISECONDS)).thenReturn(Stream.of(packet1, packet2, packet3));

        Optional<VehicleIdentificationPacket> response = instance.request(VehicleIdentificationPacket.class, 0x17);
        assertTrue(response.isPresent());
        VehicleIdentificationPacket result = response.get();
        assertEquals(expected, result.getVin());

        verify(bus).send(sendPacketCaptor.capture());
        List<Packet> packets = sendPacketCaptor.getAllValues();
        assertEquals(1, packets.size());
        Packet packet = packets.get(0);
        assertEquals(0xEA17, packet.getId());
        assertEquals(BUS_ADDR, packet.getSource());
        assertEquals(VehicleIdentificationPacket.PGN, packet.get24(0));
    }

    @Test
    public void testRequestIgnoresOthersByPGN() throws Exception {
        String expected = "12345678901234567890";
        Packet packet1 = Packet.create(VehicleIdentificationPacket.PGN - 1, 0x17,
                ("09876543210987654321*").getBytes(UTF8));
        Packet packet2 = Packet.create(VehicleIdentificationPacket.PGN, 0x17, (expected + "*").getBytes(UTF8));
        Packet packet3 = Packet.create(VehicleIdentificationPacket.PGN + 2, 0x17,
                ("alksdfjlasdjflkajsdf*").getBytes(UTF8));
        when(bus.read(2500, TimeUnit.MILLISECONDS)).thenReturn(Stream.of(packet1, packet2, packet3));

        Optional<VehicleIdentificationPacket> response = instance.request(VehicleIdentificationPacket.class, 0x17);
        assertTrue(response.isPresent());
        VehicleIdentificationPacket result = response.get();
        assertEquals(expected, result.getVin());

        verify(bus).send(sendPacketCaptor.capture());
        List<Packet> packets = sendPacketCaptor.getAllValues();
        assertEquals(1, packets.size());
        Packet packet = packets.get(0);
        assertEquals(0xEA17, packet.getId());
        assertEquals(BUS_ADDR, packet.getSource());
        assertEquals(VehicleIdentificationPacket.PGN, packet.get24(0));
    }

    @Test
    public void testRequestMultipleByClassHandlesException() throws Exception {
        Stream<TestPacket> response = instance.requestMultiple(TestPacket.class);
        assertEquals(0, response.count());
    }

    @Test
    public void testRequestMultipleByClassReturnsAll() throws Exception {
        Packet packet1 = Packet.create(VehicleIdentificationPacket.PGN, 0x00, "EngineVIN*".getBytes(UTF8));
        Packet packet2 = Packet.create(VehicleIdentificationPacket.PGN, 0x17, "ClusterVIN*".getBytes(UTF8));
        Packet packet3 = Packet.create(VehicleIdentificationPacket.PGN, 0x21, "BodyControllerVIN*".getBytes(UTF8));
        when(bus.read(ArgumentMatchers.anyLong(), ArgumentMatchers.any(TimeUnit.class)))
                .thenReturn(Stream.of(packet1, packet2, packet3));

        Packet request = instance.createRequestPacket(VehicleIdentificationPacket.PGN, 0xFF);

        Stream<VehicleIdentificationPacket> response = instance.requestMultiple(VehicleIdentificationPacket.class);
        List<VehicleIdentificationPacket> packets = response.collect(Collectors.toList());
        assertEquals(3, packets.size());
        assertEquals("EngineVIN", packets.get(0).getVin());
        assertEquals("ClusterVIN", packets.get(1).getVin());
        assertEquals("BodyControllerVIN", packets.get(2).getVin());

        verify(bus).send(request);
    }

    @Test
    public void testRequestMultipleHandlesBusException() throws Exception {
        when(bus.read(ArgumentMatchers.anyLong(), ArgumentMatchers.any(TimeUnit.class)))
                .thenThrow(new BusException("Testing"));
        Packet request = instance.createRequestPacket(DM5DiagnosticReadinessPacket.PGN, 0x00);
        Stream<DM5DiagnosticReadinessPacket> response = instance.requestMultiple(DM5DiagnosticReadinessPacket.class,
                request);
        assertEquals(0, response.count());
    }

    @Test
    public void testRequestMultipleHandlesException() throws Exception {
        Stream<TestPacket> response = instance.requestMultiple(TestPacket.class,
                Packet.create(0xEA00 | 0, 0, 0, 0 >> 8, 0 >> 16));
        assertEquals(0, response.count());
    }

    @Test
    public void testRequestMultipleHandlesTimeout() throws Exception {
        when(bus.read(ArgumentMatchers.anyLong(), ArgumentMatchers.any(TimeUnit.class))).thenReturn(Stream.empty())
                .thenReturn(Stream.empty()).thenReturn(Stream.empty());
        Packet request = instance.createRequestPacket(VehicleIdentificationPacket.PGN, 0xFF);
        Stream<VehicleIdentificationPacket> response = instance.requestMultiple(VehicleIdentificationPacket.class,
                request);
        assertEquals(0, response.count());
        verify(bus, times(3)).send(request);
    }

    @Test
    public void testRequestMultipleIgnoresOtherPGNs() throws Exception {
        String expected = "12345678901234567890";
        Packet packet1 = Packet.create(VehicleIdentificationPacket.PGN - 1, 0x17,
                ("09876543210987654321*").getBytes(UTF8));
        Packet packet2 = Packet.create(VehicleIdentificationPacket.PGN, 0x17, (expected + "*").getBytes(UTF8));
        Packet packet3 = Packet.create(VehicleIdentificationPacket.PGN + 2, 0x17,
                ("alksdfjlasdjflkajsdf*").getBytes(UTF8));
        when(bus.read(ArgumentMatchers.anyLong(), ArgumentMatchers.any(TimeUnit.class)))
                .thenReturn(Stream.of(packet1, packet2, packet3));

        Packet request = instance.createRequestPacket(VehicleIdentificationPacket.PGN, 0xFF);

        Stream<VehicleIdentificationPacket> response = instance.requestMultiple(VehicleIdentificationPacket.class,
                request);
        List<VehicleIdentificationPacket> packets = response.collect(Collectors.toList());
        assertEquals(1, packets.size());
        assertEquals(expected, packets.get(0).getVin());

        verify(bus).send(request);
    }

    @Test
    public void testRequestMultipleReturnsAck() throws Exception {
        final Packet packet1 = Packet.create(0xE8FF, 0x17, 0x01, 0xFF, 0xFF, 0xFF, BUS_ADDR, 0xD3, 0xFE, 0x00);
        final Packet packet2 = Packet.create(0xE8FF, 0x17, 0x00, 0xFF, 0xFF, 0xFF, 0x44, 0xD3, 0xFE, 0x00);
        final Packet packet3 = Packet.create(0xEAFF, 0x44, 0x00, 0xFF, 0xFF, 0xFF);
        final Packet packet4 = Packet.create(0xE8FF, 0x17, 0x00, 0xFF, 0xFF, 0xFF, BUS_ADDR, 0xD3, 0xFE, 0x00);
        when(bus.read(ArgumentMatchers.anyLong(), ArgumentMatchers.any(TimeUnit.class)))
                .thenReturn(Stream.of(packet1, packet2, packet3, packet4));

        List<DM11ClearActiveDTCsPacket> responses = instance.requestMultiple(DM11ClearActiveDTCsPacket.class)
                .collect(Collectors.toList());
        assertEquals(1, responses.size());

        DM11ClearActiveDTCsPacket packet = responses.get(0);
        assertEquals("Acknowledged", packet.getResponse().toString());

        verify(bus).send(sendPacketCaptor.capture());
        List<Packet> packets = sendPacketCaptor.getAllValues();
        assertEquals(1, packets.size());
        Packet request = packets.get(0);
        assertEquals(0xEAFF, request.getId());
        assertEquals(BUS_ADDR, request.getSource());
        assertEquals(DM11ClearActiveDTCsPacket.PGN, request.get24(0));
    }

    @Test
    public void testRequestMultipleReturnsAll() throws Exception {
        Packet packet1 = Packet.create(VehicleIdentificationPacket.PGN, 0x00, "EngineVIN*".getBytes(UTF8));
        Packet packet2 = Packet.create(VehicleIdentificationPacket.PGN, 0x17, "ClusterVIN*".getBytes(UTF8));
        Packet packet3 = Packet.create(VehicleIdentificationPacket.PGN, 0x21, "BodyControllerVIN*".getBytes(UTF8));
        when(bus.read(ArgumentMatchers.anyLong(), ArgumentMatchers.any(TimeUnit.class)))
                .thenReturn(Stream.of(packet1, packet2, packet3));

        Packet request = instance.createRequestPacket(VehicleIdentificationPacket.PGN, 0xFF);
        Stream<VehicleIdentificationPacket> response = instance.requestMultiple(VehicleIdentificationPacket.class,
                request);
        List<VehicleIdentificationPacket> packets = response.collect(Collectors.toList());
        assertEquals(3, packets.size());
        assertEquals("EngineVIN", packets.get(0).getVin());
        assertEquals("ClusterVIN", packets.get(1).getVin());
        assertEquals("BodyControllerVIN", packets.get(2).getVin());

        verify(bus).send(request);
    }

    @Test
    public void testRequestMultipleTriesAgain() throws Exception {
        when(bus.read(ArgumentMatchers.anyLong(), ArgumentMatchers.any(TimeUnit.class))).thenReturn(Stream.empty())
                .thenReturn(Stream.of(Packet.create(VehicleIdentificationPacket.PGN, 0x00, "*".getBytes(UTF8))));
        Packet request = instance.createRequestPacket(VehicleIdentificationPacket.PGN, 0xFF);
        Stream<VehicleIdentificationPacket> response = instance.requestMultiple(VehicleIdentificationPacket.class,
                request);
        assertEquals(1, response.count());
        verify(bus, times(2)).send(request);
    }

    @Test
    public void testRequestMultipleTriesThreeTimes() throws Exception {
        when(bus.read(ArgumentMatchers.anyLong(), ArgumentMatchers.any(TimeUnit.class))).thenReturn(Stream.empty())
                .thenReturn(Stream.empty())
                .thenReturn(Stream.of(Packet.create(VehicleIdentificationPacket.PGN, 0x00, "*".getBytes(UTF8))));
        Packet request = instance.createRequestPacket(VehicleIdentificationPacket.PGN, 0xFF);
        Stream<VehicleIdentificationPacket> response = instance.requestMultiple(VehicleIdentificationPacket.class,
                request);
        assertEquals(1, response.count());
        verify(bus, times(3)).send(request);
    }

    @Test
    public void testRequestRawReturns() throws Exception {
        // NACK to addr
        Packet response1 = Packet.create(0xE8FF, 0x01, 0, 0, 0, 0, BUS_ADDR, 0xD3, 0xFE, 0x00);
        // NACK to global
        Packet response2 = Packet.create(0xE8FF, 0x01, 0, 0, 0, 0, 0xFF, 0xD3, 0xFE, 0x00);
        // NACK to addr DS
        Packet response3 = Packet.create(0xE8A5, 0x01, 0, 0, 0, 0, BUS_ADDR, 0xD3, 0xFE, 0x00);
        // NACK to global DS
        Packet response4 = Packet.create(0xE8A5, 0x01, 0, 0, 0, 0, 0xFF, 0xD3, 0xFE, 0x00);
        // ACK to addr
        Packet response5 = Packet.create(0xE8FF, 0x00, 0, 0, 0, 0, BUS_ADDR, 0xD3, 0xFE, 0x00);
        // ACK to global
        Packet response6 = Packet.create(0xE8FF, 0x00, 0, 0, 0, 0, 0xFF, 0xD3, 0xFE, 0x00);
        // ACK to addr DS
        Packet response7 = Packet.create(0xE8A5, 0x00, 0, 0, 0, 0, BUS_ADDR, 0xD3, 0xFE, 0x00);
        // ACK to global DS
        Packet response8 = Packet.create(0xE8A5, 0x00, 0, 0, 0, 0, 0xFF, 0xD3, 0xFE, 0x00);
        // Junk to different PGN
        Packet response9 = Packet.create(0xE8FF, 0x00, 0, 0, 0, 0, 0xFF, 0xFA, 0xFE, 0x00);
        // Junk with different pgn
        Packet response10 = Packet.create(0xF004, 0x00, 0, 0, 0, 0, 0xFF, 0xD3, 0xFE, 0x00);

        when(bus.read(5500, TimeUnit.MILLISECONDS)).thenReturn(Stream.of(response1, response2, response3, response4,
                response5, response6, response7, response8, response9, response10));

        Packet request = instance.createRequestPacket(DM11ClearActiveDTCsPacket.PGN, GLOBAL_ADDR);
        Stream<DM11ClearActiveDTCsPacket> results = instance.requestRaw(DM11ClearActiveDTCsPacket.class, request,
                5500, TimeUnit.MILLISECONDS);
        List<DM11ClearActiveDTCsPacket> packets = results.collect(Collectors.toList());
        assertEquals(8, packets.size());
    }

    @Test
    public void testRequestReturnsAck() throws Exception {
        final Packet packet1 = Packet.create(0xE8FF, 0x17, 0x00, 0xFF, 0xFF, 0xFF, BUS_ADDR, 0xD3, 0xFE, 0x00);
        when(bus.read(2500, TimeUnit.MILLISECONDS)).thenReturn(Stream.of(packet1));

        Optional<DM11ClearActiveDTCsPacket> response = instance.request(DM11ClearActiveDTCsPacket.class, 0x17);
        assertTrue(response.isPresent());
        DM11ClearActiveDTCsPacket packet = response.get();
        assertEquals("Acknowledged", packet.getResponse().toString());

        verify(bus).send(sendPacketCaptor.capture());
        List<Packet> packets = sendPacketCaptor.getAllValues();
        assertEquals(1, packets.size());
        Packet request = packets.get(0);
        assertEquals(0xEA17, request.getId());
        assertEquals(BUS_ADDR, request.getSource());
        assertEquals(DM11ClearActiveDTCsPacket.PGN, request.get24(0));
    }

    @Test
    public void testRequestReturnsFirstResponse() throws Exception {
        Packet packet1 = Packet.create(VehicleIdentificationPacket.PGN, 0x17, "12345678901234567890*".getBytes(UTF8));
        when(bus.read(2500, TimeUnit.MILLISECONDS)).thenReturn(Stream.of(packet1));

        Optional<VehicleIdentificationPacket> response = instance.request(VehicleIdentificationPacket.class, 0x17);
        assertTrue(response.isPresent());
        VehicleIdentificationPacket result = response.get();
        assertEquals("12345678901234567890", result.getVin());

        verify(bus).send(sendPacketCaptor.capture());
        List<Packet> packets = sendPacketCaptor.getAllValues();
        assertEquals(1, packets.size());
        Packet request = packets.get(0);
        assertEquals(0xEA17, request.getId());
        assertEquals(BUS_ADDR, request.getSource());
        assertEquals(VehicleIdentificationPacket.PGN, request.get24(0));
    }

    @Test
    public void testRequestReturnsLastResponse() throws Exception {
        Packet packet1 = Packet.create(VehicleIdentificationPacket.PGN, 0x00, "12345678901234567890*".getBytes(UTF8));
        when(bus.read(2500, TimeUnit.MILLISECONDS)).thenReturn(Stream.empty()).thenReturn(Stream.empty())
                .thenReturn(Stream.of(packet1));

        Optional<VehicleIdentificationPacket> response = instance.request(VehicleIdentificationPacket.class, 0x00);
        assertTrue(response.isPresent());
        VehicleIdentificationPacket result = response.get();
        assertEquals("12345678901234567890", result.getVin());

        verify(bus, times(3)).send(sendPacketCaptor.capture());
        List<Packet> packets = sendPacketCaptor.getAllValues();
        assertEquals(3, packets.size());
    }

    @Test
    public void testRequestReturnsSecondResponse() throws Exception {
        Packet packet1 = Packet.create(VehicleIdentificationPacket.PGN, 0x00, "12345678901234567890*".getBytes(UTF8));
        when(bus.read(2500, TimeUnit.MILLISECONDS)).thenReturn(Stream.empty()).thenReturn(Stream.of(packet1));

        Optional<VehicleIdentificationPacket> response = instance.request(VehicleIdentificationPacket.class, 0x00);
        assertTrue(response.isPresent());
        VehicleIdentificationPacket result = response.get();
        assertEquals("12345678901234567890", result.getVin());

        verify(bus, times(2)).send(sendPacketCaptor.capture());
        List<Packet> packets = sendPacketCaptor.getAllValues();
        assertEquals(2, packets.size());
    }
}
