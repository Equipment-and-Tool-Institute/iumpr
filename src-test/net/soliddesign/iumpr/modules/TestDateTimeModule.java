/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.modules;

import java.time.LocalDateTime;

/**
 * {@link DateTimeModule} that reports a fixed point in time for testing
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class TestDateTimeModule extends DateTimeModule {

    @Override
    protected LocalDateTime now() {
        return LocalDateTime.parse("2007-12-03T10:15:30.000");
    }
}
