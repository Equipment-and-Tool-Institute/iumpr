/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.bus.j1939.packets;

import net.soliddesign.iumpr.bus.Packet;

/**
 * Parses the Component Identification Packet
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class ComponentIdentificationPacket extends ParsedPacket {

    public static final int PGN = 65259;

    /**
     * Holds the different parts of the component identification:
     *
     * <pre>
     * 0 - Make
     * 1 - Model
     * 2 - Serial Number
     * 3 - Unit Number
     * </pre>
     */
    private String[] parts;

    /**
     * Constructor
     *
     * @param packet
     *            the {@link Packet} to parse
     */
    public ComponentIdentificationPacket(Packet packet) {
        super(packet);
    }

    /**
     * Returns the Make, never null
     *
     * @return String
     */
    public String getMake() {
        return getPart(0);
    }

    /**
     * Returns the Model, never null
     *
     * @return String
     */
    public String getModel() {
        return getPart(1);
    }

    @Override
    public String getName() {
        return "Component Identification";
    }

    /**
     * Helper method to make sure the packet is parsed on first use
     *
     * @param index
     *            the index of the part to return
     * @return String that corresponds to the part
     */
    private String getPart(int index) {
        if (parts == null) {
            parts = new String[4];
            parsePacket();
        }
        return parts[index];
    }

    /**
     * Returns the Serial Number, never null
     *
     * @return String
     */
    public String getSerialNumber() {
        return getPart(2);
    }

    /**
     * Returns the Unit Number, never null
     *
     * @return String
     */
    public String getUnitNumber() {
        return getPart(3);
    }

    /**
     * Parses the {@link Packet} into the parts
     */
    private void parsePacket() {
        String information = format(getPacket().getBytes());
        int beginIndex = 0;
        int endIndex = 0;
        for (int i = 0; i < 4; i++) {
            endIndex = information.indexOf("*", beginIndex);
            parts[i] = information.substring(beginIndex, endIndex).trim();
            beginIndex = endIndex + 1;
        }
    }

    @Override
    public String toString() {
        String result = getStringPrefix();
        result += "Make: " + getMake() + ", ";
        result += "Model: " + getModel() + ", ";
        result += "Serial Number: " + getSerialNumber() + ", ";
        result += "Unit Number: " + getUnitNumber();
        return result;
    }
}
