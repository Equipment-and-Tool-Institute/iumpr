/**
 * Copyright 2017 Equipment & Tool Institute
 */
package org.etools.j1939tools.bus.j1939.packets;

import static org.junit.Assert.assertEquals;

import org.etools.j1939tools.bus.Packet;
import org.etools.j1939tools.bus.j1939.packets.DM28PermanentEmissionDTCPacket;
import org.junit.Test;

/**
 * Unit tests the {@link DM28PermanentEmissionDTCPacket} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class DM28PermanentEmissionDTCPacketTest {

    @Test
    public void testGetName() {
        Packet packet = Packet.create(0, 0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00);
        assertEquals("DM28", new DM28PermanentEmissionDTCPacket(packet).getName());
    }

    @Test
    public void testPGN() {
        assertEquals(64896, DM28PermanentEmissionDTCPacket.PGN);
    }

}
