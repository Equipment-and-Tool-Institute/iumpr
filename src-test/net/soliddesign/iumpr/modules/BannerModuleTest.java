/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.modules;

import static net.soliddesign.iumpr.IUMPR.NL;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import net.soliddesign.iumpr.BuildNumber;
import net.soliddesign.iumpr.controllers.TestResultsListener;
import net.soliddesign.iumpr.modules.BannerModule.Type;

/**
 * The unit tests for the {@link BannerModule}
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class BannerModuleTest {

    private BuildNumber buildNumber;
    private BannerModule instance;
    private TestResultsListener listener;

    @Before
    public void setup() {
        listener = new TestResultsListener();
        buildNumber = new BuildNumber() {
            @Override
            public String getVersionNumber() {
                return "1.2.0";
            };
        };
    }

    @Test
    public void testAbortedWithNull() {
        instance = new BannerModule(null, new TestDateTimeModule(), buildNumber);
        String expected = "2007-12-03T10:15:30.000 I U M P R Data Collection Tool  Aborted" + NL;
        instance.reportAborted(listener);
        assertEquals(expected, listener.getResults());
    }

    @Test
    public void testCollectionLogAborted() {
        instance = new BannerModule(Type.COLLECTION_LOG, new TestDateTimeModule(), buildNumber);
        String expected = "2007-12-03T10:15:30.000 I U M P R Data Collection Tool Data Collection Log Aborted"
                + NL;
        instance.reportAborted(listener);
        assertEquals(expected, listener.getResults());
    }

    @Test
    public void testCollectionLogFooter() {
        instance = new BannerModule(Type.COLLECTION_LOG, new TestDateTimeModule(), buildNumber);
        String expected = "2007-12-03T10:15:30.000 I U M P R Data Collection Tool Data Collection Log END OF REPORT"
                + NL;
        instance.reportFooter(listener);
        assertEquals(expected, listener.getResults());
    }

    @Test
    public void testCollectionLogHeader() {
        instance = new BannerModule(Type.COLLECTION_LOG, new TestDateTimeModule(), buildNumber);
        String expected = "";
        expected += "2007-12-03T10:15:30.000 I U M P R Data Collection Tool for 13 CCR 1971.1(l)(2) and (h)(1.7)"
                + NL;
        expected += "2007-12-03T10:15:30.000 I U M P R Data Collection Tool version 1.2.0" + NL;
        expected += NL;
        expected += "IIIIIIIII  UUU     UUU  MMM       MMM  PPPPPPPP     RRRRRRRR     DATA COLLECTION LOG" + NL;
        expected += "IIIIIIIII  UUU     UUU  MMMM     MMMM  PPPPPPPPP    RRRRRRRRR    DATA COLLECTION LOG" + NL;
        expected += "   III     UUU     UUU  MMMMM   MMMMM  PPP    PPP   RRR    RRR   DATA COLLECTION LOG" + NL;
        expected += "   III     UUU     UUU  MMMMMMMMMMMMM  PPP     PPP  RRR     RRR  DATA COLLECTION LOG" + NL;
        expected += "   III     UUU     UUU  MMM  MMM  MMM  PPP     PPP  RRR     RRR  DATA COLLECTION LOG" + NL;
        expected += "   III     UUU     UUU  MMM   M   MMM  PPP    PPP   RRR    RRR   DATA COLLECTION LOG" + NL;
        expected += "   III     UUU     UUU  MMM       MMM  PPPPPPPPP    RRRRRRRRR    DATA COLLECTION LOG" + NL;
        expected += "   III     UUU     UUU  MMM       MMM  PPPPPPPP     RRRRRRRR     DATA COLLECTION LOG" + NL;
        expected += "   III     UUU     UUU  MMM       MMM  PPP          RRR RRR      DATA COLLECTION LOG" + NL;
        expected += "   III     UUU     UUU  MMM       MMM  PPP          RRR  RRR     DATA COLLECTION LOG" + NL;
        expected += "   III     UUU     UUU  MMM       MMM  PPP          RRR   RRR    DATA COLLECTION LOG" + NL;
        expected += "IIIIIIIII  UUUUUUUUUUU  MMM       MMM  PPP          RRR    RRR   DATA COLLECTION LOG" + NL;
        expected += "IIIIIIIII   UUUUUUUUU   MMM       MMM  PPP          RRR     RRR  DATA COLLECTION LOG" + NL;
        expected += NL;
        expected += "2007-12-03T10:15:30.000 I U M P R Data Collection Tool Data Collection Log" + NL;
        instance.reportHeader(listener);
        assertEquals(expected, listener.getResults());
    }

    @Test
    public void testCollectionLogStopped() {
        instance = new BannerModule(Type.COLLECTION_LOG, new TestDateTimeModule(), buildNumber);
        String expected = "2007-12-03T10:15:30.000 I U M P R Data Collection Tool Data Collection Log Stopped"
                + NL;
        instance.reportStopped(listener);
        assertEquals(expected, listener.getResults());
    }

    @Test
    public void testDataPlateLogHeader() {
        instance = new BannerModule(Type.DATA_PLATE, new TestDateTimeModule(), buildNumber);
        String expected = "";
        expected += "2007-12-03T10:15:30.000 I U M P R Data Collection Tool for 13 CCR 1971.1(l)(2) and (h)(1.7)"
                + NL;
        expected += "2007-12-03T10:15:30.000 I U M P R Data Collection Tool version 1.2.0" + NL;
        expected += NL;
        expected += "IIIIIIIII  UUU     UUU  MMM       MMM  PPPPPPPP     RRRRRRRR     DATA PLATE REPORT" + NL;
        expected += "IIIIIIIII  UUU     UUU  MMMM     MMMM  PPPPPPPPP    RRRRRRRRR    DATA PLATE REPORT" + NL;
        expected += "   III     UUU     UUU  MMMMM   MMMMM  PPP    PPP   RRR    RRR   DATA PLATE REPORT" + NL;
        expected += "   III     UUU     UUU  MMMMMMMMMMMMM  PPP     PPP  RRR     RRR  DATA PLATE REPORT" + NL;
        expected += "   III     UUU     UUU  MMM  MMM  MMM  PPP     PPP  RRR     RRR  DATA PLATE REPORT" + NL;
        expected += "   III     UUU     UUU  MMM   M   MMM  PPP    PPP   RRR    RRR   DATA PLATE REPORT" + NL;
        expected += "   III     UUU     UUU  MMM       MMM  PPPPPPPPP    RRRRRRRRR    DATA PLATE REPORT" + NL;
        expected += "   III     UUU     UUU  MMM       MMM  PPPPPPPP     RRRRRRRR     DATA PLATE REPORT" + NL;
        expected += "   III     UUU     UUU  MMM       MMM  PPP          RRR RRR      DATA PLATE REPORT" + NL;
        expected += "   III     UUU     UUU  MMM       MMM  PPP          RRR  RRR     DATA PLATE REPORT" + NL;
        expected += "   III     UUU     UUU  MMM       MMM  PPP          RRR   RRR    DATA PLATE REPORT" + NL;
        expected += "IIIIIIIII  UUUUUUUUUUU  MMM       MMM  PPP          RRR    RRR   DATA PLATE REPORT" + NL;
        expected += "IIIIIIIII   UUUUUUUUU   MMM       MMM  PPP          RRR     RRR  DATA PLATE REPORT" + NL;
        expected += NL;
        expected += "2007-12-03T10:15:30.000 I U M P R Data Collection Tool Data Plate Report" + NL;
        instance.reportHeader(listener);
        assertEquals(expected, listener.getResults());
    }

    @Test
    public void testDatePlateAborted() {
        instance = new BannerModule(Type.DATA_PLATE, new TestDateTimeModule(), buildNumber);
        String expected = "2007-12-03T10:15:30.000 I U M P R Data Collection Tool Data Plate Report Aborted"
                + NL;
        instance.reportAborted(listener);
        assertEquals(expected, listener.getResults());
    }

    @Test
    public void testDatePlateFooter() {
        instance = new BannerModule(Type.DATA_PLATE, new TestDateTimeModule(), buildNumber);
        String expected = "2007-12-03T10:15:30.000 I U M P R Data Collection Tool Data Plate Report END OF REPORT"
                + NL;
        instance.reportFooter(listener);
        assertEquals(expected, listener.getResults());
    }

    @Test
    public void testDatePlateStopped() {
        instance = new BannerModule(Type.DATA_PLATE, new TestDateTimeModule(), buildNumber);
        String expected = "2007-12-03T10:15:30.000 I U M P R Data Collection Tool Data Plate Report Stopped"
                + NL;
        instance.reportStopped(listener);
        assertEquals(expected, listener.getResults());
    }

    @Test
    public void testFooterWithNull() {
        instance = new BannerModule(null, new TestDateTimeModule(), buildNumber);
        String expected = "2007-12-03T10:15:30.000 I U M P R Data Collection Tool  END OF REPORT" + NL;
        instance.reportFooter(listener);
        assertEquals(expected, listener.getResults());
    }

    @Test
    public void testGetTypeNameForCollectionLog() {
        instance = new BannerModule(Type.COLLECTION_LOG, new TestDateTimeModule(), buildNumber);
        assertEquals("Data Collection Log", instance.getTypeName());
    }

    @Test
    public void testGetTypeNameForDataPlate() {
        instance = new BannerModule(Type.DATA_PLATE, new TestDateTimeModule(), buildNumber);
        assertEquals("Data Plate Report", instance.getTypeName());
    }

    @Test
    public void testGetTypeNameForMonitorLog() {
        instance = new BannerModule(Type.MONITOR_LOG, new TestDateTimeModule(), buildNumber);
        assertEquals("Data Monitor Log", instance.getTypeName());
    }

    @Test
    public void testGetTypeNameNull() {
        instance = new BannerModule(null, new TestDateTimeModule(), buildNumber);
        assertEquals("", instance.getTypeName());
    }

    @Test
    public void testHeaderWithNull() {
        instance = new BannerModule(null, new TestDateTimeModule(), buildNumber);
        String expected = "";
        expected += "2007-12-03T10:15:30.000 I U M P R Data Collection Tool for 13 CCR 1971.1(l)(2) and (h)(1.7)"
                + NL;
        expected += "2007-12-03T10:15:30.000 I U M P R Data Collection Tool version 1.2.0" + NL;
        expected += NL;
        expected += "IIIIIIIII  UUU     UUU  MMM       MMM  PPPPPPPP     RRRRRRRR     " + NL;
        expected += "IIIIIIIII  UUU     UUU  MMMM     MMMM  PPPPPPPPP    RRRRRRRRR    " + NL;
        expected += "   III     UUU     UUU  MMMMM   MMMMM  PPP    PPP   RRR    RRR   " + NL;
        expected += "   III     UUU     UUU  MMMMMMMMMMMMM  PPP     PPP  RRR     RRR  " + NL;
        expected += "   III     UUU     UUU  MMM  MMM  MMM  PPP     PPP  RRR     RRR  " + NL;
        expected += "   III     UUU     UUU  MMM   M   MMM  PPP    PPP   RRR    RRR   " + NL;
        expected += "   III     UUU     UUU  MMM       MMM  PPPPPPPPP    RRRRRRRRR    " + NL;
        expected += "   III     UUU     UUU  MMM       MMM  PPPPPPPP     RRRRRRRR     " + NL;
        expected += "   III     UUU     UUU  MMM       MMM  PPP          RRR RRR      " + NL;
        expected += "   III     UUU     UUU  MMM       MMM  PPP          RRR  RRR     " + NL;
        expected += "   III     UUU     UUU  MMM       MMM  PPP          RRR   RRR    " + NL;
        expected += "IIIIIIIII  UUUUUUUUUUU  MMM       MMM  PPP          RRR    RRR   " + NL;
        expected += "IIIIIIIII   UUUUUUUUU   MMM       MMM  PPP          RRR     RRR  " + NL;
        expected += NL;
        expected += "2007-12-03T10:15:30.000 I U M P R Data Collection Tool " + NL;
        instance.reportHeader(listener);
        assertEquals(expected, listener.getResults());
    }

    @Test
    public void testMonitorLogAborted() {
        instance = new BannerModule(Type.MONITOR_LOG, new TestDateTimeModule(), buildNumber);
        String expected = "2007-12-03T10:15:30.000 I U M P R Data Collection Tool Data Monitor Log Aborted"
                + NL;
        instance.reportAborted(listener);
        assertEquals(expected, listener.getResults());
    }

    @Test
    public void testMonitorLogFooter() {
        instance = new BannerModule(Type.MONITOR_LOG, new TestDateTimeModule(), buildNumber);
        String expected = "2007-12-03T10:15:30.000 I U M P R Data Collection Tool Data Monitor Log END OF REPORT"
                + NL;
        instance.reportFooter(listener);
        assertEquals(expected, listener.getResults());
    }

    @Test
    public void testMonitorLogHeader() {
        instance = new BannerModule(Type.MONITOR_LOG, new TestDateTimeModule(), buildNumber);
        String expected = "";
        expected += "2007-12-03T10:15:30.000 I U M P R Data Collection Tool for 13 CCR 1971.1(l)(2) and (h)(1.7)"
                + NL;
        expected += "2007-12-03T10:15:30.000 I U M P R Data Collection Tool version 1.2.0" + NL;
        expected += NL;
        expected += "IIIIIIIII  UUU     UUU  MMM       MMM  PPPPPPPP     RRRRRRRR     DATA MONITOR LOG" + NL;
        expected += "IIIIIIIII  UUU     UUU  MMMM     MMMM  PPPPPPPPP    RRRRRRRRR    DATA MONITOR LOG" + NL;
        expected += "   III     UUU     UUU  MMMMM   MMMMM  PPP    PPP   RRR    RRR   DATA MONITOR LOG" + NL;
        expected += "   III     UUU     UUU  MMMMMMMMMMMMM  PPP     PPP  RRR     RRR  DATA MONITOR LOG" + NL;
        expected += "   III     UUU     UUU  MMM  MMM  MMM  PPP     PPP  RRR     RRR  DATA MONITOR LOG" + NL;
        expected += "   III     UUU     UUU  MMM   M   MMM  PPP    PPP   RRR    RRR   DATA MONITOR LOG" + NL;
        expected += "   III     UUU     UUU  MMM       MMM  PPPPPPPPP    RRRRRRRRR    DATA MONITOR LOG" + NL;
        expected += "   III     UUU     UUU  MMM       MMM  PPPPPPPP     RRRRRRRR     DATA MONITOR LOG" + NL;
        expected += "   III     UUU     UUU  MMM       MMM  PPP          RRR RRR      DATA MONITOR LOG" + NL;
        expected += "   III     UUU     UUU  MMM       MMM  PPP          RRR  RRR     DATA MONITOR LOG" + NL;
        expected += "   III     UUU     UUU  MMM       MMM  PPP          RRR   RRR    DATA MONITOR LOG" + NL;
        expected += "IIIIIIIII  UUUUUUUUUUU  MMM       MMM  PPP          RRR    RRR   DATA MONITOR LOG" + NL;
        expected += "IIIIIIIII   UUUUUUUUU   MMM       MMM  PPP          RRR     RRR  DATA MONITOR LOG" + NL;
        expected += NL;
        expected += "2007-12-03T10:15:30.000 I U M P R Data Collection Tool Data Monitor Log" + NL;
        instance.reportHeader(listener);
        assertEquals(expected, listener.getResults());
    }

    @Test
    public void testMonitorLogStopped() {
        instance = new BannerModule(Type.MONITOR_LOG, new TestDateTimeModule(), buildNumber);
        String expected = "2007-12-03T10:15:30.000 I U M P R Data Collection Tool Data Monitor Log Stopped"
                + NL;
        instance.reportStopped(listener);
        assertEquals(expected, listener.getResults());
    }

    @Test
    public void testStoppedWithNull() {
        instance = new BannerModule(null, new TestDateTimeModule(), buildNumber);
        String expected = "2007-12-03T10:15:30.000 I U M P R Data Collection Tool  Stopped" + NL;
        instance.reportStopped(listener);
        assertEquals(expected, listener.getResults());
    }

    @Test
    public void testTypeToString() {
        assertEquals("Data Collection Log", Type.COLLECTION_LOG.toString());
        assertEquals("Data Monitor Log", Type.MONITOR_LOG.toString());
        assertEquals("Data Plate Report", Type.DATA_PLATE.toString());
    }
}
