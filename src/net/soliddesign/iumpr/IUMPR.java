/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr;

import java.awt.EventQueue;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.UIManager;

import net.soliddesign.iumpr.ui.UserInterfaceView;

/**
 * Main class for the IUMPR Application
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class IUMPR {

    private static final Logger logger = Logger.getGlobal();

    /**
     * System independent New Line
     */
    public static final String NL = System.lineSeparator();

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

            FileHandler fileHandler = new FileHandler("%t/iumpr%g.log", 10 * 1024 * 1024, 100, true);
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

    /**
     * Returns true if the application is being debugged
     *
     * @return true if it's being debugged
     */
    public static final boolean isDebug() {
        return java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString()
                .indexOf("-agentlib:jdwp") > 0;
    }

    /**
     * Returns true if the application is under test
     *
     * @return true if it's being tested
     */
    public static final boolean isTesting() {
        return System.getProperty(TESTING_PROPERTY_NAME, "false").equals("true");
    }

    /**
     * Launch the application.
     *
     * @param args
     *            The arguments used to start the application
     */
    public static void main(String[] args) {
        getLogger().info("IUMPR starting");
        if (args.length > 0 && TESTING_PROPERTY_NAME.equals(args[0])) {
            setTesting(true);
        }

        try {
            // Set System L&F
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            getLogger().log(Level.INFO, "Unable to set Look and Feel");
        }

        EventQueue.invokeLater(() -> {
            try {
                new UserInterfaceView().getFrame().setVisible(true);
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Error showing frame", e);
            }
        });
    }

    /**
     * Sets the System Property to indicate the system is under test
     *
     * @param testing
     *            - true to indicate the system is under test
     */
    public static final void setTesting(boolean testing) {
        System.setProperty(TESTING_PROPERTY_NAME, Boolean.valueOf(testing).toString());
    }
}
