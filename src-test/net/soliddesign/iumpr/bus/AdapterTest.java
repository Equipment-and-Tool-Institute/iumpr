/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.bus;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for the {@link Adapter} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class AdapterTest {

    @Test
    public void test() {
        Adapter instance = new Adapter("Name", "DLL", (short) 55);
        assertEquals("Name", instance.getName());
        assertEquals("DLL", instance.getDLLName());
        assertEquals((short) 55, instance.getDeviceId());
    }
}
