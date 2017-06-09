/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.bus.j1939.packets;

import net.soliddesign.iumpr.bus.Packet;

/**
 * The {@link ParsedPacket} for the Emission Related Pending Diagnostic Trouble
 * Codes (DM6)
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class DM6PendingEmissionDTCPacket extends DiagnosticTroubleCodePacket {
    public static final int PGN = 65231;

    public DM6PendingEmissionDTCPacket(Packet packet) {
        super(packet);
    }

    @Override
    public String getName() {
        return "DM6";
    }

}
