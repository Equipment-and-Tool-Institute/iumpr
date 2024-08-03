/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.ui.status;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.Component;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;

import org.etools.j1939tools.j1939.packets.DM20MonitorPerformanceRatioPacket;
import org.etools.j1939tools.j1939.packets.DM21DiagnosticReadinessPacket;
import org.etools.j1939tools.j1939.packets.ParsedPacket;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit tests for the {@link InfoTable} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class InfoTableTest {

    private InfoTable instance;

    private TableModelListener listener;
    private boolean tableUpdated;

    @Before
    public void setUp() throws Exception {
        listener = e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                tableUpdated = true;
            }
        };
        instance = new InfoTable();
        instance.getModel().addTableModelListener(listener);
    }

    @Test
    public void testInitialSetup() {
        assertEquals("Name", instance.getColumnName(0));
        assertEquals("Value", instance.getColumnName(1));
        assertEquals("Unit", instance.getColumnName(2));

        assertEquals("Time Since Code Clear from DM21", instance.getValueAt(0, 0));
        assertEquals(null, instance.getValueAt(0, 1));
        assertEquals("Minutes", instance.getValueAt(0, 2));

        assertEquals("Distance Since Code Clear from DM21", instance.getValueAt(1, 0));
        assertEquals(null, instance.getValueAt(1, 1));
        assertEquals("Miles", instance.getValueAt(1, 2));

        assertEquals("Ignition Cycles from DM20", instance.getValueAt(2, 0));
        assertEquals(null, instance.getValueAt(2, 1));
        assertEquals("Cycles", instance.getValueAt(2, 2));

        assertEquals("OBD Monitoring Conditions Encountered from DM20", instance.getValueAt(3, 0));
        assertEquals(null, instance.getValueAt(3, 1));
        assertEquals("Counts", instance.getValueAt(3, 2));

        assertEquals(3, instance.getColumnCount());
        assertEquals(4, instance.getRowCount());

        assertTrue(instance.getAutoCreateColumnsFromModel());
        assertTrue(instance.getAutoCreateRowSorter());

        for (int row = 0; row < instance.getRowCount(); row++) {
            for (int col = 0; col < instance.getColumnCount(); col++) {
                assertFalse(instance.isCellEditable(row, col));
            }
        }
    }

    @Test
    public void testProcessDM20First() {
        DM20MonitorPerformanceRatioPacket packet = mock(DM20MonitorPerformanceRatioPacket.class);
        when(packet.getSourceAddress()).thenReturn(0);
        when(packet.getIgnitionCycles()).thenReturn(100);
        when(packet.getOBDConditionsCount()).thenReturn(15);

        instance.process(packet);

        assertEquals(100.0, instance.getValueAt(2, 1));
        validateRowBackground(instance.getBackground(), 2);
        assertEquals(15.0, instance.getValueAt(3, 1));
        validateRowBackground(instance.getBackground(), 3);
        assertTrue(tableUpdated);
    }

    @Test
    public void testProcessDM20NonEngine() {
        DM20MonitorPerformanceRatioPacket packet1 = mock(DM20MonitorPerformanceRatioPacket.class);
        when(packet1.getSourceAddress()).thenReturn(0);
        when(packet1.getIgnitionCycles()).thenReturn(100);
        when(packet1.getOBDConditionsCount()).thenReturn(15);

        instance.process(packet1);

        assertEquals(100.0, instance.getValueAt(2, 1));
        validateRowBackground(instance.getBackground(), 2);
        assertEquals(15.0, instance.getValueAt(3, 1));
        validateRowBackground(instance.getBackground(), 3);
        assertTrue(tableUpdated);

        tableUpdated = false;

        DM20MonitorPerformanceRatioPacket packet2 = mock(DM20MonitorPerformanceRatioPacket.class);
        when(packet2.getSourceAddress()).thenReturn(55);
        when(packet2.getIgnitionCycles()).thenReturn(101);
        when(packet2.getOBDConditionsCount()).thenReturn(16);

        instance.process(packet2);

        assertEquals(101.0, instance.getValueAt(2, 1));
        validateRowBackground(Color.GREEN, 2);
        assertEquals(16.0, instance.getValueAt(3, 1));
        validateRowBackground(Color.GREEN, 3);
        assertTrue(tableUpdated);

        // Re-process the initial engine packet; values don't change
        instance.process(packet1);
        assertEquals(101.0, instance.getValueAt(2, 1));
        validateRowBackground(Color.GREEN, 2);
        assertEquals(16.0, instance.getValueAt(3, 1));
        validateRowBackground(Color.GREEN, 3);
        assertTrue(tableUpdated);
    }

    @Test
    public void testProcessDM20UpdateNegative() {
        DM20MonitorPerformanceRatioPacket packet1 = mock(DM20MonitorPerformanceRatioPacket.class);
        when(packet1.getSourceAddress()).thenReturn(0);
        when(packet1.getIgnitionCycles()).thenReturn(100);
        when(packet1.getOBDConditionsCount()).thenReturn(15);

        instance.process(packet1);

        assertEquals(100.0, instance.getValueAt(2, 1));
        validateRowBackground(instance.getBackground(), 2);
        assertEquals(15.0, instance.getValueAt(3, 1));
        validateRowBackground(instance.getBackground(), 3);
        assertTrue(tableUpdated);

        tableUpdated = false;

        DM20MonitorPerformanceRatioPacket packet2 = mock(DM20MonitorPerformanceRatioPacket.class);
        when(packet2.getSourceAddress()).thenReturn(0);
        when(packet2.getIgnitionCycles()).thenReturn(50);
        when(packet2.getOBDConditionsCount()).thenReturn(5);

        instance.process(packet2);

        assertEquals(50.0, instance.getValueAt(2, 1));
        validateRowBackground(Color.RED, 2);
        assertEquals(5.0, instance.getValueAt(3, 1));
        validateRowBackground(Color.RED, 3);
        assertTrue(tableUpdated);
    }

    @Test
    public void testProcessDM20UpdateNoChange() {
        DM20MonitorPerformanceRatioPacket packet1 = mock(DM20MonitorPerformanceRatioPacket.class);
        when(packet1.getSourceAddress()).thenReturn(0);
        when(packet1.getIgnitionCycles()).thenReturn(100);
        when(packet1.getOBDConditionsCount()).thenReturn(15);

        instance.process(packet1);

        assertEquals(100.0, instance.getValueAt(2, 1));
        validateRowBackground(instance.getBackground(), 2);
        assertEquals(15.0, instance.getValueAt(3, 1));
        validateRowBackground(instance.getBackground(), 3);
        assertTrue(tableUpdated);

        tableUpdated = false;

        DM20MonitorPerformanceRatioPacket packet2 = mock(DM20MonitorPerformanceRatioPacket.class);
        when(packet2.getSourceAddress()).thenReturn(0);
        when(packet2.getIgnitionCycles()).thenReturn(100);
        when(packet2.getOBDConditionsCount()).thenReturn(15);

        instance.process(packet2);

        assertEquals(100.0, instance.getValueAt(2, 1));
        validateRowBackground(instance.getBackground(), 2);
        assertEquals(15.0, instance.getValueAt(3, 1));
        validateRowBackground(instance.getBackground(), 3);
        assertFalse(tableUpdated);
    }

    @Test
    public void testProcessDM20UpdatePositive() {
        DM20MonitorPerformanceRatioPacket packet1 = mock(DM20MonitorPerformanceRatioPacket.class);
        when(packet1.getSourceAddress()).thenReturn(0);
        when(packet1.getIgnitionCycles()).thenReturn(100);
        when(packet1.getOBDConditionsCount()).thenReturn(15);

        instance.process(packet1);

        assertEquals(100.0, instance.getValueAt(2, 1));
        validateRowBackground(instance.getBackground(), 2);
        assertEquals(15.0, instance.getValueAt(3, 1));
        validateRowBackground(instance.getBackground(), 3);
        assertTrue(tableUpdated);

        tableUpdated = false;

        DM20MonitorPerformanceRatioPacket packet2 = mock(DM20MonitorPerformanceRatioPacket.class);
        when(packet2.getSourceAddress()).thenReturn(0);
        when(packet2.getIgnitionCycles()).thenReturn(200);
        when(packet2.getOBDConditionsCount()).thenReturn(30);

        instance.process(packet2);

        assertEquals(200.0, instance.getValueAt(2, 1));
        validateRowBackground(Color.GREEN, 2);
        assertEquals(30.0, instance.getValueAt(3, 1));
        validateRowBackground(Color.GREEN, 3);
        assertTrue(tableUpdated);
    }

    @Test
    public void testProcessDM21First() {
        DM21DiagnosticReadinessPacket packet = mock(DM21DiagnosticReadinessPacket.class);
        when(packet.getMilesSinceDTCsCleared()).thenReturn(100.0);
        when(packet.getMinutesSinceDTCsCleared()).thenReturn(15.0);

        instance.process(packet);

        assertEquals(15.0, instance.getValueAt(0, 1));
        validateRowBackground(instance.getBackground(), 0);
        assertEquals(100.0, instance.getValueAt(1, 1));
        validateRowBackground(instance.getBackground(), 1);
        assertTrue(tableUpdated);
    }

    @Test
    public void testProcessDM21UpdateNegative() {
        DM21DiagnosticReadinessPacket packet1 = mock(DM21DiagnosticReadinessPacket.class);
        when(packet1.getMilesSinceDTCsCleared()).thenReturn(100.0);
        when(packet1.getMinutesSinceDTCsCleared()).thenReturn(15.0);

        instance.process(packet1);

        assertEquals(15.0, instance.getValueAt(0, 1));
        validateRowBackground(instance.getBackground(), 0);
        assertEquals(100.0, instance.getValueAt(1, 1));
        validateRowBackground(instance.getBackground(), 1);
        assertTrue(tableUpdated);

        tableUpdated = false;

        DM21DiagnosticReadinessPacket packet2 = mock(DM21DiagnosticReadinessPacket.class);
        when(packet2.getMilesSinceDTCsCleared()).thenReturn(50.0);
        when(packet2.getMinutesSinceDTCsCleared()).thenReturn(10.0);

        instance.process(packet2);

        assertEquals(10.0, instance.getValueAt(0, 1));
        validateRowBackground(Color.RED, 0);
        assertEquals(50.0, instance.getValueAt(1, 1));
        validateRowBackground(Color.RED, 1);
        assertTrue(tableUpdated);
    }

    @Test
    public void testProcessDM21UpdateNoChange() {
        DM21DiagnosticReadinessPacket packet1 = mock(DM21DiagnosticReadinessPacket.class);
        when(packet1.getMilesSinceDTCsCleared()).thenReturn(100.0);
        when(packet1.getMinutesSinceDTCsCleared()).thenReturn(15.0);

        instance.process(packet1);

        assertEquals(15.0, instance.getValueAt(0, 1));
        validateRowBackground(instance.getBackground(), 0);
        assertEquals(100.0, instance.getValueAt(1, 1));
        validateRowBackground(instance.getBackground(), 1);
        assertTrue(tableUpdated);

        tableUpdated = false;

        DM21DiagnosticReadinessPacket packet2 = mock(DM21DiagnosticReadinessPacket.class);
        when(packet2.getMilesSinceDTCsCleared()).thenReturn(100.0);
        when(packet2.getMinutesSinceDTCsCleared()).thenReturn(15.0);

        instance.process(packet2);

        assertEquals(15.0, instance.getValueAt(0, 1));
        validateRowBackground(instance.getBackground(), 0);
        assertEquals(100.0, instance.getValueAt(1, 1));
        validateRowBackground(instance.getBackground(), 1);
        assertFalse(tableUpdated);
    }

    @Test
    public void testProcessDM21UpdatePositve() {
        DM21DiagnosticReadinessPacket packet1 = mock(DM21DiagnosticReadinessPacket.class);
        when(packet1.getMilesSinceDTCsCleared()).thenReturn(100.0);
        when(packet1.getMinutesSinceDTCsCleared()).thenReturn(15.0);

        instance.process(packet1);

        assertEquals(15.0, instance.getValueAt(0, 1));
        validateRowBackground(instance.getBackground(), 0);
        assertEquals(100.0, instance.getValueAt(1, 1));
        validateRowBackground(instance.getBackground(), 1);
        assertTrue(tableUpdated);

        tableUpdated = false;

        DM21DiagnosticReadinessPacket packet2 = mock(DM21DiagnosticReadinessPacket.class);
        when(packet2.getMilesSinceDTCsCleared()).thenReturn(200.0);
        when(packet2.getMinutesSinceDTCsCleared()).thenReturn(30.0);

        instance.process(packet2);

        assertEquals(30.0, instance.getValueAt(0, 1));
        validateRowBackground(Color.GREEN, 0);
        assertEquals(200.0, instance.getValueAt(1, 1));
        validateRowBackground(Color.GREEN, 1);
        assertTrue(tableUpdated);
    }

    @Test
    public void testProcessIgnoresOtherPackets() {
        ParsedPacket packet = mock(ParsedPacket.class);

        instance.process(packet);

        assertFalse(tableUpdated);
        Mockito.verifyNoMoreInteractions(packet);
    }

    private void validateRowBackground(Color expected, int row) {
        for (int col = 0; col < instance.getColumnCount(); col++) {
            TableCellRenderer renderer = instance.getCellRenderer(row, col);
            Component component = instance.prepareRenderer(renderer, row, col);
            assertEquals(expected, component.getBackground());
        }
    }
}
