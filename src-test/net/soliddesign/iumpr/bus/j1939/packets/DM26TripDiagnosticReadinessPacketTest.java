/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.bus.j1939.packets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Objects;

import org.junit.Test;

import net.soliddesign.iumpr.bus.Packet;

/**
 * Unit tests the {@link DM26TripDiagnosticReadinessPacket} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class DM26TripDiagnosticReadinessPacketTest {

    @Test
    public void testEqualsAndHashCode() {
        Packet packet = Packet.create(0, 0, 11, 22, 33, 44, 55, 66, 77, 88);
        DM26TripDiagnosticReadinessPacket instance1 = new DM26TripDiagnosticReadinessPacket(packet);
        DM26TripDiagnosticReadinessPacket instance2 = new DM26TripDiagnosticReadinessPacket(packet);

        assertTrue(instance1.equals(instance2));
        assertTrue(instance2.equals(instance1));
        assertTrue(instance1.hashCode() == instance2.hashCode());
    }

    @Test
    public void testEqualsAndHashCodeSelf() {
        Packet packet = Packet.create(0, 0, 11, 22, 33, 44, 55, 66, 77, 88);
        DM26TripDiagnosticReadinessPacket instance = new DM26TripDiagnosticReadinessPacket(packet);
        assertTrue(instance.equals(instance));
        assertTrue(instance.hashCode() == instance.hashCode());
    }

    @Test
    public void testEqualsContinouslyMonitoredSystems() {
        Packet packet1 = Packet.create(0, 0, 0x11, 0x22, 0x33, 0x00, 0x55, 0x66, 0x77, 0x88);
        DM26TripDiagnosticReadinessPacket instance1 = new DM26TripDiagnosticReadinessPacket(packet1);
        for (int i = 1; i < 255; i++) {
            Packet packet2 = Packet.create(0, 0, 0x11, 0x22, 0x33, i, 0x55, 0x66, 0x77, 0x88);
            DM26TripDiagnosticReadinessPacket instance2 = new DM26TripDiagnosticReadinessPacket(packet2);
            boolean equal = Objects.equals(instance1.getContinuouslyMonitoredSystems(),
                    instance2.getContinuouslyMonitoredSystems());
            assertEquals("Failed with packet " + packet2, equal, instance1.equals(instance2));
        }
    }

    @Test
    public void testEqualsWithObject() {
        Packet packet = Packet.create(0, 0, 11, 22, 33, 44, 55, 66, 77, 88);
        DM26TripDiagnosticReadinessPacket instance = new DM26TripDiagnosticReadinessPacket(packet);
        assertFalse(instance.equals("DM26TripDiagnosticReadinessPacket"));
    }

    @Test
    public void testGetTimeSinceEngineStart() {
        Packet packet = Packet.create(0, 0, 11, 22, 33, 44, 55, 66, 77, 88);
        DM26TripDiagnosticReadinessPacket instance = new DM26TripDiagnosticReadinessPacket(packet);
        assertEquals(5643, instance.getTimeSinceEngineStart(), 0.0);
    }

    @Test
    public void testGetTimeSinceEngineStartWithError() {
        Packet packet = Packet.create(0, 0, 0x00, 0xFE, 33, 44, 55, 66, 77, 88);
        DM26TripDiagnosticReadinessPacket instance = new DM26TripDiagnosticReadinessPacket(packet);
        assertEquals(ParsedPacket.ERROR, instance.getTimeSinceEngineStart(), 0.0);
    }

    @Test
    public void testGetTimeSinceEngineStartWithNA() {
        Packet packet = Packet.create(0, 0, 0x00, 0xFF, 33, 44, 55, 66, 77, 88);
        DM26TripDiagnosticReadinessPacket instance = new DM26TripDiagnosticReadinessPacket(packet);
        assertEquals(ParsedPacket.NOT_AVAILABLE, instance.getTimeSinceEngineStart(), 0.0);
    }

    @Test
    public void testGetWarmUpsSinceClear() {
        Packet packet = Packet.create(0, 0, 11, 22, 33, 44, 55, 66, 77, 88);
        DM26TripDiagnosticReadinessPacket instance = new DM26TripDiagnosticReadinessPacket(packet);
        assertEquals(33, instance.getWarmUpsSinceClear());
    }

    @Test
    public void testGetWarmUpsSinceClearWithError() {
        Packet packet = Packet.create(0, 0, 11, 22, 0xFE, 44, 55, 66, 77, 88);
        DM26TripDiagnosticReadinessPacket instance = new DM26TripDiagnosticReadinessPacket(packet);
        assertEquals((byte) 0xFE, instance.getWarmUpsSinceClear());
    }

    @Test
    public void testGetWarmUpsSinceClearWithNA() {
        Packet packet = Packet.create(0, 0, 11, 22, 0xFF, 44, 55, 66, 77, 88);
        DM26TripDiagnosticReadinessPacket instance = new DM26TripDiagnosticReadinessPacket(packet);
        assertEquals((byte) 0xFF, instance.getWarmUpsSinceClear());
    }

    @Test
    public void testNotEqualsNonContinouslyMonitoredSystemsCompleted() {
        Packet packet1 = Packet.create(0, 0, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88);
        DM26TripDiagnosticReadinessPacket instance1 = new DM26TripDiagnosticReadinessPacket(packet1);
        for (int i = 1; i < 255; i++) {
            Packet packet2 = Packet.create(0, 0, 0x11, 0x22, 0x33, 0x44, 0xFF, 0xFF, i, i);
            DM26TripDiagnosticReadinessPacket instance2 = new DM26TripDiagnosticReadinessPacket(packet2);
            assertFalse("Failed with packet " + packet2, instance1.equals(instance2));
        }
    }

    @Test
    public void testNotEqualsNonContinouslyMonitoredSystemsSupported() {
        Packet packet1 = Packet.create(0, 0, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88);
        DM26TripDiagnosticReadinessPacket instance1 = new DM26TripDiagnosticReadinessPacket(packet1);
        for (int i = 1; i < 255; i++) {
            Packet packet2 = Packet.create(0, 0, 0x11, 0x22, 0x33, 0x44, i, i, 0x77, 0x88);
            DM26TripDiagnosticReadinessPacket instance2 = new DM26TripDiagnosticReadinessPacket(packet2);
            assertFalse("Failed with packet " + packet2, instance1.equals(instance2));
        }
    }

    @Test
    public void testNotEqualsTimeSinceClear() {
        Packet packet1 = Packet.create(0, 0, 11, 22, 33, 44, 55, 66, 77, 88);
        DM26TripDiagnosticReadinessPacket instance1 = new DM26TripDiagnosticReadinessPacket(packet1);
        Packet packet2 = Packet.create(0, 0, 00, 22, 33, 44, 55, 66, 77, 88);
        DM26TripDiagnosticReadinessPacket instance2 = new DM26TripDiagnosticReadinessPacket(packet2);

        assertFalse(instance1.equals(instance2));
    }

    @Test
    public void testNotEqualsTimeSinceClearByte2() {
        Packet packet1 = Packet.create(0, 0, 11, 22, 33, 44, 55, 66, 77, 88);
        DM26TripDiagnosticReadinessPacket instance1 = new DM26TripDiagnosticReadinessPacket(packet1);
        Packet packet2 = Packet.create(0, 0, 11, 00, 33, 44, 55, 66, 77, 88);
        DM26TripDiagnosticReadinessPacket instance2 = new DM26TripDiagnosticReadinessPacket(packet2);

        assertFalse(instance1.equals(instance2));
    }

    @Test
    public void testNotEqualsWarmUps() {
        Packet packet1 = Packet.create(0, 0, 11, 22, 33, 44, 55, 66, 77, 88);
        DM26TripDiagnosticReadinessPacket instance1 = new DM26TripDiagnosticReadinessPacket(packet1);
        Packet packet2 = Packet.create(0, 0, 11, 22, 00, 44, 55, 66, 77, 88);
        DM26TripDiagnosticReadinessPacket instance2 = new DM26TripDiagnosticReadinessPacket(packet2);

        assertFalse(instance1.equals(instance2));
    }

    @Test
    public void testPGN() {
        assertEquals(64952, DM26TripDiagnosticReadinessPacket.PGN);
    }

    @Test
    public void testToString() {
        Packet packet = Packet.create(0, 0, 11, 22, 20, 44, 55, 66, 77, 88);
        DM26TripDiagnosticReadinessPacket instance = new DM26TripDiagnosticReadinessPacket(packet);
        assertEquals("DM26 from Engine #1 (0): Warm-ups: 20, Time Since Engine Start: 5,643 seconds",
                instance.toString());
    }

    @Test
    public void testToStringWithError() {
        Packet packet = Packet.create(0, 0, 00, 0xFE, 0xFE, 44, 55, 66, 77, 88);
        DM26TripDiagnosticReadinessPacket instance = new DM26TripDiagnosticReadinessPacket(packet);
        assertEquals("DM26 from Engine #1 (0): Warm-ups: error, Time Since Engine Start: error", instance.toString());
    }

    @Test
    public void testToStringWithNA() {
        Packet packet = Packet.create(0, 0, 00, 0xFF, 0xFF, 44, 55, 66, 77, 88);
        DM26TripDiagnosticReadinessPacket instance = new DM26TripDiagnosticReadinessPacket(packet);
        assertEquals("DM26 from Engine #1 (0): Warm-ups: not available, Time Since Engine Start: not available",
                instance.toString());
    }
}
