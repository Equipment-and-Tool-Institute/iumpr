/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.bus.j1939.packets;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests the {@link LampStatus} enum
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class LampStatusTest {

    @Test
    public void testFastFlash() {
        assertEquals(LampStatus.FAST_FLASH, LampStatus.getStatus(1, 1));
    }

    @Test
    public void testOff() {
        assertEquals(LampStatus.OFF, LampStatus.getStatus(0, 0));
        assertEquals(LampStatus.OFF, LampStatus.getStatus(0, 1));
        assertEquals(LampStatus.OFF, LampStatus.getStatus(0, 2));
        assertEquals(LampStatus.OFF, LampStatus.getStatus(0, 3));
    }

    @Test
    public void testOn() {
        assertEquals(LampStatus.ON, LampStatus.getStatus(1, 3));
    }

    @Test
    public void testOther() {
        assertEquals(LampStatus.OTHER, LampStatus.getStatus(1, 2));
        assertEquals(LampStatus.OTHER, LampStatus.getStatus(2, 0));
        assertEquals(LampStatus.OTHER, LampStatus.getStatus(2, 1));
        assertEquals(LampStatus.OTHER, LampStatus.getStatus(2, 2));
        assertEquals(LampStatus.OTHER, LampStatus.getStatus(2, 3));
        assertEquals(LampStatus.OTHER, LampStatus.getStatus(3, 0));
        assertEquals(LampStatus.OTHER, LampStatus.getStatus(3, 1));
        assertEquals(LampStatus.OTHER, LampStatus.getStatus(3, 2));
        assertEquals(LampStatus.OTHER, LampStatus.getStatus(3, 3));
    }

    @Test
    public void testSlowFlash() {
        assertEquals(LampStatus.SLOW_FLASH, LampStatus.getStatus(1, 0));
    }

    @Test
    public void testToString() {
        assertEquals("Off", LampStatus.OFF.toString());
        assertEquals("On", LampStatus.ON.toString());
        assertEquals("Fast Flash", LampStatus.FAST_FLASH.toString());
        assertEquals("Slow Flash", LampStatus.SLOW_FLASH.toString());
    }

    @Test
    public void testValueOf() {
        assertEquals(LampStatus.OFF, LampStatus.valueOf("OFF"));
        assertEquals(LampStatus.ON, LampStatus.valueOf("ON"));
        assertEquals(LampStatus.FAST_FLASH, LampStatus.valueOf("FAST_FLASH"));
        assertEquals(LampStatus.SLOW_FLASH, LampStatus.valueOf("SLOW_FLASH"));
        assertEquals(LampStatus.OTHER, LampStatus.valueOf("OTHER"));
    }

    @Test
    public void testValues() {
        assertEquals(5, LampStatus.values().length);
    }

}
