/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.bus.j1939.packets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.soliddesign.iumpr.bus.Packet;

/**
 * A Super class for Diagnostic Readiness Packets
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class DiagnosticReadinessPacket extends ParsedPacket {

    public DiagnosticReadinessPacket(Packet packet) {
        super(packet);
    }

    private MonitoredSystem createContinouslyMonitoredSystem(String name, int completedMask) {
        int supportedMask = completedMask >> 4;
        boolean notCompleted = (getByte(3) & completedMask) == completedMask;
        boolean supported = isOBDModule() && (getByte(3) & supportedMask) == supportedMask;
        MonitoredSystem.Status status = MonitoredSystem.Status.findStatus(notCompleted, supported);
        return new MonitoredSystem(name, status, getSourceAddress(), completedMask);
    }

    private MonitoredSystem createNonContinouslyMonitoredSystem(String name, int lowerByte, int mask) {
        boolean notCompleted = (getByte(lowerByte + 2) & mask) == mask;
        boolean supported = isOBDModule() && (getByte(lowerByte) & mask) == mask;
        MonitoredSystem.Status status = MonitoredSystem.Status.findStatus(notCompleted, supported);
        return new MonitoredSystem(name, status, getSourceAddress(), lowerByte << 8 | mask);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof DiagnosticReadinessPacket)) {
            return false;
        }

        DiagnosticReadinessPacket that = (DiagnosticReadinessPacket) obj;
        return getSourceAddress() == that.getSourceAddress()
                && Objects.equals(getContinuouslyMonitoredSystems(), that.getContinuouslyMonitoredSystems())
                && Objects.equals(getNonContinuouslyMonitoredSystems(), that.getNonContinuouslyMonitoredSystems());
    }

    /**
     * Returns the List of Continuously monitored systems
     *
     * @return {@link List}
     */
    public List<MonitoredSystem> getContinuouslyMonitoredSystems() {
        List<MonitoredSystem> systems = new ArrayList<>();
        systems.add(createContinouslyMonitoredSystem("Comprehensive component   ", 0x40));
        systems.add(createContinouslyMonitoredSystem("Fuel System               ", 0x20));
        systems.add(createContinouslyMonitoredSystem("Misfire                   ", 0x10));
        return systems;
    }

    /**
     * Returns the {@link Set} of Continuously and Non-continuously monitored
     * systems
     *
     * @return {@link Set}
     */
    public Set<MonitoredSystem> getMonitoredSystems() {
        Set<MonitoredSystem> set = new HashSet<>();
        set.addAll(getContinuouslyMonitoredSystems());
        set.addAll(getNonContinuouslyMonitoredSystems());
        return set;
    }

    /**
     * Returns the List of Non-continuously monitored systems
     *
     * @return {@link List}
     */
    public List<MonitoredSystem> getNonContinuouslyMonitoredSystems() {
        List<MonitoredSystem> systems = new ArrayList<>();
        systems.add(
                createNonContinouslyMonitoredSystem("EGR/VVT system            ", 4, 0x80));
        systems.add(
                createNonContinouslyMonitoredSystem("Exhaust Gas Sensor heater ", 4, 0x40));
        systems.add(
                createNonContinouslyMonitoredSystem("Exhaust Gas Sensor        ", 4, 0x20));
        systems.add(
                createNonContinouslyMonitoredSystem("A/C system refrigerant    ", 4, 0x10));
        systems.add(
                createNonContinouslyMonitoredSystem("Secondary air system      ", 4, 0x08));
        systems.add(
                createNonContinouslyMonitoredSystem("Evaporative system        ", 4, 0x04));
        systems.add(
                createNonContinouslyMonitoredSystem("Heated catalyst           ", 4, 0x02));
        systems.add(
                createNonContinouslyMonitoredSystem("Catalyst                  ", 4, 0x01));
        systems.add(
                createNonContinouslyMonitoredSystem("NMHC converting catalyst  ", 5, 0x10));
        systems.add(
                createNonContinouslyMonitoredSystem("NOx catalyst/adsorber     ", 5, 0x08));
        systems.add(
                createNonContinouslyMonitoredSystem("Diesel Particulate Filter ", 5, 0x04));
        systems.add(
                createNonContinouslyMonitoredSystem("Boost pressure control sys", 5, 0x02));
        systems.add(
                createNonContinouslyMonitoredSystem("Cold start aid system     ", 5, 0x01));

        return systems;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getContinuouslyMonitoredSystems(), getNonContinuouslyMonitoredSystems());
    }

    /**
     * Returns true if this module is an OBD Module that supports this function
     *
     * @return boolean
     */
    private boolean isOBDModule() {
        return getByte(2) != (byte) 0x05 && getByte(2) != (byte) 0xFF;
    }
}