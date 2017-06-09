/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.modules;

import static net.soliddesign.iumpr.bus.j1939.J1939.ENGINE_ADDR;
import static net.soliddesign.iumpr.bus.j1939.J1939.GLOBAL_ADDR;

import java.util.Optional;

import net.soliddesign.iumpr.bus.Packet;
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
     * Requests the Engine Hours from the engine and generates a {@link String}
     * that's suitable for inclusion in the report
     *
     * @param listener
     *            the {@link ResultsListener} that will be given the report
     */
    public void reportEngineHours(ResultsListener listener) {
        Packet request = getJ1939().createRequestPacket(EngineHoursPacket.PGN, ENGINE_ADDR);
        generateReport(listener, "Engine Hours Request", EngineHoursPacket.class, request);
    }

    /**
     * Requests the Total Vehicle Distance from the Engine and generates a
     * {@link String} that's suitable for inclusion in the report
     *
     * @param listener
     *            the {@link ResultsListener} that will be given the report
     */
    public void reportVehicleDistance(ResultsListener listener) {
        listener.onResult(getTime() + " Vehicle Distance");
        Optional<HighResVehicleDistancePacket> hiResPacket = getJ1939().read(HighResVehicleDistancePacket.class,
                ENGINE_ADDR);
        Optional<? extends ParsedPacket> packet = !hiResPacket.isPresent()
                ? getJ1939().read(TotalVehicleDistancePacket.class, ENGINE_ADDR) : hiResPacket;
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
