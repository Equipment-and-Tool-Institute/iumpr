/**
 * Copyright 2019 Equipment & Tool Institute
 */
package org.etools.j1939_84;

import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main class for the J1939_84 Application
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 */
public class J1939_84 {

    /**
     * The name of the property that is set when the application is being used
     * in a development mode
     */
    public static final String DEV_PROPERTY_NAME = "DEV";

    private static final Logger logger = Logger.getGlobal();

    /**
     * System independent New Line
     */
    public static final String NL = System.lineSeparator();

    /** Page break including text for editors that do not support page break. */
    public static final String PAGE_BREAK = " <<PAGE BREAK>> " + NL + "\f";

    /**
     * The name of the property that is set when the application is being used
     * in a testing mode
     */
    public static final String TESTING_PROPERTY_NAME = "TESTING";

    static {
        try {
            System.setProperty("java.util.logging.SimpleFormatter.format",
                    "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL %4$-6s %2$s %5$s%6$s%n");
            logger.setUseParentHandlers(false);

            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.ALL);
            logger.addHandler(consoleHandler);

            FileHandler fileHandler = new FileHandler("%t/j1939_84%g.log", 10 * 1024 * 1024, 100, true);
            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(consoleHandler.getFormatter());
            logger.addHandler(fileHandler);

            logger.setLevel(Level.ALL);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error setting up logging", e);
        }
    }

    public static Logger getLogger() {
        return logger;
    }

    public static boolean isAutoMode() {
        return isDevEnv() && isTesting();
    }

    /**
     * Returns true if the application is running in dev environment
     *
     * @return true if it's being tested
     */
    public static boolean isDevEnv() {
        return Boolean.getBoolean(DEV_PROPERTY_NAME);
    }

    /**
     * Returns true if the application is under test
     *
     * @return true if it's being tested
     */
    public static boolean isTesting() {
        return Boolean.getBoolean(TESTING_PROPERTY_NAME);
    }

    /**
     * Sets the System Property to indicate the system is under test
     *
     * @param testing
     *            - true to indicate the system is under test
     */
    public static void setTesting(boolean testing) {
        System.setProperty(TESTING_PROPERTY_NAME, Boolean.toString(testing));
    }

}
