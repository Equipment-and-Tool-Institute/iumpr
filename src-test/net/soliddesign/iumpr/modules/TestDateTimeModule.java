/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.modules;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

/**
 * {@link DateTimeModule} that reports a fixed point in time for testing
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class TestDateTimeModule extends DateTimeModule {

    /**
     * The {@link DateTimeFormatter} used for testing that will return a static
     * value.
     */
    private final DateTimeFormatter timeFormatter = new DateTimeFormatterBuilder()
            .appendLiteral("10:15:30.000").toFormatter();

    /**
     * Method returns the actual {@link DateTimeFormatter} used in production
     * code, not the test one.
     *
     * @return {@link DateTimeFormatter}
     */
    public DateTimeFormatter getSuperTimeFormatter() {
        return super.getTimeFormatter();
    }

    @Override
    public DateTimeFormatter getTimeFormatter() {
        return timeFormatter;
    }

    @Override
    protected LocalDateTime now() {
        return LocalDateTime.parse("2007-12-03T10:15:30.000");
    }
}
