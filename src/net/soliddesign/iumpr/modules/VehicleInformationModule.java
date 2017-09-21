/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.modules;

import static net.soliddesign.iumpr.bus.j1939.J1939.GLOBAL_ADDR;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import net.soliddesign.iumpr.bus.BusException;
import net.soliddesign.iumpr.bus.Packet;
import net.soliddesign.iumpr.bus.j1939.packets.AddressClaimPacket;
import net.soliddesign.iumpr.bus.j1939.packets.ComponentIdentificationPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM19CalibrationInformationPacket;
import net.soliddesign.iumpr.bus.j1939.packets.EngineHoursPacket;
import net.soliddesign.iumpr.bus.j1939.packets.HighResVehicleDistancePacket;
import net.soliddesign.iumpr.bus.j1939.packets.ParsedPacket;
import net.soliddesign.iumpr.bus.j1939.packets.TotalVehicleDistancePacket;
import net.soliddesign.iumpr.bus.j1939.packets.VehicleIdentificationPacket;
import net.soliddesign.iumpr.controllers.ResultsListener;

/**
 * The {@link FunctionalModule} that is used to gather general information about
 * the vehicle
 *
 * @author Matt Gumbel (matt@solidesign.net)
 *
 */
public class VehicleInformationModule extends FunctionalModule {

    /**
     * Constructor
     */
    public VehicleInformationModule() {
        this(new DateTimeModule());
    }

    /**
     * Constructor exposed for testing
     *
     * @param dateTimeModule
     *            the {@link DateTimeModule} to use
     */
    public VehicleInformationModule(DateTimeModule dateTimeModule) {
        super(dateTimeModule);
    }

    /**
     * Sends the Request for Address Claim and reports the results
     *
     * @param listener
     *            the {@link ResultsListener} that will be given the report
     */
    public void reportAddressClaim(ResultsListener listener) {
        Packet request = getJ1939().createRequestPacket(AddressClaimPacket.PGN, GLOBAL_ADDR);
        List<AddressClaimPacket> responses = generateReport(listener, "Global Request for Address Claim",
                AddressClaimPacket.class, request);
        if (!responses.isEmpty() && !responses.stream().filter(p -> p.getFunctionId() == 0).findAny().isPresent()) {
            listener.onResult("Error: No module reported Function 0");
        }
    }

    /**
     * Requests the Calibration Information from all vehicle modules and
     * generates a {@link String} that's suitable for inclusion in the report
     *
     * @param listener
     *            the {@link ResultsListener} that will be given the report
     */
    public void reportCalibrationInformation(ResultsListener listener) {
        Packet request = getJ1939().createRequestPacket(DM19CalibrationInformationPacket.PGN, GLOBAL_ADDR);
        generateReport(listener, "Global DM19 (Calibration Information) Request",
                DM19CalibrationInformationPacket.class,
                request);
    }

    /**
     * Requests the Component Identification from all vehicle modules and
     * generates a {@link String} that's suitable for inclusion in the report
     *
     * @param listener
     *            the {@link ResultsListener} that will be given the report
     */
    public void reportComponentIdentification(ResultsListener listener) {
        Packet request = getJ1939().createRequestPacket(ComponentIdentificationPacket.PGN, GLOBAL_ADDR);
        generateReport(listener, "Global Component Identification Request", ComponentIdentificationPacket.class,
                request);
    }

    /**
     * Queries the bus and reports the speed of the vehicle bus
     *
     * @param listener
     *            the {@link ResultsListener} that will be given the report
     */
    public void reportConnectionSpeed(ResultsListener listener) {
        String result = getDateTime() + " Baud Rate: ";
        try {
            int speed = getJ1939().getBus().getConnectionSpeed();
            result += NumberFormat.getInstance(Locale.US).format(speed) + " bps";
        } catch (BusException e) {
            result += "Could not be determined";
        }
        listener.onResult(result);
    }

    /**
     * Requests the Engine Hours from the engine and generates a {@link String}
     * that's suitable for inclusion in the report
     *
     * @param listener
     *            the {@link ResultsListener} that will be given the report
     */
    public void reportEngineHours(ResultsListener listener) {
        Packet request = getJ1939().createRequestPacket(EngineHoursPacket.PGN, GLOBAL_ADDR);
        generateReport(listener, "Engine Hours Request", EngineHoursPacket.class, request);
    }

    /**
     * Requests the maximum Total Vehicle Distance from the vehicle and
     * generates a {@link String} that's suitable for inclusion in the report
     *
     * @param listener
     *            the {@link ResultsListener} that will be given the report
     */
    public void reportVehicleDistance(ResultsListener listener) {
        listener.onResult(getDateTime() + " Vehicle Distance");
        Optional<HighResVehicleDistancePacket> hiResPacket = getJ1939()
                .read(HighResVehicleDistancePacket.class, 3, TimeUnit.SECONDS)
                .filter(p -> p.getTotalVehicleDistance() != ParsedPacket.NOT_AVAILABLE
                        && p.getTotalVehicleDistance() != ParsedPacket.ERROR)
                .max((p1, p2) -> Double.compare(p1.getTotalVehicleDistance(), p2.getTotalVehicleDistance()));

        Optional<? extends ParsedPacket> packet;
        if (hiResPacket.isPresent()) {
            packet = hiResPacket;
        } else {
            packet = getJ1939().read(TotalVehicleDistancePacket.class, 300, TimeUnit.MILLISECONDS)
                    .filter(p -> p.getTotalVehicleDistance() != ParsedPacket.NOT_AVAILABLE
                            && p.getTotalVehicleDistance() != ParsedPacket.ERROR)
                    .max((p1, p2) -> Double.compare(p1.getTotalVehicleDistance(), p2.getTotalVehicleDistance()));
        }

        listener.onResult(packet.map(getPacketMapperFunction()).orElse(TIMEOUT_MESSAGE));
    }

    /**
     * Requests the Vehicle Identification from all vehicle modules and
     * generates a {@link String} that's suitable for inclusion in the report
     *
     * @param listener
     *            the {@link ResultsListener} that will be given the report
     */
    public void reportVin(ResultsListener listener) {
        Packet request = getJ1939().createRequestPacket(VehicleIdentificationPacket.PGN, GLOBAL_ADDR);
        generateReport(listener, "Global VIN Request", VehicleIdentificationPacket.class, request);
    }
}
