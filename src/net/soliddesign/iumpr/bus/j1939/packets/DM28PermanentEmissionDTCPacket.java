/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.bus.j1939.packets;

import net.soliddesign.iumpr.bus.Packet;

/**
 * The {@link ParsedPacket} for Emission-Related Permanent Diagnostic Trouble
 * Codes (DM28)
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class DM28PermanentEmissionDTCPacket extends DiagnosticTroubleCodePacket {
    public static final int PGN = 64896;

    public DM28PermanentEmissionDTCPacket(Packet packet) {
        super(packet);
    }

    @Override
    public String getName() {
        return "DM28";
    }

}
