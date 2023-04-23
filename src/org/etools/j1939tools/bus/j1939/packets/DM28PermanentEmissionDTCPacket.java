/**
 * Copyright 2017 Equipment & Tool Institute
 */
package org.etools.j1939tools.bus.j1939.packets;

import org.etools.j1939tools.bus.Packet;

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
