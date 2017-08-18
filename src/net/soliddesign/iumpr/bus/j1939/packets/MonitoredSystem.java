/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.bus.j1939.packets;

import java.util.Objects;

/**
 * The Name and Status of an Emission Monitored System (DM5 and DM26)
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class MonitoredSystem implements Comparable<MonitoredSystem> {

    /**
     * The Status of a Monitored System
     */
    public enum Status {
        // The order of these MUST remain Complete, Not Complete, Not Supported
        COMPLETE("    complete"), NOT_COMPLETE("not complete"), NOT_SUPPORTED("not enabled");
        public static Status findStatus(boolean notCompleted, boolean supported) {
            if (!supported) {
                return NOT_SUPPORTED;
            } else {
                return notCompleted ? NOT_COMPLETE : COMPLETE;
            }
        }

        private final String string;

        private Status(String string) {
            this.string = string;
        }

        @Override
        public String toString() {
            return string;
        }
    }

    private final int id;
    private final String name;
    private final int sourceAddress;
    private final Status status;

    /**
     * Creates a Monitored System
     *
     * @param name
     *            the Name of the Monitored System
     * @param status
     *            the {@link Status} of the Monitored System
     * @param sourceAddress
     *            the source address that reported this
     * @param id
     *            the unique id for this system. This allows
     *            {@link MonitoredSystem} from various source addresses to be
     *            matched up
     */
    public MonitoredSystem(String name, Status status, int sourceAddress, int id) {
        this.name = name;
        this.status = status;
        this.sourceAddress = sourceAddress;
        this.id = id;
    }

    @Override
    public int compareTo(MonitoredSystem o) {
        int result = getName().compareTo(o.getName());
        if (result == 0) {
            result = getSourceAddress() - o.getSourceAddress();
        }
        if (result == 0) {
            result = getStatus().compareTo(o.getStatus());
        }
        if (result == 0) {
            result = getId() - o.getId();
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof MonitoredSystem)) {
            return false;
        }

        MonitoredSystem that = (MonitoredSystem) obj;
        return Objects.equals(getName(), that.getName())
                && Objects.equals(getStatus(), that.getStatus())
                && Objects.equals(getSourceAddress(), that.getSourceAddress())
                && Objects.equals(getId(), that.getId());
    }

    /**
     * Returns the unique identification for the Monitored System. This allows
     * {@link MonitoredSystem} from various source addresses to be matched up
     *
     * @return int
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the name of the Monitored System
     *
     * @return {@link String}
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the source address of the module this {@link MonitoredSystem} is
     * from
     *
     * @return the sourceAddress
     */
    public int getSourceAddress() {
        return sourceAddress;
    }

    /**
     * Returns the {@link Status} of the Monitored System
     *
     * @return {@link Status}
     */
    public Status getStatus() {
        return status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getStatus(), getSourceAddress(), getId());
    }

    @Override
    public String toString() {
        return getName() + " " + getStatus();
    }
}
