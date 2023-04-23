/**
 * Copyright 2017 Equipment & Tool Institute
 */
package org.etools.j1939tools.bus.j1939.packets;

import static net.soliddesign.iumpr.IUMPR.NL;

import java.util.ArrayList;
import java.util.List;

import org.etools.j1939tools.bus.Packet;

/**
 * Parses the SPN Support (DM24) Packet
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class DM24SPNSupportPacket extends ParsedPacket {

    public static final int PGN = 64950;

    private List<SupportedSPN> spns;

    public DM24SPNSupportPacket(Packet packet) {
        super(packet);
    }

    @Override
    public String getName() {
        return "DM24";
    }

    /**
     * Returns the {@link List} of {@link SupportedSPN}
     *
     * @return {@link List}
     */
    public List<SupportedSPN> getSupportedSpns() {
        if (spns == null) {
            spns = new ArrayList<>();
            parsePacket();
        }
        return spns;
    }

    /**
     * Parses the packet to populate all the {@link SupportedSPN}s
     */
    private void parsePacket() {
        final int length = getPacket().getLength();
        for (int i = 0; i + 3 < length; i = i + 4) {
            final SupportedSPN parsedSpn = parseSpn(i);
            if (parsedSpn.getSpn() != 0) {
                spns.add(parsedSpn);
            }
        }
    }

    /**
     * Parses a portion of the packet to create a {@link SupportedSPN}
     *
     * @param bytes
     *            the bytes to parse
     * @param index
     *            the index at which the parsing starts
     * @return a {@link SupportedSPN}
     */
    private SupportedSPN parseSpn(int index) {
        int[] data = getPacket().getData(index, index + 4);
        return new SupportedSPN(data);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getStringPrefix());
        sb.append("(Supporting Scaled Test Results) [" + NL);
        for (SupportedSPN spn : getSupportedSpns()) {
            if (spn.supportsScaledTestResults()) {
                sb.append("  ").append(spn).append(NL);
            }
        }
        sb.append("]");
        return sb.toString();
    }

}
