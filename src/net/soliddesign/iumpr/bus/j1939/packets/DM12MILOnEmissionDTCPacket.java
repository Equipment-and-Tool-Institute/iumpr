/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.bus.j1939.packets;

import net.soliddesign.iumpr.bus.Packet;

/**
 * The {@link ParsedPacket} for Emission-Related Malfunction Indicator Lamp On
 * Diagnostic Trouble Codes (DM12)
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class DM12MILOnEmissionDTCPacket extends DiagnosticTroubleCodePacket {
    public static final int PGN = 65236;

    public DM12MILOnEmissionDTCPacket(Packet packet) {
        super(packet);
    }

    @Override
    public String getName() {
        return "DM12";
    }

}
