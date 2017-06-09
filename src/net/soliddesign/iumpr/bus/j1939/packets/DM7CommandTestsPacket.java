/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.bus.j1939.packets;

import net.soliddesign.iumpr.bus.Packet;

/**
 * The DM7 Packet. This isn't used to parse any packets as it will only be sent
 * to the vehicle. Responses will be {@link DM30ScaledTestResultsPacket}
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class DM7CommandTestsPacket extends ParsedPacket {

    public static final int PGN = 58112;

    public DM7CommandTestsPacket(Packet packet) {
        super(packet);
    }

    @Override
    public String getName() {
        return "DM7";
    }

}
