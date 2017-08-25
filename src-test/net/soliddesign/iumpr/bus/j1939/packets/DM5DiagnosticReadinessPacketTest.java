/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.bus.j1939.packets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.junit.Test;

import net.soliddesign.iumpr.bus.Packet;

/**
 * Unit tests the {@link DM5DiagnosticReadinessPacket} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class DM5DiagnosticReadinessPacketTest {
    @Test
    public void testEqualsAndHashCode() {
        Packet packet = Packet.create(65230, 0, 11, 22, 33, 44, 55, 66, 77, 88);
        DM5DiagnosticReadinessPacket instance1 = new DM5DiagnosticReadinessPacket(packet);
        DM5DiagnosticReadinessPacket instance2 = new DM5DiagnosticReadinessPacket(packet);

        assertTrue(instance1.equals(instance2));
        assertTrue(instance2.equals(instance1));
        assertTrue(instance1.hashCode() == instance2.hashCode());
    }

    @Test
    public void testEqualsAndHashCodeSelf() {
        Packet packet = Packet.create(65230, 0, 11, 22, 33, 44, 55, 66, 77, 88);
        DM5DiagnosticReadinessPacket instance = new DM5DiagnosticReadinessPacket(packet);
        assertTrue(instance.equals(instance));
        assertTrue(instance.hashCode() == instance.hashCode());
    }

    @Test
    public void testEqualsContinouslyMonitoredSystems() {
        Packet packet1 = Packet.create(65230, 0, 0x11, 0x22, 0x33, 0x00, 0x55, 0x66, 0x77, 0x88);
        DM5DiagnosticReadinessPacket instance1 = new DM5DiagnosticReadinessPacket(packet1);
        for (int i = 1; i < 255; i++) {
            Packet packet2 = Packet.create(0, 0, 0x11, 0x22, 0x33, i, 0x55, 0x66, 0x77, 0x88);
            DM5DiagnosticReadinessPacket instance2 = new DM5DiagnosticReadinessPacket(packet2);
            boolean equal = Objects.equals(instance1.getContinuouslyMonitoredSystems(),
                    instance2.getContinuouslyMonitoredSystems());
            assertEquals("Failed with packet " + packet2, equal, instance1.equals(instance2));
        }
    }

    @Test
    public void testEqualsWithObject() {
        Packet packet = Packet.create(65230, 0, 11, 22, 33, 44, 55, 66, 77, 88);
        DM5DiagnosticReadinessPacket instance = new DM5DiagnosticReadinessPacket(packet);
        assertFalse(instance.equals(new Object()));
    }

    @Test
    public void testGetActiveCodeCount() {
        Packet packet = Packet.create(65230, 0, 11, 22, 33, 44, 55, 66, 77, 88);
        DM5DiagnosticReadinessPacket instance = new DM5DiagnosticReadinessPacket(packet);
        assertEquals(11, instance.getActiveCodeCount());
    }

    @Test
    public void testGetActiveCodeCountWithError() {
        Packet packet = Packet.create(65230, 0, 0xFE, 22, 33, 44, 55, 66, 77, 88);
        DM5DiagnosticReadinessPacket instance = new DM5DiagnosticReadinessPacket(packet);
        assertEquals((byte) 0xFE, instance.getActiveCodeCount());
    }

    @Test
    public void testGetActiveCodeCountWithNA() {
        Packet packet = Packet.create(65230, 0, 0xFF, 22, 33, 44, 55, 66, 77, 88);
        DM5DiagnosticReadinessPacket instance = new DM5DiagnosticReadinessPacket(packet);
        assertEquals((byte) 0xFF, instance.getActiveCodeCount());
    }

    @Test
    public void testGetOBDCompliance() {
        Packet packet = Packet.create(65230, 0, 11, 22, 33, 44, 55, 66, 77, 88);
        DM5DiagnosticReadinessPacket instance = new DM5DiagnosticReadinessPacket(packet);
        assertEquals(33, instance.getOBDCompliance());
    }

    @Test
    public void testGetPreviouslyActiveCodeCount() {
        Packet packet = Packet.create(65230, 0, 11, 22, 33, 44, 55, 66, 77, 88);
        DM5DiagnosticReadinessPacket instance = new DM5DiagnosticReadinessPacket(packet);
        assertEquals(22, instance.getPreviouslyActiveCodeCount());
    }

    @Test
    public void testGetPreviouslyActiveCodeCountWithError() {
        Packet packet = Packet.create(65230, 0, 11, 0xFE, 33, 44, 55, 66, 77, 88);
        DM5DiagnosticReadinessPacket instance = new DM5DiagnosticReadinessPacket(packet);
        assertEquals((byte) 0xFE, instance.getPreviouslyActiveCodeCount());
    }

    @Test
    public void testGetPreviouslyActiveCodeCountWithNA() {
        Packet packet = Packet.create(65230, 0, 11, 0xFF, 33, 44, 55, 66, 77, 88);
        DM5DiagnosticReadinessPacket instance = new DM5DiagnosticReadinessPacket(packet);
        assertEquals((byte) 0xFF, instance.getPreviouslyActiveCodeCount());
    }

    @Test
    public void testisHdObdComplianceFalse() {
        Packet packet = Packet.create(65230, 0, 11, 22, 33, 44, 55, 66, 77, 88);
        DM5DiagnosticReadinessPacket instance = new DM5DiagnosticReadinessPacket(packet);
        assertFalse(instance.isHdObd());
    }

    @Test
    public void testisHdObdComplianceTrue19() {
        Packet packet = Packet.create(65230, 0, 11, 22, 19, 44, 55, 66, 77, 88);
        DM5DiagnosticReadinessPacket instance = new DM5DiagnosticReadinessPacket(packet);
        assertTrue(instance.isHdObd());
    }

    @Test
    public void testisHdObdComplianceTrue20() {
        Packet packet = Packet.create(65230, 0, 11, 22, 20, 44, 55, 66, 77, 88);
        DM5DiagnosticReadinessPacket instance = new DM5DiagnosticReadinessPacket(packet);
        assertTrue(instance.isHdObd());
    }

    @Test
    public void testNotEqualsActiveCount() {
        Packet packet1 = Packet.create(65230, 0, 11, 22, 33, 44, 55, 66, 77, 88);
        DM5DiagnosticReadinessPacket instance1 = new DM5DiagnosticReadinessPacket(packet1);
        Packet packet2 = Packet.create(65230, 0, 00, 22, 33, 44, 55, 66, 77, 88);
        DM5DiagnosticReadinessPacket instance2 = new DM5DiagnosticReadinessPacket(packet2);

        assertFalse(instance1.equals(instance2));
    }

    @Test
    public void testNotEqualsNonContinouslyMonitoredSystemsCompleted() {
        Packet packet1 = Packet.create(0, 0, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88);
        DM5DiagnosticReadinessPacket instance1 = new DM5DiagnosticReadinessPacket(packet1);
        for (int i = 1; i < 255; i++) {
            Packet packet2 = Packet.create(0, 0, 0x11, 0x22, 0x33, 0x44, 0xFF, 0xFF, i, i);
            DM5DiagnosticReadinessPacket instance2 = new DM5DiagnosticReadinessPacket(packet2);
            assertFalse("Failed with packet " + packet2, instance1.equals(instance2));
        }
    }

    @Test
    public void testNotEqualsNonContinouslyMonitoredSystemsSupported() {
        Packet packet1 = Packet.create(0, 0, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88);
        DM5DiagnosticReadinessPacket instance1 = new DM5DiagnosticReadinessPacket(packet1);
        for (int i = 1; i < 255; i++) {
            Packet packet2 = Packet.create(0, 0, 0x11, 0x22, 0x33, 0x44, i, i, 0x77, 0x88);
            DM5DiagnosticReadinessPacket instance2 = new DM5DiagnosticReadinessPacket(packet2);
            assertFalse("Failed with packet " + packet2, instance1.equals(instance2));
        }
    }

    @Test
    public void testNotEqualsOBDCompliance() {
        Packet packet1 = Packet.create(65230, 0, 11, 22, 33, 44, 55, 66, 77, 88);
        DM5DiagnosticReadinessPacket instance1 = new DM5DiagnosticReadinessPacket(packet1);
        Packet packet2 = Packet.create(65230, 0, 11, 22, 00, 44, 55, 66, 77, 88);
        DM5DiagnosticReadinessPacket instance2 = new DM5DiagnosticReadinessPacket(packet2);

        assertFalse(instance1.equals(instance2));
    }

    @Test
    public void testNotEqualsPreviouslyActiveCount() {
        Packet packet1 = Packet.create(65230, 0, 11, 22, 33, 44, 55, 66, 77, 88);
        DM5DiagnosticReadinessPacket instance1 = new DM5DiagnosticReadinessPacket(packet1);
        Packet packet2 = Packet.create(65230, 0, 11, 00, 33, 44, 55, 66, 77, 88);
        DM5DiagnosticReadinessPacket instance2 = new DM5DiagnosticReadinessPacket(packet2);

        assertFalse(instance1.equals(instance2));
    }

    @Test
    public void testPGN() {
        assertEquals(65230, DM5DiagnosticReadinessPacket.PGN);
    }

    @Test
    public void testToString() {
        Packet packet = Packet.create(65230, 0, 11, 22, 20, 44, 55, 66, 77, 88);
        DM5DiagnosticReadinessPacket instance = new DM5DiagnosticReadinessPacket(packet);
        assertEquals(
                "DM5 from Engine #1 (0): OBD Compliance: HD OBD (20), Active Codes: 11, Previously Active Codes: 22",
                instance.toString());
    }

    @Test
    public void testToStringWithAllOBDComplianceValues() {
        Map<Integer, String> testCases = new HashMap<>();
        testCases.put(1, "OBD II");
        testCases.put(2, "OBD");
        testCases.put(3, "OBD and OBD II");
        testCases.put(4, "OBD I");
        testCases.put(5, "Not intended to meet OBD II requirements");
        testCases.put(6, "EOBD");
        testCases.put(7, "EOBD and OBD II");
        testCases.put(8, "EOBD and OBD");
        testCases.put(9, "EOBD, OBD and OBD II");
        testCases.put(10, "JOBD");
        testCases.put(11, "JOBD and OBD II");
        testCases.put(12, "JOBD and EOBD");
        testCases.put(13, "JOBD, EOBD and OBD II");
        testCases.put(14, "Heavy Duty Vehicles (EURO IV) B1");
        testCases.put(15, "Heavy Duty Vehicles (EURO V) B2");
        testCases.put(16, "Heavy Duty Vehicles (EURO EEC) C (gas engines)");
        testCases.put(17, "EMD");
        testCases.put(18, "EMD+");
        testCases.put(19, "HD OBD P");
        testCases.put(20, "HD OBD");
        testCases.put(21, "WWH OBD");
        testCases.put(22, "OBD II");
        testCases.put(23, "HD EOBD");
        testCases.put(24, "Reserved for SAE/Unknown");
        testCases.put(25, "OBD-M (SI-SD/I)");
        testCases.put(26, "EURO VI");
        testCases.put(34, "OBD, OBD II, HD OBD");
        testCases.put(35, "OBD, OBD II, HD OBD P");
        testCases.put(251, "value 251");
        testCases.put(252, "value 252");
        testCases.put(253, "value 253");
        testCases.put(254, "Error");
        testCases.put(255, "Not available");

        for (Entry<Integer, String> testCase : testCases.entrySet()) {
            int value = testCase.getKey();
            String expected = testCase.getValue();
            DM5DiagnosticReadinessPacket instance = new DM5DiagnosticReadinessPacket(
                    Packet.create(65230, 0, 11, 22, value, 44, 55, 66, 77, 88));
            assertEquals("DM5 from Engine #1 (0): OBD Compliance: " + expected + " (" + value
                    + "), Active Codes: 11, Previously Active Codes: 22", instance.toString());
        }
    }

    @Test
    public void testToStringWithError() {
        Packet packet = Packet.create(65230, 0, 0xFE, 0xFE, 0xFE, 44, 55, 66, 77, 88);
        DM5DiagnosticReadinessPacket instance = new DM5DiagnosticReadinessPacket(packet);
        assertEquals(
                "DM5 from Engine #1 (0): OBD Compliance: Error (254), Active Codes: error, Previously Active Codes: error",
                instance.toString());
    }

    @Test
    public void testToStringWithNA() {
        Packet packet = Packet.create(65230, 0, 0xFF, 0xFF, 0xFF, 44, 55, 66, 77, 88);
        DM5DiagnosticReadinessPacket instance = new DM5DiagnosticReadinessPacket(packet);
        assertEquals(
                "DM5 from Engine #1 (0): OBD Compliance: Not available (255), Active Codes: not available, Previously Active Codes: not available",
                instance.toString());
    }

}
