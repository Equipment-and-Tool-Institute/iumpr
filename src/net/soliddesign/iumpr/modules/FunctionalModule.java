/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.modules;

import static net.soliddesign.iumpr.IUMPR.NL;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.etools.j1939tools.bus.Packet;
import org.etools.j1939tools.j1939.J1939;
import org.etools.j1939tools.j1939.packets.GenericPacket;
import org.etools.j1939tools.j1939.packets.ParsedPacket;
import org.etools.j1939tools.modules.DateTimeModule;

import net.soliddesign.iumpr.controllers.ResultsListener;

/**
 * Super class for all Functional Modules
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public abstract class FunctionalModule {

    public static final String TIMEOUT_MESSAGE = "Error: Timeout - No Response.";

    private final DateTimeModule dateTimeModule;

    private J1939 j1939;

    /**
     * Constructor
     *
     * @param dateTimeModule
     *            the {@link DateTimeModule} that generates the date/time
     */
    protected FunctionalModule(DateTimeModule dateTimeModule) {
        this.dateTimeModule = dateTimeModule;
    }

    /**
     * Helper Method that will parse the results send them to the
     * {@link ResultsListener}
     *
     * @param <T>
     *            the class of the Packets
     * @param listener
     *            the {@link ResultsListener} to add the results to
     * @param results
     *            the response packets to parse
     * @return a List of the Packets
     */
    protected <T extends ParsedPacket> List<T> addToReport(ResultsListener listener, Stream<T> results) {
        List<T> packets = results.collect(Collectors.toList());
        if (packets.isEmpty()) {
            listener.onResult(TIMEOUT_MESSAGE);
        } else {
            List<String> strings = packets.stream()
                    .sorted(Comparator.comparing((T p) -> p.getPacket()
                            .getTimestamp()))
                    .map(getPacketMapperFunction())
                    .toList();
            listener.onResult(strings);
        }
        return packets;
    }

    /**
     * Helper method to generate a report
     *
     * @param <T>
     *            the class of the Packet that will received
     * @param listener
     *            the {@link ResultsListener} that will be given the results
     * @param title
     *            the Title of the report section
     * @param clazz
     *            the Class of a ParsedPacket that's expected to be returned
     *            from the vehicle
     * @param request
     *            the {@link Packet} that will be sent to solicit responses from
     *            the vehicle modules
     *
     * @return the List of Packets that were received
     */
    protected <T extends GenericPacket> List<T> generateReport(ResultsListener listener, String title, Class<T> clazz,
            Packet request) {
        listener.onResult(getDateTime() + " " + title);
        listener.onResult(getTime() + " " + request.toString());
        Stream<T> packets = getJ1939().requestMultiple(clazz, request);
        return addToReport(listener, packets);
    }

    /**
     * Returns the Date/Time formatted for the reports
     *
     * @return {@link String}
     */
    protected String getDateTime() {
        return getDateTimeModule().getDateTime();
    }

    /**
     * Returns the {@link DateTimeModule}
     *
     * @return {@link DateTimeModule}
     */
    protected DateTimeModule getDateTimeModule() {
        return dateTimeModule;
    }

    /**
     * Returns the {@link J1939} used to communicate with vehicle
     *
     * @return {@link J1939}
     */
    protected J1939 getJ1939() {
        return j1939;
    }

    protected Function<ParsedPacket, String> getPacketMapperFunction() {
        return t -> t.getPacket().toTimeString() + NL + t.toString();
    }

    /**
     * Returns the Time formatted for the reports
     *
     * @return {@link String}
     */
    protected String getTime() {
        return getDateTimeModule().getTime();
    }

    /**
     * Sets the {@link J1939} that is used to communicate with the vehicle
     *
     * @param j1939
     *            the {@link J1939} to set
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Not a concern in desktop app.")
    public void setJ1939(J1939 j1939) {
        this.j1939 = j1939;
    }
}
