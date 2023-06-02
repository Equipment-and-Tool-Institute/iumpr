/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.ui.status;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;

import org.etools.j1939tools.j1939.packets.DM20MonitorPerformanceRatioPacket;
import org.etools.j1939tools.j1939.packets.ParsedPacket;
import org.etools.j1939tools.j1939.packets.PerformanceRatio;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.soliddesign.iumpr.modules.ReportFileModule;

/**
 * Unit tests for the {@link RatiosTable}
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", justification = "The verify call doesn't need to read the values")
public class RatiosTableTest {

    private RatiosTable instance;

    private TableModelListener listener;

    @Mock
    private ReportFileModule reportFileModule;

    private boolean tableUpdated;

    @Before
    public void setUp() throws Exception {
        listener = e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                tableUpdated = true;
            }
        };
        instance = new RatiosTable();
        instance.getModel().addTableModelListener(listener);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(reportFileModule);
    }

    @Test
    public void testInitialSetup() {
        assertEquals("Source", instance.getColumnName(0));
        assertEquals("Monitor from DM20", instance.getColumnName(1));
        assertEquals("Numerator", instance.getColumnName(2));
        assertEquals("Denominator", instance.getColumnName(3));

        assertEquals(4, instance.getColumnCount());
        assertEquals(0, instance.getRowCount());

        assertTrue(instance.getAutoCreateColumnsFromModel());
        assertTrue(instance.getAutoCreateRowSorter());
    }

    @Test
    public void testProcessDM20() {
        DM20MonitorPerformanceRatioPacket packet = mock(DM20MonitorPerformanceRatioPacket.class);
        Set<PerformanceRatio> ratios = new HashSet<>();
        ratios.add(new PerformanceRatio(123, 0, 1, 0));
        ratios.add(new PerformanceRatio(456, 2, 3, 0));
        when(packet.getRatios()).thenReturn(ratios.stream().collect(Collectors.toList()));
        when(reportFileModule.getInitialRatios()).thenReturn(ratios);

        instance.setReportFileModule(reportFileModule);
        instance.process(packet);
        assertTrue(tableUpdated);

        assertEquals(2, instance.getRowCount());
        assertEquals("Engine #1 (0)", instance.getValueAt(0, 0));
        assertEquals("SPN  123 Transmission Clutch 1 Pressure", instance.getValueAt(0, 1));
        assertEquals(0, instance.getValueAt(0, 2));
        assertEquals(1, instance.getValueAt(0, 3));
        validateBackgroundColor(instance.getBackground(), 0);

        assertEquals("Engine #1 (0)", instance.getValueAt(1, 0));
        assertEquals("SPN  456 Unknown", instance.getValueAt(1, 1));
        assertEquals(2, instance.getValueAt(1, 2));
        assertEquals(3, instance.getValueAt(1, 3));
        validateBackgroundColor(instance.getBackground(), 1);

        for (int row = 0; row < instance.getRowCount(); row++) {
            for (int col = 0; col < instance.getColumnCount(); col++) {
                assertFalse(instance.isCellEditable(row, col));
            }
        }
        verify(reportFileModule).getInitialRatios();
    }

    @Test
    public void testProcessDM20ChangedSinceInitialBackwards() {
        {
            List<PerformanceRatio> ratios = new ArrayList<>();
            ratios.add(new PerformanceRatio(123, 10, 1, 0));
            ratios.add(new PerformanceRatio(456, 2, 3, 0));
            when(reportFileModule.getInitialRatios()).thenReturn(ratios.stream().collect(Collectors.toSet()));
        }

        DM20MonitorPerformanceRatioPacket packet = mock(DM20MonitorPerformanceRatioPacket.class);
        List<PerformanceRatio> ratios = new ArrayList<>();
        ratios.add(new PerformanceRatio(123, 1, 1, 0));
        ratios.add(new PerformanceRatio(456, 2, 3, 0));
        when(packet.getRatios()).thenReturn(ratios);

        instance.setReportFileModule(reportFileModule);
        instance.process(packet);
        assertTrue(tableUpdated);

        assertEquals(2, instance.getRowCount());
        assertEquals("Engine #1 (0)", instance.getValueAt(0, 0));
        assertEquals("SPN  123 Transmission Clutch 1 Pressure", instance.getValueAt(0, 1));
        assertEquals(1, instance.getValueAt(0, 2));
        assertEquals(1, instance.getValueAt(0, 3));
        validateBackgroundColor(instance.getBackground(), 0, 0);
        validateBackgroundColor(instance.getBackground(), 0, 1);
        validateBackgroundColor(Color.RED, 0, 2);
        validateBackgroundColor(instance.getBackground(), 0, 3);

        assertEquals("Engine #1 (0)", instance.getValueAt(1, 0));
        assertEquals("SPN  456 Unknown", instance.getValueAt(1, 1));
        assertEquals(2, instance.getValueAt(1, 2));
        assertEquals(3, instance.getValueAt(1, 3));
        validateBackgroundColor(instance.getBackground(), 1);

        verify(reportFileModule).getInitialRatios();
    }

    @Test
    public void testProcessDM20ChangedSinceInitialForwards() {
        {
            List<PerformanceRatio> ratios = new ArrayList<>();
            ratios.add(new PerformanceRatio(123, 0, 1, 0));
            ratios.add(new PerformanceRatio(456, 2, 3, 0));
            when(reportFileModule.getInitialRatios()).thenReturn(ratios.stream().collect(Collectors.toSet()));
        }

        DM20MonitorPerformanceRatioPacket packet = mock(DM20MonitorPerformanceRatioPacket.class);
        List<PerformanceRatio> ratios = new ArrayList<>();
        ratios.add(new PerformanceRatio(123, 1, 1, 0));
        ratios.add(new PerformanceRatio(456, 2, 3, 0));
        when(packet.getRatios()).thenReturn(ratios);

        instance.setReportFileModule(reportFileModule);
        instance.process(packet);
        assertTrue(tableUpdated);

        assertEquals(2, instance.getRowCount());
        assertEquals("Engine #1 (0)", instance.getValueAt(0, 0));
        assertEquals("SPN  123 Transmission Clutch 1 Pressure", instance.getValueAt(0, 1));
        assertEquals(1, instance.getValueAt(0, 2));
        assertEquals(1, instance.getValueAt(0, 3));
        validateBackgroundColor(instance.getBackground(), 0, 0);
        validateBackgroundColor(instance.getBackground(), 0, 1);
        validateBackgroundColor(Color.GREEN, 0, 2);
        validateBackgroundColor(instance.getBackground(), 0, 3);

        assertEquals("Engine #1 (0)", instance.getValueAt(1, 0));
        assertEquals("SPN  456 Unknown", instance.getValueAt(1, 1));
        assertEquals(2, instance.getValueAt(1, 2));
        assertEquals(3, instance.getValueAt(1, 3));
        validateBackgroundColor(instance.getBackground(), 1);

        verify(reportFileModule).getInitialRatios();
    }

    @Test
    public void testProcessDM20TwiceDenominatorBackwards() {
        DM20MonitorPerformanceRatioPacket packet = mock(DM20MonitorPerformanceRatioPacket.class);
        List<PerformanceRatio> ratios = new ArrayList<>();
        ratios.add(new PerformanceRatio(123, 0, 1, 0));
        ratios.add(new PerformanceRatio(456, 2, 3, 0));
        when(packet.getRatios()).thenReturn(ratios);
        when(reportFileModule.getInitialRatios()).thenReturn(ratios.stream().collect(Collectors.toSet()));

        instance.setReportFileModule(reportFileModule);
        instance.process(packet);
        assertTrue(tableUpdated);

        assertEquals(2, instance.getRowCount());
        assertEquals("Engine #1 (0)", instance.getValueAt(0, 0));
        assertEquals("SPN  123 Transmission Clutch 1 Pressure", instance.getValueAt(0, 1));
        assertEquals(0, instance.getValueAt(0, 2));
        assertEquals(1, instance.getValueAt(0, 3));
        validateBackgroundColor(instance.getBackground(), 0);

        assertEquals("Engine #1 (0)", instance.getValueAt(1, 0));
        assertEquals("SPN  456 Unknown", instance.getValueAt(1, 1));
        assertEquals(2, instance.getValueAt(1, 2));
        assertEquals(3, instance.getValueAt(1, 3));
        validateBackgroundColor(instance.getBackground(), 1);

        tableUpdated = false;

        DM20MonitorPerformanceRatioPacket packet2 = mock(DM20MonitorPerformanceRatioPacket.class);
        List<PerformanceRatio> ratios2 = new ArrayList<>();
        ratios2.add(new PerformanceRatio(123, 0, 0, 0));
        ratios2.add(new PerformanceRatio(456, 2, 3, 0));
        when(packet2.getRatios()).thenReturn(ratios2);

        instance.process(packet2);
        assertTrue(tableUpdated);

        assertEquals(2, instance.getRowCount());
        assertEquals("Engine #1 (0)", instance.getValueAt(0, 0));
        assertEquals("SPN  123 Transmission Clutch 1 Pressure", instance.getValueAt(0, 1));
        assertEquals(0, instance.getValueAt(0, 2));
        assertEquals(0, instance.getValueAt(0, 3));
        validateBackgroundColor(instance.getBackground(), 0);

        assertEquals("Engine #1 (0)", instance.getValueAt(1, 0));
        assertEquals("SPN  456 Unknown", instance.getValueAt(1, 1));
        assertEquals(2, instance.getValueAt(1, 2));
        assertEquals(3, instance.getValueAt(1, 3));
        validateBackgroundColor(instance.getBackground(), 1);

        verify(reportFileModule).getInitialRatios();
    }

    @Test
    public void testProcessDM20TwiceDenominatorForward() {
        DM20MonitorPerformanceRatioPacket packet = mock(DM20MonitorPerformanceRatioPacket.class);
        List<PerformanceRatio> ratios = new ArrayList<>();
        ratios.add(new PerformanceRatio(123, 0, 1, 0));
        ratios.add(new PerformanceRatio(456, 2, 3, 0));
        when(packet.getRatios()).thenReturn(ratios);
        when(reportFileModule.getInitialRatios()).thenReturn(ratios.stream().collect(Collectors.toSet()));

        instance.setReportFileModule(reportFileModule);
        instance.process(packet);
        assertTrue(tableUpdated);

        assertEquals(2, instance.getRowCount());
        assertEquals("Engine #1 (0)", instance.getValueAt(0, 0));
        assertEquals("SPN  123 Transmission Clutch 1 Pressure", instance.getValueAt(0, 1));
        assertEquals(0, instance.getValueAt(0, 2));
        assertEquals(1, instance.getValueAt(0, 3));
        validateBackgroundColor(instance.getBackground(), 0);

        assertEquals("Engine #1 (0)", instance.getValueAt(1, 0));
        assertEquals("SPN  456 Unknown", instance.getValueAt(1, 1));
        assertEquals(2, instance.getValueAt(1, 2));
        assertEquals(3, instance.getValueAt(1, 3));
        validateBackgroundColor(instance.getBackground(), 1);

        tableUpdated = false;

        DM20MonitorPerformanceRatioPacket packet2 = mock(DM20MonitorPerformanceRatioPacket.class);
        List<PerformanceRatio> ratios2 = new ArrayList<>();
        ratios2.add(new PerformanceRatio(123, 0, 2, 0));
        ratios2.add(new PerformanceRatio(456, 2, 3, 0));
        when(packet2.getRatios()).thenReturn(ratios2);

        instance.process(packet2);
        assertTrue(tableUpdated);

        assertEquals(2, instance.getRowCount());
        assertEquals("Engine #1 (0)", instance.getValueAt(0, 0));
        assertEquals("SPN  123 Transmission Clutch 1 Pressure", instance.getValueAt(0, 1));
        assertEquals(0, instance.getValueAt(0, 2));
        assertEquals(2, instance.getValueAt(0, 3));
        validateBackgroundColor(instance.getBackground(), 0);

        assertEquals("Engine #1 (0)", instance.getValueAt(1, 0));
        assertEquals("SPN  456 Unknown", instance.getValueAt(1, 1));
        assertEquals(2, instance.getValueAt(1, 2));
        assertEquals(3, instance.getValueAt(1, 3));
        validateBackgroundColor(instance.getBackground(), 1);

        verify(reportFileModule).getInitialRatios();
    }

    @Test
    public void testProcessDM20TwiceNoUpdate() {
        DM20MonitorPerformanceRatioPacket packet = mock(DM20MonitorPerformanceRatioPacket.class);
        List<PerformanceRatio> ratios = new ArrayList<>();
        ratios.add(new PerformanceRatio(123, 0, 1, 0));
        ratios.add(new PerformanceRatio(456, 2, 3, 0));
        when(packet.getRatios()).thenReturn(ratios);
        when(reportFileModule.getInitialRatios()).thenReturn(ratios.stream().collect(Collectors.toSet()));

        instance.setReportFileModule(reportFileModule);
        instance.process(packet);
        assertTrue(tableUpdated);

        assertEquals(2, instance.getRowCount());
        assertEquals("Engine #1 (0)", instance.getValueAt(0, 0));
        assertEquals("SPN  123 Transmission Clutch 1 Pressure", instance.getValueAt(0, 1));
        assertEquals(0, instance.getValueAt(0, 2));
        assertEquals(1, instance.getValueAt(0, 3));
        validateBackgroundColor(instance.getBackground(), 0);

        assertEquals("Engine #1 (0)", instance.getValueAt(1, 0));
        assertEquals("SPN  456 Unknown", instance.getValueAt(1, 1));
        assertEquals(2, instance.getValueAt(1, 2));
        assertEquals(3, instance.getValueAt(1, 3));
        validateBackgroundColor(instance.getBackground(), 1);

        tableUpdated = false;

        instance.process(packet);
        assertFalse(tableUpdated);

        assertEquals(2, instance.getRowCount());
        assertEquals("Engine #1 (0)", instance.getValueAt(0, 0));
        assertEquals("SPN  123 Transmission Clutch 1 Pressure", instance.getValueAt(0, 1));
        assertEquals(0, instance.getValueAt(0, 2));
        assertEquals(1, instance.getValueAt(0, 3));
        validateBackgroundColor(instance.getBackground(), 0);

        assertEquals("Engine #1 (0)", instance.getValueAt(1, 0));
        assertEquals("SPN  456 Unknown", instance.getValueAt(1, 1));
        assertEquals(2, instance.getValueAt(1, 2));
        assertEquals(3, instance.getValueAt(1, 3));
        validateBackgroundColor(instance.getBackground(), 1);

        verify(reportFileModule).getInitialRatios();
    }

    @Test
    public void testProcessDM20TwiceWithUpdateNumeratorBackwards() {
        DM20MonitorPerformanceRatioPacket packet = mock(DM20MonitorPerformanceRatioPacket.class);
        List<PerformanceRatio> ratios = new ArrayList<>();
        ratios.add(new PerformanceRatio(123, 0, 1, 0));
        ratios.add(new PerformanceRatio(456, 2, 3, 0));
        when(packet.getRatios()).thenReturn(ratios);
        when(reportFileModule.getInitialRatios()).thenReturn(ratios.stream().collect(Collectors.toSet()));

        instance.setReportFileModule(reportFileModule);
        instance.process(packet);
        assertTrue(tableUpdated);

        assertEquals(2, instance.getRowCount());
        assertEquals("Engine #1 (0)", instance.getValueAt(0, 0));
        assertEquals("SPN  123 Transmission Clutch 1 Pressure", instance.getValueAt(0, 1));
        assertEquals(0, instance.getValueAt(0, 2));
        assertEquals(1, instance.getValueAt(0, 3));
        validateBackgroundColor(instance.getBackground(), 0);

        assertEquals("Engine #1 (0)", instance.getValueAt(1, 0));
        assertEquals("SPN  456 Unknown", instance.getValueAt(1, 1));
        assertEquals(2, instance.getValueAt(1, 2));
        assertEquals(3, instance.getValueAt(1, 3));
        validateBackgroundColor(instance.getBackground(), 1);

        tableUpdated = false;

        DM20MonitorPerformanceRatioPacket packet2 = mock(DM20MonitorPerformanceRatioPacket.class);
        List<PerformanceRatio> ratios2 = new ArrayList<>();
        ratios2.add(new PerformanceRatio(123, -1, 1, 0));
        ratios2.add(new PerformanceRatio(456, 2, 3, 0));
        when(packet2.getRatios()).thenReturn(ratios2);

        instance.process(packet2);
        assertTrue(tableUpdated);

        assertEquals(2, instance.getRowCount());
        assertEquals("Engine #1 (0)", instance.getValueAt(0, 0));
        assertEquals("SPN  123 Transmission Clutch 1 Pressure", instance.getValueAt(0, 1));
        assertEquals(-1, instance.getValueAt(0, 2));
        assertEquals(1, instance.getValueAt(0, 3));
        validateBackgroundColor(Color.RED, 0);

        assertEquals("Engine #1 (0)", instance.getValueAt(1, 0));
        assertEquals("SPN  456 Unknown", instance.getValueAt(1, 1));
        assertEquals(2, instance.getValueAt(1, 2));
        assertEquals(3, instance.getValueAt(1, 3));
        validateBackgroundColor(instance.getBackground(), 1);

        verify(reportFileModule).getInitialRatios();
    }

    @Test
    public void testProcessDM20TwiceWithUpdateNumeratorForward() {
        DM20MonitorPerformanceRatioPacket packet = mock(DM20MonitorPerformanceRatioPacket.class);
        List<PerformanceRatio> ratios = new ArrayList<>();
        ratios.add(new PerformanceRatio(123, 0, 1, 0));
        ratios.add(new PerformanceRatio(456, 2, 3, 0));
        when(packet.getRatios()).thenReturn(ratios);
        when(reportFileModule.getInitialRatios()).thenReturn(ratios.stream().collect(Collectors.toSet()));

        instance.setReportFileModule(reportFileModule);
        instance.process(packet);
        assertTrue(tableUpdated);

        assertEquals(2, instance.getRowCount());
        assertEquals("Engine #1 (0)", instance.getValueAt(0, 0));
        assertEquals("SPN  123 Transmission Clutch 1 Pressure", instance.getValueAt(0, 1));
        assertEquals(0, instance.getValueAt(0, 2));
        assertEquals(1, instance.getValueAt(0, 3));
        validateBackgroundColor(instance.getBackground(), 0);

        assertEquals("Engine #1 (0)", instance.getValueAt(1, 0));
        assertEquals("SPN  456 Unknown", instance.getValueAt(1, 1));
        assertEquals(2, instance.getValueAt(1, 2));
        assertEquals(3, instance.getValueAt(1, 3));
        validateBackgroundColor(instance.getBackground(), 1);

        tableUpdated = false;

        DM20MonitorPerformanceRatioPacket packet2 = mock(DM20MonitorPerformanceRatioPacket.class);
        List<PerformanceRatio> ratios2 = new ArrayList<>();
        ratios2.add(new PerformanceRatio(123, 1, 1, 0));
        ratios2.add(new PerformanceRatio(456, 2, 3, 0));
        when(packet2.getRatios()).thenReturn(ratios2);

        instance.process(packet2);
        assertTrue(tableUpdated);

        assertEquals(2, instance.getRowCount());
        assertEquals("Engine #1 (0)", instance.getValueAt(0, 0));
        assertEquals("SPN  123 Transmission Clutch 1 Pressure", instance.getValueAt(0, 1));
        assertEquals(1, instance.getValueAt(0, 2));
        assertEquals(1, instance.getValueAt(0, 3));
        validateBackgroundColor(Color.GREEN, 0);

        assertEquals("Engine #1 (0)", instance.getValueAt(1, 0));
        assertEquals("SPN  456 Unknown", instance.getValueAt(1, 1));
        assertEquals(2, instance.getValueAt(1, 2));
        assertEquals(3, instance.getValueAt(1, 3));
        validateBackgroundColor(instance.getBackground(), 1);

        verify(reportFileModule).getInitialRatios();
    }

    @Test
    public void testProcessDM20WithNewRatio() {
        {
            List<PerformanceRatio> ratios = new ArrayList<>();
            ratios.add(new PerformanceRatio(123, 1, 1, 0));
            when(reportFileModule.getInitialRatios()).thenReturn(ratios.stream().collect(Collectors.toSet()));
        }

        DM20MonitorPerformanceRatioPacket packet = mock(DM20MonitorPerformanceRatioPacket.class);
        List<PerformanceRatio> ratios = new ArrayList<>();
        ratios.add(new PerformanceRatio(123, 1, 1, 0));
        ratios.add(new PerformanceRatio(456, 2, 3, 0));
        when(packet.getRatios()).thenReturn(ratios);

        instance.setReportFileModule(reportFileModule);
        instance.process(packet);
        assertTrue(tableUpdated);

        assertEquals(2, instance.getRowCount());
        assertEquals("Engine #1 (0)", instance.getValueAt(0, 0));
        assertEquals("SPN  123 Transmission Clutch 1 Pressure", instance.getValueAt(0, 1));
        assertEquals(1, instance.getValueAt(0, 2));
        assertEquals(1, instance.getValueAt(0, 3));
        validateBackgroundColor(instance.getBackground(), 0);

        assertEquals("Engine #1 (0)", instance.getValueAt(1, 0));
        assertEquals("SPN  456 Unknown", instance.getValueAt(1, 1));
        assertEquals(2, instance.getValueAt(1, 2));
        assertEquals(3, instance.getValueAt(1, 3));
        validateBackgroundColor(instance.getBackground(), 1, 0);
        validateBackgroundColor(instance.getBackground(), 1, 1);
        validateBackgroundColor(Color.RED, 1, 2);
        validateBackgroundColor(instance.getBackground(), 1, 3);

        verify(reportFileModule).getInitialRatios();
    }

    @Test
    public void testProcessIgnoresOtherPackets() {
        ParsedPacket packet = mock(ParsedPacket.class);

        instance.process(packet);

        assertFalse(tableUpdated);
        verifyZeroInteractions(packet);
    }

    private void validateBackgroundColor(Color expectedColor, int row) {
        for (int col = 0; col < instance.getColumnCount(); col++) {
            validateBackgroundColor(expectedColor, row, col);
        }
    }

    private void validateBackgroundColor(Color expectedColor, int row, int col) {
        TableCellRenderer renderer = instance.getCellRenderer(row, col);
        Component component = instance.prepareRenderer(renderer, row, col);
        assertEquals(expectedColor, component.getBackground());
    }

}
