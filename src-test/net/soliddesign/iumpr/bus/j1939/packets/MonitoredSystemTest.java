/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.bus.j1939.packets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.soliddesign.iumpr.bus.j1939.packets.MonitoredSystem.Status;

/**
 * Unit tests the {@link MonitoredSystem} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class MonitoredSystemTest {

    @Test
    public void testEqualsHashCode() {
        MonitoredSystem instance1 = new MonitoredSystem("Name", Status.COMPLETE, 0, 123);
        MonitoredSystem instance2 = new MonitoredSystem("Name", Status.COMPLETE, 0, 123);
        assertTrue(instance1.equals(instance2));
        assertTrue(instance2.equals(instance1));
        assertTrue(instance1.hashCode() == instance2.hashCode());
        assertEquals(0, instance1.compareTo(instance2));
    }

    @Test
    public void testEqualsHashCodeSelf() {
        MonitoredSystem instance = new MonitoredSystem("Name", Status.COMPLETE, 0, 123);
        assertTrue(instance.equals(instance));
        assertTrue(instance.hashCode() == instance.hashCode());
        assertEquals(0, instance.compareTo(instance));
    }

    @Test
    public void testGetId() {
        MonitoredSystem instance = new MonitoredSystem("Name", Status.COMPLETE, 0, 123);
        assertEquals(123, instance.getId());
    }

    @Test
    public void testGetName() {
        MonitoredSystem instance = new MonitoredSystem("Name", Status.COMPLETE, 0, 123);
        assertEquals("Name", instance.getName());
    }

    @Test
    public void testGetSourceAddress() {
        MonitoredSystem instance = new MonitoredSystem("Name", Status.COMPLETE, 0, 123);
        assertEquals(0, instance.getSourceAddress());
    }

    @Test
    public void testGetStatus() {
        MonitoredSystem instance = new MonitoredSystem("Name", Status.COMPLETE, 0, 123);
        assertEquals(Status.COMPLETE, instance.getStatus());
    }

    @Test
    public void testNotEqualsByAddress() {
        MonitoredSystem instance1 = new MonitoredSystem("Name", Status.COMPLETE, 0, 123);
        MonitoredSystem instance2 = new MonitoredSystem("Name", Status.COMPLETE, 1, 123);
        assertFalse(instance1.equals(instance2));
        assertFalse(instance2.equals(instance1));
        assertEquals(-1, instance1.compareTo(instance2));
    }

    @Test
    public void testNotEqualsById() {
        MonitoredSystem instance1 = new MonitoredSystem("Name", Status.COMPLETE, 0, 123);
        MonitoredSystem instance2 = new MonitoredSystem("Name", Status.COMPLETE, 0, 456);
        assertFalse(instance1.equals(instance2));
        assertFalse(instance2.equals(instance1));
        assertEquals(-333, instance1.compareTo(instance2));
    }

    @Test
    public void testNotEqualsByName() {
        MonitoredSystem instance1 = new MonitoredSystem("Name1", Status.COMPLETE, 0, 123);
        MonitoredSystem instance2 = new MonitoredSystem("Name2", Status.COMPLETE, 0, 123);
        assertFalse(instance1.equals(instance2));
        assertFalse(instance2.equals(instance1));
        assertEquals(-1, instance1.compareTo(instance2));
    }

    @Test
    public void testNotEqualsByStatus() {
        MonitoredSystem instance1 = new MonitoredSystem("Name", Status.COMPLETE, 0, 123);
        MonitoredSystem instance2 = new MonitoredSystem("Name", Status.NOT_COMPLETE, 0, 123);
        assertFalse(instance1.equals(instance2));
        assertFalse(instance2.equals(instance1));
        assertEquals(-1, instance1.compareTo(instance2));
    }

    @Test
    public void testNotEqualsObject() {
        MonitoredSystem instance = new MonitoredSystem("Name", Status.COMPLETE, 0, 123);
        assertFalse(instance.equals("MonitoredSystem"));
    }

    @Test
    public void testToString() {
        MonitoredSystem instance = new MonitoredSystem("Name", Status.COMPLETE, 0, 123);
        assertEquals("Name     complete", instance.toString());
    }

}
