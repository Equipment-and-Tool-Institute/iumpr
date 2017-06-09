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
 * Unit tests for the {@link CompositeMonitoredSystem} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class CompositeMonitoredSystemTest {

    @Test
    public void testEqualsHashCode() {
        CompositeMonitoredSystem instance1 = new CompositeMonitoredSystem("Name", 0, 123);
        CompositeMonitoredSystem instance2 = new CompositeMonitoredSystem("Name", 0, 123);
        assertTrue(instance1.equals(instance2));
        assertTrue(instance2.equals(instance1));
        assertTrue(instance1.hashCode() == instance2.hashCode());
    }

    @Test
    public void testEqualsHashCodeSelf() {
        CompositeMonitoredSystem instance = new CompositeMonitoredSystem("Name", 0, 123);
        assertTrue(instance.equals(instance));
        assertTrue(instance.hashCode() == instance.hashCode());
    }

    @Test
    public void testGetId() {
        CompositeMonitoredSystem instance = new CompositeMonitoredSystem("Name", 0, 123);
        assertEquals(123, instance.getId());
    }

    @Test
    public void testGetName() {
        CompositeMonitoredSystem instance = new CompositeMonitoredSystem("Name", 0, 123);
        assertEquals("Name", instance.getName());
    }

    @Test
    public void testGetSourceAddress() {
        CompositeMonitoredSystem instance = new CompositeMonitoredSystem("Name", 0, 123);
        assertEquals(0, instance.getSourceAddress());
    }

    @Test
    public void testGetStatus() {
        CompositeMonitoredSystem instance = new CompositeMonitoredSystem("Name", 0, 123);
        assertEquals(null, instance.getStatus());
    }

    @Test
    public void testGetStatusComplete() {
        MonitoredSystem system1 = new MonitoredSystem("System", Status.COMPLETE, 1, 123);
        MonitoredSystem system2 = new MonitoredSystem("System", Status.COMPLETE, 2, 123);
        MonitoredSystem system3 = new MonitoredSystem("System", Status.NOT_SUPPORTED, 3, 123);
        CompositeMonitoredSystem instance = new CompositeMonitoredSystem(system1);
        instance.addMonitoredSystems(system2);
        instance.addMonitoredSystems(system3);
        assertEquals(Status.COMPLETE, instance.getStatus());
    }

    @Test
    public void testGetStatusNotComplete() {
        MonitoredSystem system1 = new MonitoredSystem("System", Status.COMPLETE, 1, 123);
        MonitoredSystem system2 = new MonitoredSystem("System", Status.NOT_COMPLETE, 2, 123);
        MonitoredSystem system3 = new MonitoredSystem("System", Status.NOT_SUPPORTED, 3, 123);
        CompositeMonitoredSystem instance = new CompositeMonitoredSystem(system1);
        instance.addMonitoredSystems(system2);
        instance.addMonitoredSystems(system3);
        assertEquals(Status.NOT_COMPLETE, instance.getStatus());
    }

    @Test
    public void testGetStatusNotMonitored() {
        MonitoredSystem system1 = new MonitoredSystem("System", Status.NOT_SUPPORTED, 1, 123);
        MonitoredSystem system2 = new MonitoredSystem("System", Status.NOT_SUPPORTED, 2, 123);
        MonitoredSystem system3 = new MonitoredSystem("System", Status.NOT_SUPPORTED, 3, 123);
        CompositeMonitoredSystem instance = new CompositeMonitoredSystem(system1);
        instance.addMonitoredSystems(system2);
        instance.addMonitoredSystems(system3);
        assertEquals(Status.NOT_SUPPORTED, instance.getStatus());
    }

    @Test
    public void testNotEqualsByAddress() {
        CompositeMonitoredSystem instance1 = new CompositeMonitoredSystem("Name", 0, 123);
        CompositeMonitoredSystem instance2 = new CompositeMonitoredSystem("Name", 1, 123);
        assertFalse(instance1.equals(instance2));
        assertFalse(instance2.equals(instance1));
    }

    @Test
    public void testNotEqualsById() {
        CompositeMonitoredSystem instance1 = new CompositeMonitoredSystem("Name", 0, 123);
        CompositeMonitoredSystem instance2 = new CompositeMonitoredSystem("Name", 0, 456);
        assertFalse(instance1.equals(instance2));
        assertFalse(instance2.equals(instance1));
    }

    @Test
    public void testNotEqualsByName() {
        CompositeMonitoredSystem instance1 = new CompositeMonitoredSystem("Name1", 0, 123);
        CompositeMonitoredSystem instance2 = new CompositeMonitoredSystem("Name2", 0, 123);
        assertFalse(instance1.equals(instance2));
        assertFalse(instance2.equals(instance1));
    }

    @Test
    public void testNotEqualsByStatus() {
        MonitoredSystem system1 = new MonitoredSystem("System", Status.COMPLETE, 1, 123);
        CompositeMonitoredSystem instance1 = new CompositeMonitoredSystem(system1);
        MonitoredSystem system2 = new MonitoredSystem("System", Status.NOT_COMPLETE, 1, 123);
        CompositeMonitoredSystem instance2 = new CompositeMonitoredSystem(system2);
        assertFalse(instance1.equals(instance2));
        assertFalse(instance2.equals(instance1));
    }

    @Test
    public void testNotEqualsObject() {
        CompositeMonitoredSystem instance = new CompositeMonitoredSystem("Name", 0, 123);
        assertFalse(instance.equals("CompositeMonitoredSystem"));
    }

    @Test
    public void testToString() {
        MonitoredSystem system1 = new MonitoredSystem("System", Status.COMPLETE, 1, 123);
        CompositeMonitoredSystem instance = new CompositeMonitoredSystem(system1);
        assertEquals("System     complete", instance.toString());
    }

}
