/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.bus.j1939.packets;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is a {@link MonitoredSystem} that is a composite of all
 * {@link MonitoredSystem} s from various sources
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class CompositeMonitoredSystem extends MonitoredSystem {

    /**
     * The composite {@link Status}
     */
    private Status status;

    /**
     * The Map of source address to {@link MonitoredSystem}
     */
    private final Map<Integer, Status> systems = new ConcurrentHashMap<>();

    /**
     * Creates a {@link CompositeMonitoredSystem} starting with the given
     * {@link MonitoredSystem}
     *
     * @param system
     *            the {@link MonitoredSystem} to start with
     */
    public CompositeMonitoredSystem(MonitoredSystem system) {
        super(system.getName(), system.getStatus(), system.getSourceAddress(), system.getId());
        addMonitoredSystems(system);
    }

    /**
     * Creates a {@link CompositeMonitoredSystem} with a name and id
     *
     * @param name
     *            the name
     * @param sourceAddress
     *            the source address of the module this is from
     * @param id
     *            the id
     */
    public CompositeMonitoredSystem(String name, int sourceAddress, int id) {
        super(name, null, sourceAddress, id);
    }

    /**
     * Adds a {@link MonitoredSystem} to the collection
     *
     * @param system
     *            the {@link MonitoredSystem} to add
     * @return true if the {@link Status} changed; false if it didn't change
     */
    public boolean addMonitoredSystems(MonitoredSystem system) {
        systems.put(system.getSourceAddress(), system.getStatus());

        boolean result = false;
        Status newStatus = getCompositeStatus();
        result = newStatus != status;
        status = newStatus;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    /**
     * Helper method to determine the {@link Status} based upon all the
     * {@link MonitoredSystem} s
     *
     * @return {@link Status}
     */
    private Status getCompositeStatus() {
        if (systems.isEmpty()) {
            return Status.NOT_SUPPORTED;
        }

        boolean hasComplete = false;

        for (Status systemStatus : systems.values()) {
            if (systemStatus == Status.NOT_COMPLETE) {
                return Status.NOT_COMPLETE;
            }
            if (systemStatus == Status.COMPLETE) {
                hasComplete = true;
            }
        }
        return hasComplete ? Status.COMPLETE : Status.NOT_SUPPORTED;
    }

    /**
     * Returns the composite {@link Status}
     *
     * @return {@link Status}
     */
    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
