/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.soliddesign.iumpr.NumberFormatter;
import net.soliddesign.iumpr.bus.Packet;
import net.soliddesign.iumpr.bus.j1939.J1939;
import net.soliddesign.iumpr.bus.j1939.Lookup;
import net.soliddesign.iumpr.bus.j1939.packets.CompositeMonitoredSystem;
import net.soliddesign.iumpr.bus.j1939.packets.DM20MonitorPerformanceRatioPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM21DiagnosticReadinessPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM26TripDiagnosticReadinessPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM5DiagnosticReadinessPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DiagnosticReadinessPacket;
import net.soliddesign.iumpr.bus.j1939.packets.MonitoredSystem;
import net.soliddesign.iumpr.bus.j1939.packets.MonitoredSystem.Status;
import net.soliddesign.iumpr.bus.j1939.packets.ParsedPacket;
import net.soliddesign.iumpr.bus.j1939.packets.PerformanceRatio;
import net.soliddesign.iumpr.controllers.ResultsListener;

/**
 * {@link FunctionalModule} that requests DM5, DM20, DM21, and DM26 messages
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class DiagnosticReadinessModule extends FunctionalModule {

    public static final int TSCC_GAP_LIMIT = 60; // minutes

    /**
     * Helper method to gather the given monitored systems into a vehicle
     * composite
     *
     * @param monitoredSystems
     *            the {@link MonitoredSystem}s to gather
     * @return a List of {@link MonitoredSystem}
     */
    public static List<CompositeMonitoredSystem> getCompositeSystems(Collection<MonitoredSystem> monitoredSystems) {
        Map<Integer, CompositeMonitoredSystem> map = new HashMap<>();
        for (MonitoredSystem system : monitoredSystems) {
            int id = system.getId();
            CompositeMonitoredSystem existingSystem = map.get(id);
            if (existingSystem == null) {
                map.put(id, new CompositeMonitoredSystem(system));
            } else {
                existingSystem.addMonitoredSystems(system);
            }
        }
        List<CompositeMonitoredSystem> systems = new ArrayList<>(map.values());
        Collections.sort(systems);
        return systems;
    }

    /**
     * Helper method to get the Number of Ignition Cycles from the packets. Only
     * the value from the engine controller is returned. -1 is returned if there
     * is no engine packet in the Collection.
     *
     * @param packets
     *            the {@link Collection} of
     *            {@link DM20MonitorPerformanceRatioPacket}
     * @return int
     */
    public static int getIgnitionCycles(Collection<DM20MonitorPerformanceRatioPacket> packets) {
        return packets.stream().filter(p -> p.getSourceAddress() == J1939.ENGINE_ADDR)
                .mapToInt(p -> p.getIgnitionCycles()).findFirst().orElse(-1);
    }

    /**
     * Helper method to get the maximum number of OBD Monitoring Conditions
     * Encountered from the packets. Only the value from the engine controller
     * is returned. -1 is returned if there is no engine packet in the
     * Collection.
     *
     * @param packets
     *            the {@link Collection} of
     *            {@link DM20MonitorPerformanceRatioPacket}
     * @return int
     */
    public static int getOBDCounts(Collection<DM20MonitorPerformanceRatioPacket> packets) {
        return packets.stream().filter(p -> p.getSourceAddress() == J1939.ENGINE_ADDR)
                .mapToInt(p -> p.getOBDConditionsCount()).findFirst().orElse(-1);
    }

    /**
     * Helper method to get the {@link Set} of {@link PerformanceRatio} from the
     * packets
     *
     * @param packets
     *            the {@link Collection} of
     *            {@link DM20MonitorPerformanceRatioPacket}
     *
     * @return {@link Set} of {@link PerformanceRatio}
     */
    public static Set<PerformanceRatio> getRatios(Collection<DM20MonitorPerformanceRatioPacket> packets) {
        return packets.stream().flatMap(t -> t.getRatios().stream()).collect(Collectors.toSet());
    }

    /**
     * Helper method to get the {@link Set} of {@link MonitoredSystem} from
     * {@link DM5DiagnosticReadinessPacket}
     *
     * @param packets
     *            the {@link Collection} of {@link DM5DiagnosticReadinessPacket}
     * @return {@link Set} of {@link MonitoredSystem}
     */
    public static Set<MonitoredSystem> getSystems(Collection<DM5DiagnosticReadinessPacket> packets) {
        return packets.stream().flatMap(t -> t.getMonitoredSystems().stream()).collect(Collectors.toSet());
    }

    /**
     * Constructor
     */
    public DiagnosticReadinessModule() {
        this(new DateTimeModule());
    }

    /**
     * Constructor exposed for testing
     *
     * @param dateTimeModule
     *            the {@link DateTimeModule} to use
     */
    public DiagnosticReadinessModule(DateTimeModule dateTimeModule) {
        super(dateTimeModule);
    }

    /**
     * Helper method to extract all the {@link MonitoredSystem}s given a
     * {@link List} of {@link DiagnosticReadinessPacket}s
     *
     * @param packets
     *            the Packets to parse
     * @return a {@link List} of {@link MonitoredSystem}s
     */
    private List<CompositeMonitoredSystem> getCompositeSystems(List<? extends DiagnosticReadinessPacket> packets) {
        return getCompositeSystems(packets.stream().flatMap(p -> p.getMonitoredSystems().stream())
                .collect(Collectors.toSet()));
    }

    /**
     * Sends a global request for DM20 Packets. The request and results will be
     * returned to the {@link ResultsListener}
     *
     * @param listener
     *            the {@link ResultsListener} for the results
     * @param fullString
     *            true to include the full string of the results in the report;
     *            false to only include the returned raw packet in the report
     * @return the {@link List} of {@link DM20MonitorPerformanceRatioPacket}s
     */
    public List<DM20MonitorPerformanceRatioPacket> getDM20Packets(ResultsListener listener, boolean fullString) {
        return getPackets("Global DM20 Request", DM20MonitorPerformanceRatioPacket.PGN,
                DM20MonitorPerformanceRatioPacket.class, listener, fullString);
    }

    /**
     * Sends a global request for DM21 Packets. The request and results will be
     * returned to the {@link ResultsListener}
     *
     * @param listener
     *            the {@link ResultsListener} for the results
     * @param fullString
     *            true to include the full string of the results in the report;
     *            false to only include the returned raw packet in the report
     * @return the {@link List} of {@link DM21DiagnosticReadinessPacket}s
     */
    public List<DM21DiagnosticReadinessPacket> getDM21Packets(ResultsListener listener, boolean fullString) {
        return getPackets("Global DM21 Request", DM21DiagnosticReadinessPacket.PGN, DM21DiagnosticReadinessPacket.class,
                listener, fullString);
    }

    /**
     * Sends a global request for DM26 Packets. The request and results will be
     * returned to the {@link ResultsListener}
     *
     * @param listener
     *            the {@link ResultsListener} for the results
     * @param fullString
     *            true to include the full string of the results in the report;
     *            false to only include the returned raw packet in the report
     * @return the {@link List} of {@link DM26TripDiagnosticReadinessPacket}s
     */
    public List<DM26TripDiagnosticReadinessPacket> getDM26Packets(ResultsListener listener, boolean fullString) {
        return getPackets("Global DM26 Request", DM26TripDiagnosticReadinessPacket.PGN,
                DM26TripDiagnosticReadinessPacket.class, listener, fullString);
    }

    /**
     * Sends a global request for DM5 Packets. The request and results will be
     * returned to the {@link ResultsListener}
     *
     * @param listener
     *            the {@link ResultsListener} for the results
     * @param fullString
     *            true to include the full string of the results in the report;
     *            false to only include the returned raw packet in the report
     * @return the {@link List} of {@link DM5DiagnosticReadinessPacket}s
     */
    public List<DM5DiagnosticReadinessPacket> getDM5Packets(ResultsListener listener, boolean fullString) {
        return getPackets("Global DM5 Request", DM5DiagnosticReadinessPacket.PGN, DM5DiagnosticReadinessPacket.class,
                listener, fullString);
    }

    /**
     * Helper method to find the longest {@link PerformanceRatio} name
     *
     * @param ratios
     *            all the {@link PerformanceRatio}s
     * @return the length of the longest name
     */
    private int getLongestName(Collection<PerformanceRatio> ratios) {
        return ratios.stream().mapToInt(t -> (t.getName()).length()).max().getAsInt();
    }

    /**
     * Helper method to find the longest {@link PerformanceRatio} source
     *
     * @param ratios
     *            all the {@link PerformanceRatio}s
     * @return the length of the longest source
     */
    private int getLongestSource(Collection<PerformanceRatio> ratios) {
        return ratios.stream().mapToInt(t -> (t.getSource()).length()).max().getAsInt();
    }

    /**
     * Sends the DM5 to determine which modules support HD-OBD. It returns a
     * {@link List} of source addresses of the modules that do support HD-OBD.
     *
     * @param listener
     *            the {@link ResultsListener} that is notified of the
     *            communications
     * @return List of source addresses
     */
    public List<Integer> getOBDModules(ResultsListener listener) {
        List<DM5DiagnosticReadinessPacket> packets = getDM5Packets(listener, false);
        Set<Integer> addressSet = packets.stream().filter(t -> t.isHdObd()).map(t -> t.getSourceAddress())
                .collect(Collectors.toSet());
        List<Integer> addresses = new ArrayList<>(addressSet);
        Collections.sort(addresses);
        if (addresses.isEmpty()) {
            listener.onResult("No modules report as HD-OBD compliant - stopping.");
        } else {
            for (int i : addresses) {
                listener.onResult(Lookup.getAddressName(i) + " reported as an HD-OBD Module.");
            }
        }
        return addresses;
    }

    /**
     * Helper method to request packets from the vehicle
     *
     * @param <T>
     *            The class of packets that will be returned
     * @param title
     *            the section title for inclusion in report
     * @param pgn
     *            the PGN that's being requested
     * @param clazz
     *            the {@link Class} of packet that will be returned
     * @param listener
     *            the {@link ResultsListener} that will be notified of the
     *            traffic
     * @param fullString
     *            true to include the full string of the results in the report;
     *            false to only include the returned raw packet in the report
     * @return the List of packets returned
     */
    private <T extends ParsedPacket> List<T> getPackets(String title, int pgn, Class<T> clazz, ResultsListener listener,
            boolean fullString) {
        Packet request = getJ1939().createRequestPacket(pgn, J1939.GLOBAL_ADDR);
        if (listener != null) {
            listener.onResult(getTime() + " " + title);
            listener.onResult(getTime() + " " + request.toString() + TX);
        }

        List<T> packets = getJ1939().requestMultiple(clazz, request).collect(Collectors.toList());

        if (listener != null) {
            if (packets.isEmpty()) {
                listener.onResult(TIMEOUT_MESSAGE);
            } else {
                for (ParsedPacket packet : packets) {
                    listener.onResult(getTime() + " " + packet.getPacket().toString());
                    if (fullString) {
                        listener.onResult(packet.toString());
                    }
                }
            }
        }
        return packets;
    }

    /**
     * Helper method to return the {@link Status} of the {@link MonitoredSystem}
     * padded with extra space on the right if necessary
     *
     * @param system
     *            the {@link MonitoredSystem} to pad
     * @return String with extra space on the right
     */
    private String getPaddedStatus(MonitoredSystem system) {
        return padRight(system.getStatus().toString(), 23);
    }

    /**
     * Pads the String with spaces on the left
     *
     * @param string
     *            the String to pad
     * @param length
     *            the maximum number of spaces
     * @return the padded string
     */
    private String padLeft(String string, int length) {
        return String.format("%1$" + length + "s", string);
    }

    /**
     * Pads the String with spaces on the right
     *
     * @param string
     *            the String to pad
     * @param length
     *            the maximum number of spaces
     * @return the padded string
     */
    private String padRight(String string, int length) {
        return String.format("%1$-" + length + "s", string);
    }

    /**
     * Requests all DM20s from all vehicle modules. The results are reported
     * back to the supplied listener
     *
     * @param listener
     *            the {@link ResultsListener} that will be notified of results
     * @return true if packets were received
     */
    public boolean reportDM20(ResultsListener listener) {
        List<DM20MonitorPerformanceRatioPacket> packets = getDM20Packets(listener, true);
        return !packets.isEmpty();
    }

    /**
     * Requests all DM21s from all vehicle modules. The results are reported
     * back to the supplied listener
     *
     * @param listener
     *            the {@link ResultsListener} that will be notified of results
     * @param lastTscc
     *            the last reported Time Since Code Cleared
     */
    public void reportDM21(ResultsListener listener, int lastTscc) {
        List<DM21DiagnosticReadinessPacket> packets = getDM21Packets(listener, true);
        int tscc = packets.stream().mapToInt(p -> ((int) p.getMinutesSinceDTCsCleared())).max().orElse(-1);
        if (tscc >= 0 && lastTscc >= 0) {
            int delta = tscc - lastTscc;
            if (delta < 0) {
                listener.onResult(getTime() + " ERROR Time Since Code Cleared Reset / Rollover");
            } else if (delta > TSCC_GAP_LIMIT) {
                listener.onResult(getTime() + " ERROR Excess Time Since Code Cleared Gap of " + delta + " minutes");
            } else {
                listener.onResult(getTime() + " Time Since Code Cleared Gap of " + delta + " minutes");
            }
        }
    }

    /**
     * Requests all DM26 from all vehicle modules. The compiles the
     * {@link MonitoredSystem} to include a vehicle composite of those systems
     * for the report. The results are reported back to the supplied listener
     *
     * @param listener
     *            the {@link ResultsListener} that will be notified of results
     * @return true if packets were received
     */
    public boolean reportDM26(ResultsListener listener) {
        List<DM26TripDiagnosticReadinessPacket> packets = getDM26Packets(listener, true);
        if (!packets.isEmpty()) {
            listener.onResult("");
            listener.onResult(getTime() + " Vehicle Composite of DM26:");
            List<CompositeMonitoredSystem> systems = getCompositeSystems(packets);
            listener.onResult(systems.stream().map(t -> t.toString()).collect(Collectors.toList()));
        }
        return !packets.isEmpty();
    }

    /**
     * Requests all DM5 from all vehicle modules. The compiles the
     * {@link MonitoredSystem} to include a vehicle composite of those systems
     * for the report. The results are reported back to the supplied listener
     *
     * @param listener
     *            the {@link ResultsListener} that will be notified of results
     * @return true if packets were received
     */
    public boolean reportDM5(ResultsListener listener) {
        List<DM5DiagnosticReadinessPacket> packets = getDM5Packets(listener, true);
        if (!packets.isEmpty()) {
            listener.onResult("");
            listener.onResult(getTime() + " Vehicle Composite of DM5:");
            List<CompositeMonitoredSystem> systems = getCompositeSystems(packets);
            listener.onResult(systems.stream().map(t -> t.toString()).collect(Collectors.toList()));
        }
        return !packets.isEmpty();
    }

    /**
     * Adds a report of the difference between the initial values and the final
     * values. The results are returned to the listener
     *
     * @param listener
     *            the {@link ResultsListener} that will be given the table
     * @param initialValues
     *            the {@link Collection} of {@link MonitoredSystem} that were
     *            gathered when the process started
     * @param finalValues
     *            the {@link Collection} of {@link MonitoredSystem} that were
     *            last gathered in the process
     * @param initialTime
     *            the formatted time when the process started
     * @param finalTime
     *            the formatted time when the process ended
     */
    public void reportMonitoredSystems(ResultsListener listener, Collection<MonitoredSystem> initialValues,
            Collection<MonitoredSystem> finalValues, String initialTime, String finalTime) {
        // By design the total number will always be the same. If they are not,
        // cash in your chips because you can't trust anything

        // These are sorted by Name
        List<CompositeMonitoredSystem> startSystems = getCompositeSystems(initialValues);
        List<CompositeMonitoredSystem> endSystems = getCompositeSystems(finalValues);

        listener.onResult(getTime() + " Vehicle Composite Results of DM5:");
        String separator = "+--------------------------------------------------------+-------------------------+--------------------------+";
        listener.onResult(separator);
        listener.onResult("| " + padLeft(padRight("Monitor", 30), 54)
                + " |     Initial Status      |       Last Status        |");
        listener.onResult("| " + padRight("", 54) + " | " + initialTime + " | " + finalTime + "  |");
        listener.onResult(separator);

        for (int i = 0; i < startSystems.size(); i++) {
            MonitoredSystem startSystem = startSystems.get(i);
            MonitoredSystem endSystem = endSystems.get(i);
            boolean diff = endSystem.getStatus() != startSystem.getStatus();
            listener.onResult("| " + startSystem.getName() + " | " + getPaddedStatus(startSystem) + " | "
                    + getPaddedStatus(endSystem) + (diff ? "*" : " ") + " |");
        }
        listener.onResult(separator);
    }

    /**
     * Reports the difference between the initial values and final values in a
     * table format. The results are returned to the listener.
     *
     * @param listener
     *            the {@link ResultsListener} that will be given the table
     * @param initialValues
     *            the {@link Collection} of {@link PerformanceRatio} that were
     *            gathered when the process started
     * @param finalValues
     *            the {@link Collection} of {@link PerformanceRatio} that were
     *            last gathered by the process
     * @param initialIgnitionCycles
     *            the initial value of the number of Ignition Cycles
     * @param finalIgnitionCycles
     *            the final value of the number of Ignition Cycles
     * @param initialObdCounts
     *            the initial value of the number of OBD Monitoring Conditions
     *            Encountered
     * @param finalObdCounts
     *            the final value of the number of OBD Monitoring Conditions
     *            Encountered
     * @param initialTime
     *            the formatted time when the process started
     * @param finalTime
     *            the formatted time when the process ended
     */
    public void reportPerformanceRatios(ResultsListener listener, Collection<PerformanceRatio> initialValues,
            Collection<PerformanceRatio> finalValues, int initialIgnitionCycles, int finalIgnitionCycles,
            int initialObdCounts, int finalObdCounts, String initialTime, String finalTime) {

        // Sorts the lists by Source then by Ratio Name
        Comparator<? super PerformanceRatio> comparator = (o1, o2) -> (o1.getSource() + " " + o1.getName())
                .compareTo(o2.getSource() + " " + o2.getName());

        List<PerformanceRatio> startingRatios = new ArrayList<>(initialValues);
        startingRatios.sort(comparator);

        List<PerformanceRatio> endingRatios = new ArrayList<>(finalValues);
        endingRatios.sort(comparator);

        int nameLen = getLongestName(initialValues);
        int srcLen = getLongestSource(initialValues);

        listener.onResult(getTime() + " Vehicle Composite Results of DM20:");

        // Make String of spaces for the Source Column
        String sourceSpace = padRight("", srcLen);

        // Make a String of spaces for the Name Column
        String nameSpace = padRight("", nameLen);

        String separator1 = ("+ " + sourceSpace + " + " + nameSpace
                + " +---------------------------+----------------------------+").replaceAll(" ", "-");

        String separator2 = ("+ " + sourceSpace + " + " + nameSpace
                + " +-------------+-------------+-------------+--------------+").replaceAll(" ", "-");

        listener.onResult(separator1);
        listener.onResult(
                "| " + sourceSpace + " | " + nameSpace + " |      Initial Status       |         Last Status        |");
        listener.onResult("| " + sourceSpace + " | " + nameSpace + " |  " + initialTime + "  |  " + finalTime + "   |");
        listener.onResult(separator1);

        boolean diff = initialIgnitionCycles != finalIgnitionCycles;
        listener.onResult("| " + sourceSpace + " | " + padRight("Ignition Cycles", nameLen) + " |  "
                + padLeft("" + initialIgnitionCycles, 24) + " |  " + padLeft("" + finalIgnitionCycles, 24)
                + (diff ? "*" : " ") + " |");

        diff = initialObdCounts != finalObdCounts;
        listener.onResult("| " + sourceSpace + " | " + padRight("OBD Monitoring Conditions Encountered", nameLen)
                + " |  " + padLeft("" + initialObdCounts, 24) + " |  " + padLeft("" + finalObdCounts, 24)
                + (diff ? "*" : " ") + " |");

        listener.onResult(separator2);
        listener.onResult("| " + padRight("Source", srcLen) + " | " + padRight("Monitor Name", nameLen)
                + " | " + padLeft("Numerator", 11) + " | " + padLeft("Denominator", 11) + " | "
                + padLeft("Numerator", 10) + "  | " + padLeft("Denominator", 11) + "  |");
        listener.onResult(separator2);

        for (PerformanceRatio ratio1 : startingRatios) {
            PerformanceRatio ratio2 = null;
            for (int j = 0; j < endingRatios.size(); j++) {
                if (ratio1.getId() == endingRatios.get(j).getId()) {
                    ratio2 = endingRatios.remove(j);
                    break;
                }
            }

            String name = ratio1.getName();
            String source = ratio1.getSource();
            int num1 = ratio1.getNumerator();
            int dem1 = ratio1.getDenominator();
            int num2 = ratio2 != null ? ratio2.getNumerator() : -1;
            int dem2 = ratio2 != null ? ratio2.getDenominator() : -1;

            listener.onResult(rowForDM20(srcLen, source, nameLen, name, num1, dem1, num2, dem2));
        }

        for (PerformanceRatio ratio2 : endingRatios) {
            String name = ratio2.getName();
            String source = ratio2.getSource();
            int num1 = -1;
            int dem1 = -1;
            int num2 = ratio2.getNumerator();
            int dem2 = ratio2.getDenominator();

            listener.onResult(rowForDM20(srcLen, source, nameLen, name, num1, dem1, num2, dem2));
        }

        listener.onResult(separator2);
    }

    private String rowForDM20(int sourceLength, String source, int nameLength, String name, Integer initialNumerator,
            Integer initialDenominator, Integer finalNumerator, Integer finalDenominator) {

        String iNum = initialNumerator == null ? "" : NumberFormatter.format(initialNumerator);
        String iDem = initialDenominator == null ? "" : NumberFormatter.format(initialDenominator);
        String fNum = finalNumerator == null ? "" : NumberFormatter.format(finalNumerator);
        String fDem = finalDenominator == null ? "" : NumberFormatter.format(finalDenominator);

        boolean nDiff = !iNum.equals(fNum);
        boolean dDiff = !iDem.equals(fDem);

        return "| " + padRight(source, sourceLength) + " | " + padRight(name, nameLength) + " | "
                + padLeft(iNum, 11) + " | " + padLeft(iDem, 11) + " | "
                + padLeft(fNum, 10) + (nDiff ? "*" : " ") + " | " + padLeft(fDem, 11)
                + (dDiff ? "*" : " ") + " |";
    }

}
