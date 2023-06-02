/*
 * Copyright (c) 2021. Equipment & Tool Institute
 */
package org.etools.j1939tools.modules;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.etools.j1939_84.J1939_84;

/**
 * The Module responsible for the Date/Time
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 */
public class DateTimeModule {
    private static final long GIGA = 1000000000;
    private static DateTimeModule instance = new DateTimeModule();

    public static DateTimeModule getInstance() {
        return instance;
    }

    /**
     * Only used by tests.
     */
    public static void setInstance(DateTimeModule instance) {
        DateTimeModule.instance = instance == null ? new DateTimeModule() : instance;
    }

    private Instant last = Instant.now();

    private long nanoOffset = 0;

    private DateTimeFormatter timeFormatter;

    public DateTimeModule() {
    }

    /**
     * Formats the given {@link TemporalAccessor} as a {@link String}
     *
     * @param time
     *            the {@link TemporalAccessor} to format
     * @return {@link String}
     * @throws DateTimeException
     *             if an error occurs during formatting
     */
    public String format(TemporalAccessor time) throws DateTimeException {
        try {
            return formatDateTime(time);
        } catch (DateTimeException e) {
            return formatTime(time);
        }
    }

    /**
     * Formats the given {@link TemporalAccessor} as a {@link String} which
     * includes the Date and Time
     *
     * @param dateTime
     *            the {@link TemporalAccessor} to format
     * @return {@link String}
     * @throws DateTimeException
     *             if an error occurs during formatting
     */
    private String formatDateTime(TemporalAccessor dateTime) throws DateTimeException {
        return getDateTimeFormatter().format(dateTime);
    }

    /**
     * Formats the given {@link TemporalAccessor} as a {@link String} which
     * includes only the Time
     *
     * @param dateTime
     *            the {@link TemporalAccessor} to format
     * @return {@link String}
     * @throws DateTimeException
     *             if an error occurs during formatting
     */
    private String formatTime(TemporalAccessor time) throws DateTimeException {
        return getTimeFormatter().format(time);
    }

    /**
     * @return Current date as a string.
     */
    public String getDate() {
        return now().format(DateTimeFormatter.ofPattern("yyy/MM/dd"));
    }

    /**
     * Returns the current date/time formatted as a {@link String}
     *
     * @return {@link String}
     */
    public String getDateTime() {
        return formatDateTime(now());
    }

    /**
     * Returns the formatter used to format the date/time
     *
     * @return {@link DateTimeFormatter}
     */
    private DateTimeFormatter getDateTimeFormatter() {
        // We really want this -> DateTimeFormatter.ISO_OFFSET_DATE_TIME but
        // it doesn't have a constant number of milliseconds
        return new DateTimeFormatterBuilder().parseCaseInsensitive()
                .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD).appendLiteral('-').appendValue(MONTH_OF_YEAR, 2)
                .appendLiteral('-').appendValue(DAY_OF_MONTH, 2).appendLiteral('T').appendValue(HOUR_OF_DAY, 2)
                .appendLiteral(':').appendValue(MINUTE_OF_HOUR, 2).optionalStart().appendLiteral(':')
                .appendValue(SECOND_OF_MINUTE, 2).optionalStart().appendFraction(NANO_OF_SECOND, 3, 3, true)
                .toFormatter();
    }

    /**
     * Returns the current time formatted as a {@link String}
     *
     * @return {@link String}
     */
    public String getTime() {
        return getTimeFormatter().format(now());
    }

    public long getTimeAsLong() {
        return TimeUnit.NANOSECONDS.toMillis(now().toLocalTime().toNanoOfDay());
    }

    /**
     * Returns the formatter used to format the time
     *
     * @return the {@link DateTimeFormatter}
     */
    public DateTimeFormatter getTimeFormatter() {
        if (timeFormatter == null) {
            timeFormatter = new DateTimeFormatterBuilder().parseCaseInsensitive()
                    .appendValue(HOUR_OF_DAY, 2)
                    .appendLiteral(':')
                    .appendValue(MINUTE_OF_HOUR, 2)
                    .optionalStart()
                    .appendLiteral(':')
                    .appendValue(SECOND_OF_MINUTE, 2)
                    .optionalStart()
                    .appendFraction(NANO_OF_SECOND, 4, 4, true)
                    .toFormatter();
        }
        return timeFormatter;
    }

    public int getYear() {
        return now().getYear();
    }

    /**
     * Returns the current date/time. This is exposed to it can be overridden
     * for testing.
     *
     * @return {@link LocalDateTime}
     */
    public LocalDateTime now() {
        Instant now = Instant.now().plusNanos(nanoOffset);
        if (now.isBefore(last)) {
            now = last;
            J1939_84.getLogger().log(Level.INFO, "Reusing now: " + now);
        } else {
            last = now;
        }
        return LocalDateTime.ofInstant(now, ZoneId.systemDefault());
    }

    /**
     * Parsed a {@link String} to return a {@link TemporalAccessor}
     *
     * @param string
     *            the {@link String} to parse
     * @return {@link TemporalAccessor}
     */
    public TemporalAccessor parse(String string) {
        try {
            return LocalDateTime.from(getDateTimeFormatter().parse(string));
        } catch (DateTimeException e) {
            return LocalTime.from(getTimeFormatter().parse(string));
        }
    }

    public void pauseFor(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void setNanoTime(long nanoTime) {
        nanoOffset = Instant.now().until(Instant.ofEpochSecond(nanoTime / GIGA, nanoTime % GIGA), ChronoUnit.NANOS);
    }
}
