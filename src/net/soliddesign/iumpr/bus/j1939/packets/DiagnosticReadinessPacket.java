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
        boolean supported = (getByte(3) & supportedMask) == supportedMask;
        MonitoredSystem.Status status = MonitoredSystem.Status.findStatus(notCompleted, supported);
        return new MonitoredSystem(name, status, getSourceAddress(), completedMask);
    }

    private MonitoredSystem createNonContinouslyMonitoredSystem(String name, int lowerByte, int mask) {
        boolean notCompleted = (getByte(lowerByte + 2) & mask) == mask;
        boolean supported = (getByte(lowerByte) & mask) == mask;
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
        systems.add(createContinouslyMonitoredSystem("Comprehensive component monitoring                    ", 0x40));
        systems.add(createContinouslyMonitoredSystem("Fuel System monitoring                                ", 0x20));
        systems.add(createContinouslyMonitoredSystem("Misfire monitoring                                    ", 0x10));
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
                createNonContinouslyMonitoredSystem("EGR/VVT system monitoring                             ", 4, 0x80));
        systems.add(
                createNonContinouslyMonitoredSystem("Exhaust Gas Sensor heater monitoring                  ", 4, 0x40));
        systems.add(
                createNonContinouslyMonitoredSystem("Exhaust Gas Sensor monitoring                         ", 4, 0x20));
        systems.add(
                createNonContinouslyMonitoredSystem("A/C system refrigerant monitoring                     ", 4, 0x10));
        systems.add(
                createNonContinouslyMonitoredSystem("Secondary air system monitoring                       ", 4, 0x08));
        systems.add(
                createNonContinouslyMonitoredSystem("Evaporative system monitoring                         ", 4, 0x04));
        systems.add(
                createNonContinouslyMonitoredSystem("Heated catalyst monitoring                            ", 4, 0x02));
        systems.add(
                createNonContinouslyMonitoredSystem("Catalyst monitoring                                   ", 4, 0x01));

        systems.add(
                createNonContinouslyMonitoredSystem("NMHC converting catalyst monitoring                   ", 5, 0x10));
        systems.add(
                createNonContinouslyMonitoredSystem("NOx converting catalyst and/or NOx adsorber monitoring", 5, 0x08));
        systems.add(
                createNonContinouslyMonitoredSystem("Diesel Particulate Filter (DPF) monitoring            ", 5, 0x04));
        systems.add(
                createNonContinouslyMonitoredSystem("Boost pressure control system monitoring              ", 5, 0x02));
        systems.add(
                createNonContinouslyMonitoredSystem("Cold start aid system monitoring                      ", 5, 0x01));

        return systems;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getContinuouslyMonitoredSystems(), getNonContinuouslyMonitoredSystems());
    }
}