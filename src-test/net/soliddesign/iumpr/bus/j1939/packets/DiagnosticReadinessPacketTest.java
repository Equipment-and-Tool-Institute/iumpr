/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.bus.j1939.packets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.junit.Test;

import net.soliddesign.iumpr.bus.Packet;
import net.soliddesign.iumpr.bus.j1939.packets.MonitoredSystem.Status;

/**
 * Unit tests for the {@link DiagnosticReadinessPacket} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class DiagnosticReadinessPacketTest {

    private static DiagnosticReadinessPacket createInstance(int[] data) {
        Packet packet = Packet.create(0, 0, data);
        DiagnosticReadinessPacket instance = new DiagnosticReadinessPacket(packet);
        return instance;
    }

    @Test
    public void testEqualsAndHashCode() {
        Packet packet = Packet.create(0, 0, 11, 22, 33, 44, 55, 66, 77, 88);
        DiagnosticReadinessPacket instance1 = new DiagnosticReadinessPacket(packet);
        DiagnosticReadinessPacket instance2 = new DiagnosticReadinessPacket(packet);

        assertTrue(instance1.equals(instance2));
        assertTrue(instance2.equals(instance1));
        assertTrue(instance1.hashCode() == instance2.hashCode());
    }

    @Test
    public void testEqualsAndHashCodeSelf() {
        Packet packet = Packet.create(0, 0, 11, 22, 33, 44, 55, 66, 77, 88);
        DiagnosticReadinessPacket instance = new DiagnosticReadinessPacket(packet);
        assertTrue(instance.equals(instance));
        assertTrue(instance.hashCode() == instance.hashCode());
    }

    @Test
    public void testEqualsContinouslyMonitoredSystems() {
        Packet packet1 = Packet.create(0, 0, 0x11, 0x22, 0x33, 0x00, 0x55, 0x66, 0x77, 0x88);
        DiagnosticReadinessPacket instance1 = new DiagnosticReadinessPacket(packet1);
        for (int i = 1; i < 255; i++) {
            Packet packet2 = Packet.create(0, 0, 0x11, 0x22, 0x33, i, 0x55, 0x66, 0x77, 0x88);
            DiagnosticReadinessPacket instance2 = new DiagnosticReadinessPacket(packet2);
            boolean equal = Objects.equals(instance1.getContinuouslyMonitoredSystems(),
                    instance2.getContinuouslyMonitoredSystems());
            assertEquals("Failed with packet " + packet2, equal, instance1.equals(instance2));
        }
    }

    @Test
    public void testEqualsWithObject() {
        Packet packet = Packet.create(0, 0, 11, 22, 33, 44, 55, 66, 77, 88);
        DiagnosticReadinessPacket instance = new DiagnosticReadinessPacket(packet);
        assertFalse(instance.equals("DiagnosticReadinessPacket"));
    }

    @Test
    public void testGetContinouslyMonitoredSystemsComprehensiveComponentMonitoring() {
        final String name = "Comprehensive component monitoring                    ";
        validateContinouslyMonitoredSystems(name, 0, 0x00, Status.NOT_SUPPORTED);
        validateContinouslyMonitoredSystems(name, 0, 0x04, Status.COMPLETE);
        validateContinouslyMonitoredSystems(name, 0, 0x40, Status.NOT_SUPPORTED);
        validateContinouslyMonitoredSystems(name, 0, 0x44, Status.NOT_COMPLETE);
    }

    @Test
    public void testGetContinouslyMonitoredSystemsFuelSystemMonitoring() {
        final String name = "Fuel System monitoring                                ";
        validateContinouslyMonitoredSystems(name, 1, 0x00, Status.NOT_SUPPORTED);
        validateContinouslyMonitoredSystems(name, 1, 0x02, Status.COMPLETE);
        validateContinouslyMonitoredSystems(name, 1, 0x20, Status.NOT_SUPPORTED);
        validateContinouslyMonitoredSystems(name, 1, 0x22, Status.NOT_COMPLETE);
    }

    @Test
    public void testGetContinouslyMonitoredSystemsMisfireMonitoring() {
        final String name = "Misfire monitoring                                    ";
        validateContinouslyMonitoredSystems(name, 2, 0x00, Status.NOT_SUPPORTED);
        validateContinouslyMonitoredSystems(name, 2, 0x01, Status.COMPLETE);
        validateContinouslyMonitoredSystems(name, 2, 0x10, Status.NOT_SUPPORTED);
        validateContinouslyMonitoredSystems(name, 2, 0x11, Status.NOT_COMPLETE);
    }

    @Test
    public void testGetMonitoredSystems() {
        DiagnosticReadinessPacket instance = createInstance(new int[] { 0, 0, 0, 0, 0, 0, 0, 0 });

        List<MonitoredSystem> nonContSystems = instance.getNonContinuouslyMonitoredSystems();
        List<MonitoredSystem> contSystems = instance.getContinuouslyMonitoredSystems();
        Set<MonitoredSystem> allSystems = instance.getMonitoredSystems();
        assertTrue(allSystems.containsAll(nonContSystems));
        assertTrue(allSystems.containsAll(contSystems));
        assertEquals(nonContSystems.size() + contSystems.size(), allSystems.size());
    }

    @Test
    public void testGetNonContinouslyMonitoredSystems() {
        validateNonContinouslyMonitoredSystem1("EGR/VVT system monitoring                             ", 0, 0x80);
        validateNonContinouslyMonitoredSystem1("Exhaust Gas Sensor heater monitoring                  ", 1, 0x40);
        validateNonContinouslyMonitoredSystem1("Exhaust Gas Sensor monitoring                         ", 2, 0x20);
        validateNonContinouslyMonitoredSystem1("A/C system refrigerant monitoring                     ", 3, 0x10);
        validateNonContinouslyMonitoredSystem1("Secondary air system monitoring                       ", 4, 0x08);
        validateNonContinouslyMonitoredSystem1("Evaporative system monitoring                         ", 5, 0x04);
        validateNonContinouslyMonitoredSystem1("Heated catalyst monitoring                            ", 6, 0x02);
        validateNonContinouslyMonitoredSystem1("Catalyst monitoring                                   ", 7, 0x01);
        validateNonContinouslyMonitoredSystem2("NMHC converting catalyst monitoring                   ", 8, 0x10);
        validateNonContinouslyMonitoredSystem2("NOx converting catalyst and/or NOx adsorber monitoring", 9, 0x08);
        validateNonContinouslyMonitoredSystem2("Diesel Particulate Filter (DPF) monitoring            ", 10, 0x04);
        validateNonContinouslyMonitoredSystem2("Boost pressure control system monitoring              ", 11, 0x02);
        validateNonContinouslyMonitoredSystem2("Cold start aid system monitoring                      ", 12, 0x01);
    }

    @Test
    public void testNotEqualsNonContinouslyMonitoredSystemsCompleted() {
        Packet packet1 = Packet.create(0, 0, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88);
        DiagnosticReadinessPacket instance1 = new DiagnosticReadinessPacket(packet1);
        for (int i = 1; i < 255; i++) {
            Packet packet2 = Packet.create(0, 0, 0x11, 0x22, 0x33, 0x44, 0xFF, 0xFF, i, i);
            DiagnosticReadinessPacket instance2 = new DiagnosticReadinessPacket(packet2);
            assertFalse("Failed with packet " + packet2, instance1.equals(instance2));
        }
    }

    @Test
    public void testNotEqualsNonContinouslyMonitoredSystemsSupported() {
        Packet packet1 = Packet.create(0, 0, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88);
        DiagnosticReadinessPacket instance1 = new DiagnosticReadinessPacket(packet1);
        for (int i = 1; i < 255; i++) {
            Packet packet2 = Packet.create(0, 0, 0x11, 0x22, 0x33, 0x44, i, i, 0x77, 0x88);
            DiagnosticReadinessPacket instance2 = new DiagnosticReadinessPacket(packet2);
            assertFalse("Failed with packet " + packet2, instance1.equals(instance2));
        }
    }

    @Test
    public void testNotEqualsSourceAddress() {
        Packet packet1 = Packet.create(0, 0, 11, 22, 33, 44, 55, 66, 77, 88);
        Packet packet2 = Packet.create(0, 99, 11, 22, 33, 44, 55, 66, 77, 88);
        DiagnosticReadinessPacket instance1 = new DiagnosticReadinessPacket(packet1);
        DiagnosticReadinessPacket instance2 = new DiagnosticReadinessPacket(packet2);

        assertFalse(instance1.equals(instance2));
        assertFalse(instance2.equals(instance1));
    }

    private void validateContinouslyMonitoredSystems(String name, int index, int value, Status status) {
        int[] data = new int[] { 0, 0, 0, value, 0, 0, 0, 0 };

        DiagnosticReadinessPacket instance = createInstance(data);

        final List<MonitoredSystem> systems = instance.getContinuouslyMonitoredSystems();
        assertEquals(3, systems.size());

        final MonitoredSystem system = systems.get(index);
        assertEquals(name, system.getName());
        assertEquals(status, system.getStatus());
        assertEquals(instance.getSourceAddress(), system.getSourceAddress());
    }

    private void validateNonContinouslyMonitoredSystem1(final String name, final int index, final int mask) {
        validateNonContinouslyMonitoredSystems1(name, index, 0x00, 0x00, Status.NOT_SUPPORTED);
        validateNonContinouslyMonitoredSystems1(name, index, 0x00, mask, Status.NOT_SUPPORTED);
        validateNonContinouslyMonitoredSystems1(name, index, mask, 0x00, Status.COMPLETE);
        validateNonContinouslyMonitoredSystems1(name, index, mask, mask, Status.NOT_COMPLETE);
    }

    private void validateNonContinouslyMonitoredSystem2(final String name, final int index, final int mask) {
        validateNonContinouslyMonitoredSystems2(name, index, 0x00, 0x00, Status.NOT_SUPPORTED);
        validateNonContinouslyMonitoredSystems2(name, index, 0x00, mask, Status.NOT_SUPPORTED);
        validateNonContinouslyMonitoredSystems2(name, index, mask, 0x00, Status.COMPLETE);
        validateNonContinouslyMonitoredSystems2(name, index, mask, mask, Status.NOT_COMPLETE);
    }

    private void validateNonContinouslyMonitoredSystems1(String name, int index, int lowerByte, int upperByte,
            Status status) {
        int[] data = new int[] { 0, 0, 0, 0, lowerByte, 0, upperByte, 0 };

        DiagnosticReadinessPacket instance = createInstance(data);

        final List<MonitoredSystem> systems = instance.getNonContinuouslyMonitoredSystems();
        assertEquals(13, systems.size());

        final MonitoredSystem system = systems.get(index);
        assertEquals(name, system.getName());
        assertEquals(status, system.getStatus());
    }

    private void validateNonContinouslyMonitoredSystems2(String name, int index, int lowerByte, int upperByte,
            Status status) {
        int[] data = new int[] { 0, 0, 0, 0, 0, lowerByte, 0, upperByte };

        DiagnosticReadinessPacket instance = createInstance(data);

        final List<MonitoredSystem> systems = instance.getNonContinuouslyMonitoredSystems();
        assertEquals(13, systems.size());

        final MonitoredSystem system = systems.get(index);
        assertEquals(name, system.getName());
        assertEquals(status, system.getStatus());
        assertEquals(instance.getSourceAddress(), system.getSourceAddress());
    }

}
