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

import org.junit.Ignore;

import net.soliddesign.iumpr.bus.Packet;

/**
 * Unit tests for the {@link DiagnosticReadinessPacket} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
@Ignore // abstract tests called from subclasses
public abstract class DiagnosticReadinessPacketTest {

    protected DiagnosticReadinessPacket createInstance(int... data) {
        return createInstance(Packet.create(0, 0, data));
    }

    protected abstract DiagnosticReadinessPacket createInstance(Packet packet);

    protected abstract MonitoredSystemStatus findStatus(boolean enabled, boolean complete);

    public void test0x00() {
        DiagnosticReadinessPacket instance = createInstance(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00);
        {
            List<MonitoredSystem> systems = instance.getContinuouslyMonitoredSystems();
            for (MonitoredSystem system : systems) {
                assertEquals(system.getName() + " is wrong", findStatus(false, true), system.getStatus());
            }
        }
        {
            List<MonitoredSystem> systems = instance.getNonContinuouslyMonitoredSystems();
            for (MonitoredSystem system : systems) {
                assertEquals(system.getName() + " is wrong", findStatus(false, true), system.getStatus());
            }
        }
    }

    public void testEqualsAndHashCode() {
        DiagnosticReadinessPacket instance1 = createInstance(0x11, 0x22, 0x33, 0x00, 0x55, 0x66, 0x77, 0x88);
        DiagnosticReadinessPacket instance2 = createInstance(0x11, 0x22, 0x33, 0x00, 0x55, 0x66, 0x77, 0x88);

        assertTrue(instance1.equals(instance2));
        assertTrue(instance2.equals(instance1));
        assertTrue(instance1.hashCode() == instance2.hashCode());
    }

    public void testEqualsAndHashCodeSelf() {
        DiagnosticReadinessPacket instance = createInstance(
                new int[] { 0x11, 0x22, 0x33, 0x00, 0x55, 0x66, 0x77, 0x88 });
        assertTrue(instance.equals(instance));
        assertTrue(instance.hashCode() == instance.hashCode());
    }

    public void testEqualsContinouslyMonitoredSystems() {
        DiagnosticReadinessPacket instance1 = createInstance(0x11, 0x22, 0x33, 0x00, 0x55, 0x66, 0x77, 0x88);
        for (int i = 1; i < 255; i++) {
            DiagnosticReadinessPacket instance2 = createInstance(0x11, 0x22, 0x33, i, 0x55, 0x66, 0x77, 0x88);
            boolean equal = Objects.equals(instance1.getContinuouslyMonitoredSystems(),
                    instance2.getContinuouslyMonitoredSystems());
            assertEquals("Failed at index " + i, equal, instance1.equals(instance2));
        }
    }

    public void testEqualsWithObject() {
        DiagnosticReadinessPacket instance = createInstance(0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88);
        assertFalse(instance.equals(new Object()));
    }

    public void testGetContinouslyMonitoredSystemsComprehensiveComponentMonitoring() {
        final String name = "Comprehensive component   ";
        validateContinouslyMonitoredSystems(name, 0, 0x00, findStatus(false, true));
        validateContinouslyMonitoredSystems(name, 0, 0x04, findStatus(true, true));
        validateContinouslyMonitoredSystems(name, 0, 0x40, findStatus(false, false));
        validateContinouslyMonitoredSystems(name, 0, 0x44, findStatus(true, false));
    }

    public void testGetContinouslyMonitoredSystemsFuelSystemMonitoring() {
        final String name = "Fuel System               ";
        validateContinouslyMonitoredSystems(name, 1, 0x00, findStatus(false, true));
        validateContinouslyMonitoredSystems(name, 1, 0x02, findStatus(true, true));
        validateContinouslyMonitoredSystems(name, 1, 0x20, findStatus(false, false));
        validateContinouslyMonitoredSystems(name, 1, 0x22, findStatus(true, false));
    }

    public void testGetContinouslyMonitoredSystemsMisfireMonitoring() {
        final String name = "Misfire                   ";
        validateContinouslyMonitoredSystems(name, 2, 0x00, findStatus(false, true));
        validateContinouslyMonitoredSystems(name, 2, 0x01, findStatus(true, true));
        validateContinouslyMonitoredSystems(name, 2, 0x10, findStatus(false, false));
        validateContinouslyMonitoredSystems(name, 2, 0x11, findStatus(true, false));
    }

    public void testGetMonitoredSystems() {
        DiagnosticReadinessPacket instance = createInstance(new int[] { 0, 0, 0, 0, 0, 0, 0, 0 });

        List<MonitoredSystem> nonContSystems = instance.getNonContinuouslyMonitoredSystems();
        List<MonitoredSystem> contSystems = instance.getContinuouslyMonitoredSystems();
        Set<MonitoredSystem> allSystems = instance.getMonitoredSystems();
        assertTrue(allSystems.containsAll(nonContSystems));
        assertTrue(allSystems.containsAll(contSystems));
        assertEquals(nonContSystems.size() + contSystems.size(), allSystems.size());
    }

    public void testGetNonContinouslyMonitoredSystems() {
        validateNonContinouslyMonitoredSystem1("EGR/VVT system            ", 0, 0x80);
        validateNonContinouslyMonitoredSystem1("Exhaust Gas Sensor heater ", 1, 0x40);
        validateNonContinouslyMonitoredSystem1("Exhaust Gas Sensor        ", 2, 0x20);
        validateNonContinouslyMonitoredSystem1("A/C system refrigerant    ", 3, 0x10);
        validateNonContinouslyMonitoredSystem1("Secondary air system      ", 4, 0x08);
        validateNonContinouslyMonitoredSystem1("Evaporative system        ", 5, 0x04);
        validateNonContinouslyMonitoredSystem1("Heated catalyst           ", 6, 0x02);
        validateNonContinouslyMonitoredSystem1("Catalyst                  ", 7, 0x01);
        validateNonContinouslyMonitoredSystem2("NMHC converting catalyst  ", 8, 0x10);
        validateNonContinouslyMonitoredSystem2("NOx catalyst/adsorber     ", 9, 0x08);
        validateNonContinouslyMonitoredSystem2("Diesel Particulate Filter ", 10, 0x04);
        validateNonContinouslyMonitoredSystem2("Boost pressure control sys", 11, 0x02);
        validateNonContinouslyMonitoredSystem2("Cold start aid system     ", 12, 0x01);
    }

    public void testNotEqualsNonContinouslyMonitoredSystemsCompleted() {
        DiagnosticReadinessPacket instance1 = createInstance(0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88);
        for (int i = 1; i < 255; i++) {
            DiagnosticReadinessPacket instance2 = createInstance(0x11, 0x22, 0x33, 0x44, 0xFF, 0xFF, i, i);
            assertFalse("Failed with index " + i, instance1.equals(instance2));
        }
    }

    public void testNotEqualsNonContinouslyMonitoredSystemsSupported() {
        DiagnosticReadinessPacket instance1 = createInstance(0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88);
        for (int i = 1; i < 255; i++) {
            DiagnosticReadinessPacket instance2 = createInstance(0x11, 0x22, 0x33, 0x44, i, i, 0x77, 0x88);
            assertFalse("Failed with index " + i, instance1.equals(instance2));
        }
    }

    public void testNotEqualsSourceAddress() {
        DiagnosticReadinessPacket instance1 = createInstance(Packet.create(0, 0, 11, 22, 33, 44, 55, 66, 77, 88));
        DiagnosticReadinessPacket instance2 = createInstance(Packet.create(0, 99, 11, 22, 33, 44, 55, 66, 77, 88));

        assertFalse(instance1.equals(instance2));
        assertFalse(instance2.equals(instance1));
    }

    private void validateContinouslyMonitoredSystems(String name, int index, int value, MonitoredSystemStatus status) {
        DiagnosticReadinessPacket instance = createInstance(0, 0, 0, value, 0, 0, 0, 0);

        final List<MonitoredSystem> systems = instance.getContinuouslyMonitoredSystems();
        assertEquals(3, systems.size());

        final MonitoredSystem system = systems.get(index);
        assertEquals(name, system.getName());
        assertEquals(status, system.getStatus());
        assertEquals(instance.getSourceAddress(), system.getSourceAddress());
    }

    private void validateNonContinouslyMonitoredSystem1(final String name, final int index, final int mask) {
        validateNonContinouslyMonitoredSystems1(name, index, 0x00, 0x00, findStatus(false, true));
        validateNonContinouslyMonitoredSystems1(name, index, 0x00, mask, findStatus(false, false));
        validateNonContinouslyMonitoredSystems1(name, index, mask, 0x00, findStatus(true, true));
        validateNonContinouslyMonitoredSystems1(name, index, mask, mask, findStatus(true, false));
    }

    private void validateNonContinouslyMonitoredSystem2(final String name, final int index, final int mask) {
        validateNonContinouslyMonitoredSystems2(name, index, 0x00, 0x00, findStatus(false, true));
        validateNonContinouslyMonitoredSystems2(name, index, 0x00, mask, findStatus(false, false));
        validateNonContinouslyMonitoredSystems2(name, index, mask, 0x00, findStatus(true, true));
        validateNonContinouslyMonitoredSystems2(name, index, mask, mask, findStatus(true, false));
    }

    private void validateNonContinouslyMonitoredSystems1(String name, int index, int lowerByte, int upperByte,
            MonitoredSystemStatus status) {
        DiagnosticReadinessPacket instance = createInstance(0, 0, 0, 0, lowerByte, 0, upperByte, 0);

        final List<MonitoredSystem> systems = instance.getNonContinuouslyMonitoredSystems();
        assertEquals(13, systems.size());

        final MonitoredSystem system = systems.get(index);
        assertEquals(name, system.getName());
        assertEquals(status, system.getStatus());
    }

    private void validateNonContinouslyMonitoredSystems2(String name, int index, int lowerByte, int upperByte,
            MonitoredSystemStatus status) {
        DiagnosticReadinessPacket instance = createInstance(0, 0, 0, 0, 0, lowerByte, 0, upperByte);

        final List<MonitoredSystem> systems = instance.getNonContinuouslyMonitoredSystems();
        assertEquals(13, systems.size());

        final MonitoredSystem system = systems.get(index);
        assertEquals(name, system.getName());
        assertEquals(status, system.getStatus());
        assertEquals(instance.getSourceAddress(), system.getSourceAddress());
    }
}
