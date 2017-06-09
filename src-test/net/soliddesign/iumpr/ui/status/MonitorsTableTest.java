/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.ui.status;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;

import org.junit.Before;
import org.junit.Test;

import net.soliddesign.iumpr.bus.j1939.packets.DM26TripDiagnosticReadinessPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM5DiagnosticReadinessPacket;
import net.soliddesign.iumpr.bus.j1939.packets.MonitoredSystem;
import net.soliddesign.iumpr.bus.j1939.packets.MonitoredSystem.Status;
import net.soliddesign.iumpr.bus.j1939.packets.ParsedPacket;

/**
 * Unit tests for the {@link MonitorsTable} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class MonitorsTableTest {

    private MonitorsTable instance;

    private TableModelListener listener;
    private boolean rowsAdded;
    private boolean tableUpdated;

    private Color getBackgroundColor(int row, int col) {
        TableCellRenderer renderer = instance.getCellRenderer(row, col);
        Component component = instance.prepareRenderer(renderer, row, col);
        return component.getBackground();
    }

    @Before
    public void setUp() throws Exception {
        listener = e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                tableUpdated = true;
            } else if (e.getType() == TableModelEvent.INSERT) {
                rowsAdded = true;
            }
        };
        instance = new MonitorsTable();
        instance.getModel().addTableModelListener(listener);
    }

    @Test
    public void testInitialSetup() {
        assertEquals("Vehicle Monitors", instance.getColumnName(0));
        assertEquals("Trip Status (DM26)", instance.getColumnName(1));
        assertEquals("Overall Status (DM5)", instance.getColumnName(2));

        assertEquals(3, instance.getColumnCount());
        assertEquals(0, instance.getRowCount());

        assertTrue(instance.getAutoCreateColumnsFromModel());
        assertTrue(instance.getAutoCreateRowSorter());
    }

    @Test
    public void testProcessDM26() {
        DM26TripDiagnosticReadinessPacket packet = mock(DM26TripDiagnosticReadinessPacket.class);
        when(packet.getSourceAddress()).thenReturn(0);

        List<MonitoredSystem> contSystems = new ArrayList<>();
        contSystems.add(new MonitoredSystem("System1", Status.COMPLETE, 0, 1));
        contSystems.add(new MonitoredSystem("System2", Status.NOT_COMPLETE, 0, 2));
        contSystems.add(new MonitoredSystem("System3", Status.NOT_SUPPORTED, 0, 3));
        when(packet.getContinuouslyMonitoredSystems()).thenReturn(contSystems);

        List<MonitoredSystem> nonContSystems = new ArrayList<>();
        nonContSystems.add(new MonitoredSystem("System4", Status.COMPLETE, 0, 4));
        nonContSystems.add(new MonitoredSystem("System5", Status.NOT_COMPLETE, 0, 5));
        nonContSystems.add(new MonitoredSystem("System6", Status.NOT_SUPPORTED, 0, 6));
        when(packet.getNonContinuouslyMonitoredSystems()).thenReturn(nonContSystems);

        instance.process(packet);

        assertEquals(6, instance.getRowCount());
        assertEquals("System1", instance.getValueAt(0, 0));
        assertEquals(Status.COMPLETE, instance.getValueAt(0, 1));
        assertEquals(Color.GREEN, getBackgroundColor(0, 1));
        assertEquals(null, instance.getValueAt(0, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(0, 2));

        assertEquals("System2", instance.getValueAt(1, 0));
        assertEquals(Status.NOT_COMPLETE, instance.getValueAt(1, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(1, 1));
        assertEquals(null, instance.getValueAt(1, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(1, 2));

        assertEquals("System3", instance.getValueAt(2, 0));
        assertEquals(Status.NOT_SUPPORTED, instance.getValueAt(2, 1));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(2, 1));
        assertEquals(null, instance.getValueAt(2, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(2, 2));

        assertEquals("System4", instance.getValueAt(3, 0));
        assertEquals(Status.COMPLETE, instance.getValueAt(3, 1));
        assertEquals(Color.GREEN, getBackgroundColor(3, 1));
        assertEquals(null, instance.getValueAt(3, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(3, 2));

        assertEquals("System5", instance.getValueAt(4, 0));
        assertEquals(Status.NOT_COMPLETE, instance.getValueAt(4, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(4, 1));
        assertEquals(null, instance.getValueAt(4, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(4, 2));

        assertEquals("System6", instance.getValueAt(5, 0));
        assertEquals(Status.NOT_SUPPORTED, instance.getValueAt(5, 1));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(5, 1));
        assertEquals(null, instance.getValueAt(5, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(5, 2));

        assertTrue(tableUpdated);
        assertFalse(rowsAdded);
    }

    @Test
    public void testProcessDM26AndDM5() {
        DM26TripDiagnosticReadinessPacket dm26Packet = mock(DM26TripDiagnosticReadinessPacket.class);
        {
            when(dm26Packet.getSourceAddress()).thenReturn(0);

            List<MonitoredSystem> contSystems = new ArrayList<>();
            contSystems.add(new MonitoredSystem("System1", Status.COMPLETE, 0, 1));
            contSystems.add(new MonitoredSystem("System2", Status.NOT_COMPLETE, 0, 2));
            contSystems.add(new MonitoredSystem("System3", Status.NOT_SUPPORTED, 0, 3));
            when(dm26Packet.getContinuouslyMonitoredSystems()).thenReturn(contSystems);

            List<MonitoredSystem> nonContSystems = new ArrayList<>();
            nonContSystems.add(new MonitoredSystem("System4", Status.COMPLETE, 0, 4));
            nonContSystems.add(new MonitoredSystem("System5", Status.NOT_COMPLETE, 0, 5));
            nonContSystems.add(new MonitoredSystem("System6", Status.NOT_SUPPORTED, 0, 6));
            when(dm26Packet.getNonContinuouslyMonitoredSystems()).thenReturn(nonContSystems);
        }
        instance.process(dm26Packet);

        DM5DiagnosticReadinessPacket dm5Packet = mock(DM5DiagnosticReadinessPacket.class);
        {
            when(dm5Packet.getSourceAddress()).thenReturn(0);

            List<MonitoredSystem> contSystems = new ArrayList<>();
            contSystems.add(new MonitoredSystem("System1", Status.COMPLETE, 0, 1));
            contSystems.add(new MonitoredSystem("System2", Status.NOT_COMPLETE, 0, 2));
            contSystems.add(new MonitoredSystem("System3", Status.NOT_SUPPORTED, 0, 3));
            when(dm5Packet.getContinuouslyMonitoredSystems()).thenReturn(contSystems);

            List<MonitoredSystem> nonContSystems = new ArrayList<>();
            nonContSystems.add(new MonitoredSystem("System4", Status.COMPLETE, 0, 4));
            nonContSystems.add(new MonitoredSystem("System5", Status.NOT_COMPLETE, 0, 5));
            nonContSystems.add(new MonitoredSystem("System6", Status.NOT_SUPPORTED, 0, 6));
            when(dm5Packet.getNonContinuouslyMonitoredSystems()).thenReturn(nonContSystems);
        }
        instance.process(dm5Packet);
        assertEquals(6, instance.getRowCount());
        assertEquals("System1", instance.getValueAt(0, 0));
        assertEquals(Status.COMPLETE, instance.getValueAt(0, 1));
        assertEquals(Color.GREEN, getBackgroundColor(0, 1));
        assertEquals(Status.COMPLETE, instance.getValueAt(0, 2));
        assertEquals(Color.GREEN, getBackgroundColor(0, 2));

        assertEquals("System2", instance.getValueAt(1, 0));
        assertEquals(Status.NOT_COMPLETE, instance.getValueAt(1, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(1, 1));
        assertEquals(Status.NOT_COMPLETE, instance.getValueAt(1, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(1, 2));

        assertEquals("System3", instance.getValueAt(2, 0));
        assertEquals(Status.NOT_SUPPORTED, instance.getValueAt(2, 1));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(2, 1));
        assertEquals(Status.NOT_SUPPORTED, instance.getValueAt(2, 2));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(2, 2));

        assertEquals("System4", instance.getValueAt(3, 0));
        assertEquals(Status.COMPLETE, instance.getValueAt(3, 1));
        assertEquals(Color.GREEN, getBackgroundColor(3, 1));
        assertEquals(Status.COMPLETE, instance.getValueAt(3, 2));
        assertEquals(Color.GREEN, getBackgroundColor(3, 2));

        assertEquals("System5", instance.getValueAt(4, 0));
        assertEquals(Status.NOT_COMPLETE, instance.getValueAt(4, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(4, 1));
        assertEquals(Status.NOT_COMPLETE, instance.getValueAt(4, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(4, 2));

        assertEquals("System6", instance.getValueAt(5, 0));
        assertEquals(Status.NOT_SUPPORTED, instance.getValueAt(5, 1));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(5, 1));
        assertEquals(Status.NOT_SUPPORTED, instance.getValueAt(5, 2));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(5, 2));

        assertTrue(tableUpdated);
        assertFalse(rowsAdded);

        for (int row = 0; row < instance.getRowCount(); row++) {
            for (int col = 0; col < instance.getColumnCount(); col++) {
                assertFalse(instance.isCellEditable(row, col));
            }
        }
    }

    @Test
    public void testProcessDM5() {
        DM5DiagnosticReadinessPacket packet = mock(DM5DiagnosticReadinessPacket.class);
        when(packet.getSourceAddress()).thenReturn(0);

        List<MonitoredSystem> contSystems = new ArrayList<>();
        contSystems.add(new MonitoredSystem("System1", Status.COMPLETE, 0, 1));
        contSystems.add(new MonitoredSystem("System2", Status.NOT_COMPLETE, 0, 2));
        contSystems.add(new MonitoredSystem("System3", Status.NOT_SUPPORTED, 0, 3));
        when(packet.getContinuouslyMonitoredSystems()).thenReturn(contSystems);

        List<MonitoredSystem> nonContSystems = new ArrayList<>();
        nonContSystems.add(new MonitoredSystem("System4", Status.COMPLETE, 0, 4));
        nonContSystems.add(new MonitoredSystem("System5", Status.NOT_COMPLETE, 0, 5));
        nonContSystems.add(new MonitoredSystem("System6", Status.NOT_SUPPORTED, 0, 6));
        when(packet.getNonContinuouslyMonitoredSystems()).thenReturn(nonContSystems);

        instance.process(packet);

        assertEquals(6, instance.getRowCount());
        assertEquals("System1", instance.getValueAt(0, 0));
        assertEquals(null, instance.getValueAt(0, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(0, 1));
        assertEquals(Status.COMPLETE, instance.getValueAt(0, 2));
        assertEquals(Color.GREEN, getBackgroundColor(0, 2));

        assertEquals("System2", instance.getValueAt(1, 0));
        assertEquals(null, instance.getValueAt(1, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(1, 1));
        assertEquals(Status.NOT_COMPLETE, instance.getValueAt(1, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(1, 2));

        assertEquals("System3", instance.getValueAt(2, 0));
        assertEquals(null, instance.getValueAt(2, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(2, 1));
        assertEquals(Status.NOT_SUPPORTED, instance.getValueAt(2, 2));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(2, 2));

        assertEquals("System4", instance.getValueAt(3, 0));
        assertEquals(null, instance.getValueAt(3, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(3, 1));
        assertEquals(Status.COMPLETE, instance.getValueAt(3, 2));
        assertEquals(Color.GREEN, getBackgroundColor(3, 2));

        assertEquals("System5", instance.getValueAt(4, 0));
        assertEquals(null, instance.getValueAt(4, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(4, 1));
        assertEquals(Status.NOT_COMPLETE, instance.getValueAt(4, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(4, 2));

        assertEquals("System6", instance.getValueAt(5, 0));
        assertEquals(null, instance.getValueAt(5, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(5, 1));
        assertEquals(Status.NOT_SUPPORTED, instance.getValueAt(5, 2));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(5, 2));

        assertTrue(tableUpdated);
        assertFalse(rowsAdded);
    }

    @Test
    public void testProcessDM5AndDM26() {
        DM5DiagnosticReadinessPacket dm5Packet = mock(DM5DiagnosticReadinessPacket.class);
        {
            when(dm5Packet.getSourceAddress()).thenReturn(0);

            List<MonitoredSystem> contSystems = new ArrayList<>();
            contSystems.add(new MonitoredSystem("System1", Status.COMPLETE, 0, 1));
            contSystems.add(new MonitoredSystem("System2", Status.NOT_COMPLETE, 0, 2));
            contSystems.add(new MonitoredSystem("System3", Status.NOT_SUPPORTED, 0, 3));
            when(dm5Packet.getContinuouslyMonitoredSystems()).thenReturn(contSystems);

            List<MonitoredSystem> nonContSystems = new ArrayList<>();
            nonContSystems.add(new MonitoredSystem("System4", Status.COMPLETE, 0, 4));
            nonContSystems.add(new MonitoredSystem("System5", Status.NOT_COMPLETE, 0, 5));
            nonContSystems.add(new MonitoredSystem("System6", Status.NOT_SUPPORTED, 0, 6));
            when(dm5Packet.getNonContinuouslyMonitoredSystems()).thenReturn(nonContSystems);
        }
        instance.process(dm5Packet);

        DM26TripDiagnosticReadinessPacket dm26Packet = mock(DM26TripDiagnosticReadinessPacket.class);
        {
            when(dm26Packet.getSourceAddress()).thenReturn(0);

            List<MonitoredSystem> contSystems = new ArrayList<>();
            contSystems.add(new MonitoredSystem("System1", Status.COMPLETE, 0, 1));
            contSystems.add(new MonitoredSystem("System2", Status.NOT_COMPLETE, 0, 2));
            contSystems.add(new MonitoredSystem("System3", Status.NOT_SUPPORTED, 0, 3));
            when(dm26Packet.getContinuouslyMonitoredSystems()).thenReturn(contSystems);

            List<MonitoredSystem> nonContSystems = new ArrayList<>();
            nonContSystems.add(new MonitoredSystem("System4", Status.COMPLETE, 0, 4));
            nonContSystems.add(new MonitoredSystem("System5", Status.NOT_COMPLETE, 0, 5));
            nonContSystems.add(new MonitoredSystem("System6", Status.NOT_SUPPORTED, 0, 6));
            when(dm26Packet.getNonContinuouslyMonitoredSystems()).thenReturn(nonContSystems);
        }
        instance.process(dm26Packet);

        assertEquals(6, instance.getRowCount());
        assertEquals("System1", instance.getValueAt(0, 0));
        assertEquals(Status.COMPLETE, instance.getValueAt(0, 1));
        assertEquals(Color.GREEN, getBackgroundColor(0, 1));
        assertEquals(Status.COMPLETE, instance.getValueAt(0, 2));
        assertEquals(Color.GREEN, getBackgroundColor(0, 2));

        assertEquals("System2", instance.getValueAt(1, 0));
        assertEquals(Status.NOT_COMPLETE, instance.getValueAt(1, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(1, 1));
        assertEquals(Status.NOT_COMPLETE, instance.getValueAt(1, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(1, 2));

        assertEquals("System3", instance.getValueAt(2, 0));
        assertEquals(Status.NOT_SUPPORTED, instance.getValueAt(2, 1));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(2, 1));
        assertEquals(Status.NOT_SUPPORTED, instance.getValueAt(2, 2));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(2, 2));

        assertEquals("System4", instance.getValueAt(3, 0));
        assertEquals(Status.COMPLETE, instance.getValueAt(3, 1));
        assertEquals(Color.GREEN, getBackgroundColor(3, 1));
        assertEquals(Status.COMPLETE, instance.getValueAt(3, 2));
        assertEquals(Color.GREEN, getBackgroundColor(3, 2));

        assertEquals("System5", instance.getValueAt(4, 0));
        assertEquals(Status.NOT_COMPLETE, instance.getValueAt(4, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(4, 1));
        assertEquals(Status.NOT_COMPLETE, instance.getValueAt(4, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(4, 2));

        assertEquals("System6", instance.getValueAt(5, 0));
        assertEquals(Status.NOT_SUPPORTED, instance.getValueAt(5, 1));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(5, 1));
        assertEquals(Status.NOT_SUPPORTED, instance.getValueAt(5, 2));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(5, 2));

        assertTrue(tableUpdated);
        assertFalse(rowsAdded);
    }

    @Test
    public void testProcessIgnoresOtherPackets() {
        ParsedPacket packet = mock(ParsedPacket.class);

        instance.process(packet);

        assertFalse(tableUpdated);
        verifyZeroInteractions(packet);
    }

}
