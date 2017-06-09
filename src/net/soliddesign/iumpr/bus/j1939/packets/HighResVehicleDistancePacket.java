/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.bus.j1939.packets;

import net.soliddesign.iumpr.bus.Packet;

/**
 * The {@link ParsedPacket} responsible for translating Total Vehicle Distance
 * (High Resolution) (SPN 917)
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class HighResVehicleDistancePacket extends ParsedPacket {

    public static final int PGN = 65217;

    private final double distance;

    public HighResVehicleDistancePacket(Packet packet) {
        super(packet);
        distance = getScaledIntValue(0, 200);
    }

    @Override
    public String getName() {
        return "High Resolution Vehicle Distance";
    }

    /**
     * Returns the Total Vehicle Distance in kilometers
     *
     * @return the vehicle distance in km
     */
    public double getTotalVehicleDistance() {
        return distance;
    }

    /**
     * Returns the Total Vehicle Distance in miles
     *
     * @return the vehicle distance in miles
     */
    public double getTotalVehicleDistanceAsMiles() {
        return getTotalVehicleDistance() * KM_TO_MILES_FACTOR;
    }

    private String getVehicleDistanceAsString() {
        return getValuesWithUnits(getTotalVehicleDistance(), "km", getTotalVehicleDistanceAsMiles(), "mi");
    }

    @Override
    public String toString() {
        return getStringPrefix() + getVehicleDistanceAsString();
    }

}
