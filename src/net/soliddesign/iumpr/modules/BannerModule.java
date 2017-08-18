/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.modules;

import java.util.Locale;

import net.soliddesign.iumpr.BuildNumber;
import net.soliddesign.iumpr.controllers.ResultsListener;

/**
 * Generates the IUMPR Banner for the Reports
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class BannerModule extends FunctionalModule {
    /**
     * The different types of banners that can be generated
     */
    public enum Type {
        COLLECTION_LOG("Data Collection Log"), DATA_PLATE("Data Plate Report"), MONITOR_LOG("Data Monitor Log");

        private final String string;

        private Type(String string) {
            this.string = string;
        }

        @Override
        public String toString() {
            return string;
        }
    }

    /**
     * The string that indicates the report section is complete
     */
    static final String END_OF_REPORT = "END OF REPORT";

    /**
     * The name of the tool for inclusion in the report
     */
    static final String TOOL_NAME = "IUMPR Data Collection Tool";

    private final BuildNumber buildNumber;

    private final Type type;

    /**
     * Constructor
     *
     * @param type
     *            the Report {@link Type} this banner will be for
     */
    public BannerModule(Type type) {
        this(type, new DateTimeModule(), new BuildNumber());
    }

    /**
     * Constructor exposed to for testing
     *
     * @param type
     *            the Report {@link Type} this banner will be for
     * @param dateTimeModule
     *            the {@link DateTimeModule}
     * @param buildNumber
     *            the {@link BuildNumber}
     */
    public BannerModule(Type type, DateTimeModule dateTimeModule, BuildNumber buildNumber) {
        super(dateTimeModule);
        this.type = type;
        this.buildNumber = buildNumber;
    }

    private String getFooter(String suffix) {
        return getDateTime() + " " + TOOL_NAME + " " + getTypeName() + " " + suffix;
    }

    /**
     * Returns the name of the report
     *
     * @return the Name as a {@link String}
     */
    public String getTypeName() {
        return type == null ? "" : type.toString();
    }

    /**
     * Writes the Aborted Footer for the report to the {@link ResultsListener}
     *
     * @param listener
     *            the {@link ResultsListener}
     */
    public void reportAborted(ResultsListener listener) {
        listener.onResult(getFooter("Aborted"));
    }

    /**
     * Writes the Footer for the report to the {@link ResultsListener}
     *
     * @param listener
     *            the {@link ResultsListener}
     */
    public void reportFooter(ResultsListener listener) {
        listener.onResult(getFooter(END_OF_REPORT));
    }

    /**
     * Writes the Header for the report to the {@link ResultsListener}
     *
     * @param listener
     *            the {@link ResultsListener} to give the header to
     */
    public void reportHeader(ResultsListener listener) {
        String reportName = getTypeName();
        String ending = reportName.toUpperCase(Locale.US);

        listener.onResult(getDateTime() + " " + TOOL_NAME + " for 13 CCR 1971.1(l)(2) and (h)(1.7)");
        listener.onResult(getDateTime() + " " + TOOL_NAME + " version " + buildNumber.getVersionNumber());
        listener.onResult("");
        listener.onResult("IIIIIIIII  UUU     UUU  MMM       MMM  PPPPPPPP     RRRRRRRR     " + ending);
        listener.onResult("IIIIIIIII  UUU     UUU  MMMM     MMMM  PPPPPPPPP    RRRRRRRRR    " + ending);
        listener.onResult("   III     UUU     UUU  MMMMM   MMMMM  PPP    PPP   RRR    RRR   " + ending);
        listener.onResult("   III     UUU     UUU  MMMMMMMMMMMMM  PPP     PPP  RRR     RRR  " + ending);
        listener.onResult("   III     UUU     UUU  MMM  MMM  MMM  PPP     PPP  RRR     RRR  " + ending);
        listener.onResult("   III     UUU     UUU  MMM   M   MMM  PPP    PPP   RRR    RRR   " + ending);
        listener.onResult("   III     UUU     UUU  MMM       MMM  PPPPPPPPP    RRRRRRRRR    " + ending);
        listener.onResult("   III     UUU     UUU  MMM       MMM  PPPPPPPP     RRRRRRRR     " + ending);
        listener.onResult("   III     UUU     UUU  MMM       MMM  PPP          RRR RRR      " + ending);
        listener.onResult("   III     UUU     UUU  MMM       MMM  PPP          RRR  RRR     " + ending);
        listener.onResult("   III     UUU     UUU  MMM       MMM  PPP          RRR   RRR    " + ending);
        listener.onResult("IIIIIIIII  UUUUUUUUUUU  MMM       MMM  PPP          RRR    RRR   " + ending);
        listener.onResult("IIIIIIIII   UUUUUUUUU   MMM       MMM  PPP          RRR     RRR  " + ending);
        listener.onResult("");
        listener.onResult(getDateTime() + " " + TOOL_NAME + " " + reportName);
    }

    /**
     * Writes the Stopped Footer for the report to the {@link ResultsListener}
     *
     * @param listener
     *            the {@link ResultsListener}
     */
    public void reportStopped(ResultsListener listener) {
        listener.onResult(getFooter("Stopped"));
    }

}
