/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.bus.j1939.packets;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import net.soliddesign.iumpr.bus.j1939.packets.MonitoredSystem.Status;

/**
 * Unit tests the {@link MonitoredSystem} {@link Status} enum
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class MonitoredSystemStatusTest {

    @Test
    public void testToString() {
        assertEquals("    complete", Status.COMPLETE.toString());
        assertEquals("not complete", Status.NOT_COMPLETE.toString());
        assertEquals("not enabled", Status.NOT_SUPPORTED.toString());
    }

    @Test
    public void testValueOf() {
        assertEquals(Status.COMPLETE, Status.valueOf("COMPLETE"));
        assertEquals(Status.NOT_COMPLETE, Status.valueOf("NOT_COMPLETE"));
        assertEquals(Status.NOT_SUPPORTED, Status.valueOf("NOT_SUPPORTED"));
    }

    @Test
    public void testValues() {
        assertEquals(3, Status.values().length);
    }
}
