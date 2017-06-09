/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.bus.j1939.packets;

import net.soliddesign.iumpr.bus.j1939.Lookup;

/**
 * Class that contains the data about Supported SPNs
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class SupportedSPN {
    /**
     * Parses the data to return the SPN
     *
     * @param data
     *            the data to parse
     * @return the SPN
     */
    public static int parseSPN(int[] data) {
        // Byte: 0 bits 8-1 SPN, 8 least significant bits of SPN (most
        // significant at bit 8)
        // Byte: 1 bits 8-1 SPN, second byte of SPN (most significant at bit 8)
        // Byte: 2 bits 8-6 SPN, 3 most significant bits (most significant at
        // bit 8)
        return (((data[2] & 0xE0) << 11) & 0xFF0000) | ((data[1] << 8) & 0xFF00) | (data[0] & 0xFF);
    }

    private final byte length;

    private final int spn;

    private final int support;

    /**
     * Constructor
     *
     * @param data
     *            the data that contains the information
     */
    SupportedSPN(int[] data) {
        support = data[2] & 0x07;
        spn = SupportedSPN.parseSPN(data);
        length = (byte) (data[3] & 0xFF);
    }

    /**
     * Returns the length of the support data
     *
     * @return byte
     */
    public byte getLength() {
        return length;
    }

    /**
     * Returns the Suspect Parameter Number
     *
     * @return int
     */
    public int getSpn() {
        return spn;
    }

    /**
     * Returns true if Data Stream is supported
     *
     * @return boolean
     */
    public boolean supportsDataStream() {
        return (support & 0x02) == 0x00;
    }

    /**
     * Returns true if the Expanded Freeze Frame is supported
     *
     * @return boolean
     */
    public boolean supportsExpandedFreezeFrame() {
        return (support & 0x01) == 0x00;
    }

    /**
     * Returns true if Scaled Test Results are supported
     *
     * @return boolean
     */
    public boolean supportsScaledTestResults() {
        return (support & 0x04) == 0x00;
    }

    @Override
    public String toString() {
        String result = "SPN " + getSpn() + " - " + Lookup.getSpnName(getSpn()) + ":";

        if (support != 0x07) {
            result += " Supports";
        }
        boolean supported = false;
        if (supportsDataStream()) {
            result += " Data Stream";
            supported = true;
        }
        if (supportsExpandedFreezeFrame()) {
            result += supported ? "," : "";
            result += " Expanded Freeze Frame";
            supported = true;
        }
        if (supportsScaledTestResults()) {
            result += supported ? "," : "";
            result += " Scaled Test Results";
        }
        result += " with data length " + ParsedPacket.getValueWithUnits(getLength(), "bytes");
        return result;
    }
}
