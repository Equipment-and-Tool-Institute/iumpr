/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.modules;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;

/**
 * The Module responsible for the Date/Time
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class DateTimeModule {

    private DateTimeFormatter formatter;

    /**
     * Formats the given {@link LocalDateTime} as a {@link String}
     *
     * @param time
     *            the {@link LocalDateTime} to format
     * @return {@link String}
     */
    public String format(LocalDateTime time) {
        return getFormatter().format(time);
    }

    /**
     * Returns the current date/time formatted as a {@link String}
     *
     * @return {@link String}
     */
    public String getDateTime() {
        return getFormatter().format(now());
    }

    /**
     * Returns the formatter used to format the date/time
     *
     * @return {@link DateTimeFormatter}
     */
    private DateTimeFormatter getFormatter() {
        if (formatter == null) {
            // We really want this -> DateTimeFormatter.ISO_OFFSET_DATE_TIME but
            // it doesn't have a constant number of milliseconds
            formatter = new DateTimeFormatterBuilder().parseCaseInsensitive()
                    .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD).appendLiteral('-').appendValue(MONTH_OF_YEAR, 2)
                    .appendLiteral('-').appendValue(DAY_OF_MONTH, 2).appendLiteral('T').appendValue(HOUR_OF_DAY, 2)
                    .appendLiteral(':').appendValue(MINUTE_OF_HOUR, 2).optionalStart().appendLiteral(':')
                    .appendValue(SECOND_OF_MINUTE, 2).optionalStart().appendFraction(NANO_OF_SECOND, 3, 3, true)
                    .toFormatter();
        }
        return formatter;
    }

    /**
     * Returns the current date/time. This is exposed to it can be overridden
     * for testing.
     *
     * @return {@link LocalDateTime}
     */
    protected LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * Parsed a {@link String} to return a {@link LocalDateTime}
     *
     * @param string
     *            the {@link String} to parse
     * @return {@link LocalDateTime}
     */
    public LocalDateTime parse(String string) {
        return LocalDateTime.from(getFormatter().parse(string));
    }

}
