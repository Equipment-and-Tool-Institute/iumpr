/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import net.soliddesign.iumpr.bus.Packet;
import net.soliddesign.iumpr.bus.j1939.Lookup;
import net.soliddesign.iumpr.bus.j1939.packets.DM24SPNSupportPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM30ScaledTestResultsPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM7CommandTestsPacket;
import net.soliddesign.iumpr.bus.j1939.packets.ScaledTestResult;
import net.soliddesign.iumpr.bus.j1939.packets.ScaledTestResult.TestResult;
import net.soliddesign.iumpr.controllers.ResultsListener;

/**
 * The {@link FunctionalModule} that collects the Scaled Test Results from the
 * OBD Modules
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class OBDTestsModule extends FunctionalModule {

    /**
     * Constructor
     */
    public OBDTestsModule() {
        this(new DateTimeModule());
    }

    /**
     * Constructor exposed for testing
     *
     * @param dateTimeModule
     *            the {@link DateTimeModule} to use
     */
    public OBDTestsModule(DateTimeModule dateTimeModule) {
        super(dateTimeModule);
    }

    /**
     * Helper method to create a DM7 packet with Test ID of 247, FMI 31 and the
     * given SPN. The request will be sent to the specific destination
     *
     * @param destination
     *            the destination address for the packet
     * @param spn
     *            the SPN
     * @return Packet
     */
    private Packet createDM7Packet(int destination, int spn) {
        return Packet.create(DM7CommandTestsPacket.PGN | destination, getJ1939().getBusAddress(), 247, spn & 0xFF,
                (spn >> 8) & 0xFF, (((spn >> 16) & 0xFF) << 5) | 31, 0xFF, 0xFF, 0xFF, 0xFF);
    }

    /**
     * Queries the vehicle to get all Scaled Tests Results and reports then back
     * to the listener
     *
     * @param listener
     *            the {@link ResultsListener}
     * @param obdModules
     *            the {@link List} of addresses for ODB Modules
     */
    public void reportOBDTests(ResultsListener listener, List<Integer> obdModules) {
        Map<Integer, List<ScaledTestResult>> allTestResults = new HashMap<>();
        for (DM24SPNSupportPacket packet : requestSupportedSpnPackets(listener, obdModules)) {
            int destination = packet.getSourceAddress();
            String moduleName = Lookup.getAddressName(destination);
            // Find tests that support scaled results, remove duplicates and use
            // a predictable order for testing.
            List<Integer> spns = packet.getSupportedSpns().stream()
                    .filter(t -> t.supportsScaledTestResults()).map(s -> s.getSpn()).sorted().distinct()
                    .collect(Collectors.toList());
            if (spns.isEmpty()) {
                listener.onResult("ERROR " + moduleName + " does not have any tests that support scaled tests results");
            } else {
                List<ScaledTestResult> testResults = requestScaledTestResultsFromModule(listener, destination,
                        moduleName, spns);
                allTestResults.put(destination, testResults);
                if (testResults.isEmpty()) {
                    listener.onResult("No Scaled Tests Results from " + moduleName);
                } else {
                    listener.onResult("Scaled Tests Results from " + moduleName + ": [");
                    listener.onResult(testResults.stream().map(t -> "  " + t.toString()).collect(Collectors.toList()));
                    listener.onResult("]");
                }
            }
            listener.onResult("");
        }

        boolean hasTests = false;
        List<String> incompleteTests = new ArrayList<>();
        for (Entry<Integer, List<ScaledTestResult>> entry : allTestResults.entrySet()) {
            int key = entry.getKey();
            String module = Lookup.getAddressName(key);
            List<ScaledTestResult> results = entry.getValue();
            hasTests |= !results.isEmpty();
            // Find the tests that are incomplete and add them as string to the
            // list of incomplete tests
            incompleteTests.addAll(results.stream().filter(t -> t.getTestResult() == TestResult.NOT_COMPLETE)
                    .map(t -> "  " + module + ": " + t.toString()).collect(Collectors.toList()));
        }
        Collections.sort(incompleteTests);

        if (!hasTests) {
            listener.onResult("ERROR No tests results returned");
        } else if (incompleteTests.isEmpty()) {
            listener.onResult("All Tests Complete");
        } else {
            listener.onResult("Incomplete Tests: [");
            listener.onResult(incompleteTests);
            listener.onResult("]");
            listener.onResult(incompleteTests.size() + " Incomplete Test" + (incompleteTests.size() == 1 ? "" : "s"));
        }
    }

    /**
     * Sends a request to the given destination address for the Scaled Test
     * Results for the specified SPN
     *
     * @param listener
     *            the {@link ResultsListener}
     * @param destination
     *            the destination address to send the request to
     * @param spn
     *            the SPN for which the Scaled Test Results are being requested
     * @return the {@link List} of {@link DM30ScaledTestResultsPacket} returned.
     */
    private List<ScaledTestResult> requestScaledTestResultsForSpn(ResultsListener listener, int destination, int spn) {
        Packet request = createDM7Packet(destination, spn);
        listener.onResult(getTime() + " " + request.toString() + TX);
        DM30ScaledTestResultsPacket packet = getJ1939()
                .requestPacket(request, DM30ScaledTestResultsPacket.class, destination, 3)
                .orElse(null);
        if (packet == null) {
            listener.onResult(TIMEOUT_MESSAGE);
            return Collections.emptyList();
        } else {
            listener.onResult(getTime() + " " + packet.getPacket().toString());
            return packet.getTestResults();
        }
    }

    /**
     * Send a request to the given destination address for the Scaled Test
     * Results for all the Supported SPNs
     *
     * @param listener
     *            the {@link ResultsListener}
     * @param destination
     *            the destination address to send the request to
     * @param moduleName
     *            the name of the vehicle module for the report
     * @param spns
     *            the {@link List} of SPNs that will be requested
     * @return List of {@link ScaledTestResult}s
     */
    private List<ScaledTestResult> requestScaledTestResultsFromModule(ResultsListener listener, int destination,
            String moduleName, List<Integer> spns) {
        List<ScaledTestResult> scaledTestResults = new ArrayList<>();
        listener.onResult(getDateTime() + " Direct DM30 Requests to " + moduleName);
        for (int spn : spns) {
            List<ScaledTestResult> results = requestScaledTestResultsForSpn(listener, destination, spn);
            scaledTestResults.addAll(results);
        }
        listener.onResult("");
        return scaledTestResults;
    }

    /**
     * Sends a request to the vehicle for {@link DM24SPNSupportPacket}s
     *
     * @param listener
     *            the {@link ResultsListener}
     * @return {@link List} of {@link DM24SPNSupportPacket}s
     */
    private List<DM24SPNSupportPacket> requestSupportedSpnPackets(ResultsListener listener, List<Integer> obdModules) {
        List<DM24SPNSupportPacket> packets = new ArrayList<>();

        for (Integer address : obdModules) {
            Packet request = getJ1939().createRequestPacket(DM24SPNSupportPacket.PGN, address);
            listener.onResult(getDateTime() + " Direct DM24 Request to " + Lookup.getAddressName(address));
            listener.onResult(getTime() + " " + request.toString() + TX);
            Optional<DM24SPNSupportPacket> results = getJ1939().requestPacket(request, DM24SPNSupportPacket.class,
                    address, 3, TimeUnit.SECONDS.toMillis(15));
            if (!results.isPresent()) {
                listener.onResult(TIMEOUT_MESSAGE);
            } else {
                DM24SPNSupportPacket packet = results.get();
                listener.onResult(getTime() + " " + packet.getPacket().toString());
                listener.onResult(packet.toString());
                packets.add(packet);
            }
            listener.onResult("");
        }
        return packets;
    }

}
