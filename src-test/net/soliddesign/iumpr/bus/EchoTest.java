/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.bus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import net.soliddesign.iumpr.bus.j1939.J1939;
import net.soliddesign.iumpr.bus.j1939.packets.VehicleIdentificationPacket;
import net.soliddesign.iumpr.bus.simulated.Sim;

public class EchoTest {

    @Test
    public void failVin() throws BusException {
        Bus bus = new EchoBus(0xF9);
        assertFalse(new J1939(bus).requestMultiple(VehicleIdentificationPacket.class).findFirst().isPresent());
    }

    @Test
    public void getVin() throws BusException {
        Bus bus = new EchoBus(0xF9);
        final String VIN = "SOME VIN";
        try (Sim sim = new Sim(bus)) {
            sim.response(p -> (p.getId() & 0xFF00) == 0xEA00 && p.get24(0) == 65260,
                    () -> Packet.create(65260, 0x0, VIN.getBytes()));

            assertEquals(VIN, new J1939(bus).requestMultiple(VehicleIdentificationPacket.class).findFirst()
                    .map(p1 -> new String(p1.getPacket().getBytes())).get());
        }
    }
}
