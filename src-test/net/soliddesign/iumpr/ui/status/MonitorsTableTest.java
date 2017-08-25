/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.ui.status;

import static net.soliddesign.iumpr.bus.j1939.packets.DM26MonitoredSystemStatus.ENABLED_COMPLETE;
import static net.soliddesign.iumpr.bus.j1939.packets.DM26MonitoredSystemStatus.ENABLED_NOT_COMPLETE;
import static net.soliddesign.iumpr.bus.j1939.packets.DM26MonitoredSystemStatus.NOT_ENABLED_COMPLETE;
import static net.soliddesign.iumpr.bus.j1939.packets.DM26MonitoredSystemStatus.NOT_ENABLED_NOT_COMPLETE;
import static net.soliddesign.iumpr.bus.j1939.packets.DM5MonitoredSystemStatus.NOT_SUPPORTED_COMPLETE;
import static net.soliddesign.iumpr.bus.j1939.packets.DM5MonitoredSystemStatus.NOT_SUPPORTED_NOT_COMPLETE;
import static net.soliddesign.iumpr.bus.j1939.packets.DM5MonitoredSystemStatus.SUPPORTED_COMPLETE;
import static net.soliddesign.iumpr.bus.j1939.packets.DM5MonitoredSystemStatus.SUPPORTED_NOT_COMPLETE;
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
        assertEquals("DM26 Enabled", instance.getColumnName(1));
        assertEquals("DM26 Complete", instance.getColumnName(2));
        assertEquals("DM5 Supported", instance.getColumnName(3));
        assertEquals("DM5 Complete", instance.getColumnName(4));

        assertEquals(5, instance.getColumnCount());
        assertEquals(0, instance.getRowCount());

        assertTrue(instance.getAutoCreateColumnsFromModel());
        assertTrue(instance.getAutoCreateRowSorter());
    }

    @Test
    public void testProcessDM26() {
        DM26TripDiagnosticReadinessPacket packet = mock(DM26TripDiagnosticReadinessPacket.class);
        when(packet.getSourceAddress()).thenReturn(0);

        List<MonitoredSystem> contSystems = new ArrayList<>();
        contSystems.add(new MonitoredSystem("System1", ENABLED_COMPLETE, 0, 1));
        contSystems.add(new MonitoredSystem("System2", ENABLED_NOT_COMPLETE, 0, 2));
        contSystems.add(new MonitoredSystem("System3", NOT_ENABLED_COMPLETE, 0, 3));
        contSystems.add(new MonitoredSystem("System4", NOT_ENABLED_NOT_COMPLETE, 0, 4));
        when(packet.getContinuouslyMonitoredSystems()).thenReturn(contSystems);

        List<MonitoredSystem> nonContSystems = new ArrayList<>();
        nonContSystems.add(new MonitoredSystem("System5", ENABLED_COMPLETE, 0, 5));
        nonContSystems.add(new MonitoredSystem("System6", ENABLED_NOT_COMPLETE, 0, 6));
        nonContSystems.add(new MonitoredSystem("System7", NOT_ENABLED_COMPLETE, 0, 7));
        nonContSystems.add(new MonitoredSystem("System8", NOT_ENABLED_NOT_COMPLETE, 0, 8));
        when(packet.getNonContinuouslyMonitoredSystems()).thenReturn(nonContSystems);

        instance.process(packet);

        assertEquals(8, instance.getRowCount());
        assertEquals("System1", instance.getValueAt(0, 0));
        assertEquals(true, instance.getValueAt(0, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(0, 1));
        assertEquals(true, instance.getValueAt(0, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(0, 2));
        assertEquals(null, instance.getValueAt(0, 3));
        assertEquals(instance.getBackground(), getBackgroundColor(0, 3));
        assertEquals(null, instance.getValueAt(0, 4));
        assertEquals(instance.getBackground(), getBackgroundColor(0, 4));

        assertEquals("System2", instance.getValueAt(1, 0));
        assertEquals(true, instance.getValueAt(1, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(1, 1));
        assertEquals(false, instance.getValueAt(1, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(1, 2));
        assertEquals(null, instance.getValueAt(1, 3));
        assertEquals(instance.getBackground(), getBackgroundColor(1, 3));
        assertEquals(null, instance.getValueAt(1, 4));
        assertEquals(instance.getBackground(), getBackgroundColor(1, 4));

        assertEquals("System3", instance.getValueAt(2, 0));
        assertEquals(false, instance.getValueAt(2, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(2, 1));
        assertEquals(true, instance.getValueAt(2, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(2, 2));
        assertEquals(null, instance.getValueAt(2, 3));
        assertEquals(instance.getBackground(), getBackgroundColor(2, 3));
        assertEquals(null, instance.getValueAt(2, 4));
        assertEquals(instance.getBackground(), getBackgroundColor(2, 4));

        assertEquals("System4", instance.getValueAt(3, 0));
        assertEquals(false, instance.getValueAt(3, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(3, 1));
        assertEquals(false, instance.getValueAt(3, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(3, 2));
        assertEquals(null, instance.getValueAt(3, 3));
        assertEquals(instance.getBackground(), getBackgroundColor(3, 3));
        assertEquals(null, instance.getValueAt(3, 4));
        assertEquals(instance.getBackground(), getBackgroundColor(3, 4));

        assertEquals("System5", instance.getValueAt(4, 0));
        assertEquals(true, instance.getValueAt(4, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(4, 1));
        assertEquals(true, instance.getValueAt(4, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(4, 2));
        assertEquals(null, instance.getValueAt(4, 3));
        assertEquals(instance.getBackground(), getBackgroundColor(4, 3));
        assertEquals(null, instance.getValueAt(4, 4));
        assertEquals(instance.getBackground(), getBackgroundColor(4, 4));

        assertEquals("System6", instance.getValueAt(5, 0));
        assertEquals(true, instance.getValueAt(5, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(5, 1));
        assertEquals(false, instance.getValueAt(5, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(5, 2));
        assertEquals(null, instance.getValueAt(5, 3));
        assertEquals(instance.getBackground(), getBackgroundColor(5, 3));
        assertEquals(null, instance.getValueAt(5, 4));
        assertEquals(instance.getBackground(), getBackgroundColor(5, 4));

        assertEquals("System7", instance.getValueAt(6, 0));
        assertEquals(false, instance.getValueAt(6, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(6, 1));
        assertEquals(true, instance.getValueAt(6, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(6, 2));
        assertEquals(null, instance.getValueAt(6, 3));
        assertEquals(instance.getBackground(), getBackgroundColor(6, 3));
        assertEquals(null, instance.getValueAt(6, 4));
        assertEquals(instance.getBackground(), getBackgroundColor(6, 4));

        assertEquals("System8", instance.getValueAt(7, 0));
        assertEquals(false, instance.getValueAt(7, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(7, 1));
        assertEquals(false, instance.getValueAt(7, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(7, 2));
        assertEquals(null, instance.getValueAt(7, 3));
        assertEquals(instance.getBackground(), getBackgroundColor(7, 3));
        assertEquals(null, instance.getValueAt(7, 4));
        assertEquals(instance.getBackground(), getBackgroundColor(7, 4));
        assertTrue(tableUpdated);
        assertFalse(rowsAdded);
    }

    @Test
    public void testProcessDM26AndDM5() {
        DM26TripDiagnosticReadinessPacket dm26Packet = mock(DM26TripDiagnosticReadinessPacket.class);
        {
            when(dm26Packet.getSourceAddress()).thenReturn(0);

            List<MonitoredSystem> contSystems = new ArrayList<>();
            contSystems.add(new MonitoredSystem("System01", NOT_ENABLED_NOT_COMPLETE, 0, 1));
            contSystems.add(new MonitoredSystem("System02", NOT_ENABLED_NOT_COMPLETE, 0, 2));
            contSystems.add(new MonitoredSystem("System03", NOT_ENABLED_NOT_COMPLETE, 0, 3));
            contSystems.add(new MonitoredSystem("System04", NOT_ENABLED_NOT_COMPLETE, 0, 4));
            contSystems.add(new MonitoredSystem("System05", NOT_ENABLED_COMPLETE, 0, 5));
            contSystems.add(new MonitoredSystem("System06", NOT_ENABLED_COMPLETE, 0, 6));
            contSystems.add(new MonitoredSystem("System07", NOT_ENABLED_COMPLETE, 0, 7));
            contSystems.add(new MonitoredSystem("System08", NOT_ENABLED_COMPLETE, 0, 8));
            when(dm26Packet.getContinuouslyMonitoredSystems()).thenReturn(contSystems);

            List<MonitoredSystem> nonContSystems = new ArrayList<>();
            nonContSystems.add(new MonitoredSystem("System09", ENABLED_NOT_COMPLETE, 0, 9));
            nonContSystems.add(new MonitoredSystem("System10", ENABLED_NOT_COMPLETE, 0, 10));
            nonContSystems.add(new MonitoredSystem("System11", ENABLED_NOT_COMPLETE, 0, 11));
            nonContSystems.add(new MonitoredSystem("System12", ENABLED_NOT_COMPLETE, 0, 12));
            nonContSystems.add(new MonitoredSystem("System13", ENABLED_COMPLETE, 0, 13));
            nonContSystems.add(new MonitoredSystem("System14", ENABLED_COMPLETE, 0, 14));
            nonContSystems.add(new MonitoredSystem("System15", ENABLED_COMPLETE, 0, 15));
            nonContSystems.add(new MonitoredSystem("System16", ENABLED_COMPLETE, 0, 16));
            when(dm26Packet.getNonContinuouslyMonitoredSystems()).thenReturn(nonContSystems);
        }
        instance.process(dm26Packet);

        DM5DiagnosticReadinessPacket dm5Packet = mock(DM5DiagnosticReadinessPacket.class);
        {
            when(dm5Packet.getSourceAddress()).thenReturn(0);

            List<MonitoredSystem> contSystems = new ArrayList<>();
            contSystems.add(new MonitoredSystem("System01", NOT_SUPPORTED_NOT_COMPLETE, 0, 1));
            contSystems.add(new MonitoredSystem("System02", NOT_SUPPORTED_COMPLETE, 0, 2));
            contSystems.add(new MonitoredSystem("System03", SUPPORTED_NOT_COMPLETE, 0, 3));
            contSystems.add(new MonitoredSystem("System04", SUPPORTED_COMPLETE, 0, 4));
            contSystems.add(new MonitoredSystem("System05", NOT_SUPPORTED_NOT_COMPLETE, 0, 5));
            contSystems.add(new MonitoredSystem("System06", NOT_SUPPORTED_COMPLETE, 0, 6));
            contSystems.add(new MonitoredSystem("System07", SUPPORTED_NOT_COMPLETE, 0, 7));
            contSystems.add(new MonitoredSystem("System08", SUPPORTED_COMPLETE, 0, 8));
            when(dm5Packet.getContinuouslyMonitoredSystems()).thenReturn(contSystems);

            List<MonitoredSystem> nonContSystems = new ArrayList<>();
            nonContSystems.add(new MonitoredSystem("System09", NOT_SUPPORTED_NOT_COMPLETE, 0, 9));
            nonContSystems.add(new MonitoredSystem("System10", NOT_SUPPORTED_COMPLETE, 0, 10));
            nonContSystems.add(new MonitoredSystem("System11", SUPPORTED_NOT_COMPLETE, 0, 11));
            nonContSystems.add(new MonitoredSystem("System12", SUPPORTED_COMPLETE, 0, 12));
            nonContSystems.add(new MonitoredSystem("System13", NOT_SUPPORTED_NOT_COMPLETE, 0, 13));
            nonContSystems.add(new MonitoredSystem("System14", NOT_SUPPORTED_COMPLETE, 0, 14));
            nonContSystems.add(new MonitoredSystem("System15", SUPPORTED_NOT_COMPLETE, 0, 15));
            nonContSystems.add(new MonitoredSystem("System16", SUPPORTED_COMPLETE, 0, 16));
            when(dm5Packet.getNonContinuouslyMonitoredSystems()).thenReturn(nonContSystems);
        }
        instance.process(dm5Packet);

        assertEquals(16, instance.getRowCount());

        assertEquals("System01", instance.getValueAt(0, 0));
        assertEquals(false, instance.getValueAt(0, 1));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(0, 1));
        assertEquals(false, instance.getValueAt(0, 2));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(0, 2));
        assertEquals(false, instance.getValueAt(0, 3));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(0, 3));
        assertEquals(false, instance.getValueAt(0, 4));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(0, 4));

        assertEquals("System02", instance.getValueAt(1, 0));
        assertEquals(false, instance.getValueAt(1, 1));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(1, 1));
        assertEquals(false, instance.getValueAt(1, 2));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(1, 2));
        assertEquals(false, instance.getValueAt(1, 3));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(1, 3));
        assertEquals(true, instance.getValueAt(1, 4));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(1, 4));

        assertEquals("System03", instance.getValueAt(2, 0));
        assertEquals(false, instance.getValueAt(2, 1));
        assertEquals(Color.RED, getBackgroundColor(2, 1));
        assertEquals(false, instance.getValueAt(2, 2));
        assertEquals(Color.RED, getBackgroundColor(2, 2));
        assertEquals(true, instance.getValueAt(2, 3));
        assertEquals(Color.RED, getBackgroundColor(2, 3));
        assertEquals(false, instance.getValueAt(2, 4));
        assertEquals(Color.RED, getBackgroundColor(2, 4));

        assertEquals("System04", instance.getValueAt(3, 0));
        assertEquals(false, instance.getValueAt(3, 1));
        assertEquals(Color.GREEN, getBackgroundColor(3, 1));
        assertEquals(false, instance.getValueAt(3, 2));
        assertEquals(Color.GREEN, getBackgroundColor(3, 2));
        assertEquals(true, instance.getValueAt(3, 3));
        assertEquals(Color.GREEN, getBackgroundColor(3, 3));
        assertEquals(true, instance.getValueAt(3, 4));
        assertEquals(Color.GREEN, getBackgroundColor(3, 4));

        assertEquals("System05", instance.getValueAt(4, 0));
        assertEquals(false, instance.getValueAt(4, 1));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(4, 1));
        assertEquals(true, instance.getValueAt(4, 2));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(4, 2));
        assertEquals(false, instance.getValueAt(4, 3));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(4, 3));
        assertEquals(false, instance.getValueAt(4, 4));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(4, 4));

        assertEquals("System06", instance.getValueAt(5, 0));
        assertEquals(false, instance.getValueAt(5, 1));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(5, 1));
        assertEquals(true, instance.getValueAt(5, 2));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(5, 2));
        assertEquals(false, instance.getValueAt(5, 3));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(5, 3));
        assertEquals(true, instance.getValueAt(5, 4));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(5, 4));

        assertEquals("System07", instance.getValueAt(6, 0));
        assertEquals(false, instance.getValueAt(6, 1));
        assertEquals(Color.RED, getBackgroundColor(6, 1));
        assertEquals(true, instance.getValueAt(6, 2));
        assertEquals(Color.RED, getBackgroundColor(6, 2));
        assertEquals(true, instance.getValueAt(6, 3));
        assertEquals(Color.RED, getBackgroundColor(6, 3));
        assertEquals(false, instance.getValueAt(6, 4));
        assertEquals(Color.RED, getBackgroundColor(6, 4));

        assertEquals("System08", instance.getValueAt(7, 0));
        assertEquals(false, instance.getValueAt(7, 1));
        assertEquals(Color.GREEN, getBackgroundColor(7, 1));
        assertEquals(true, instance.getValueAt(7, 2));
        assertEquals(Color.GREEN, getBackgroundColor(7, 2));
        assertEquals(true, instance.getValueAt(7, 3));
        assertEquals(Color.GREEN, getBackgroundColor(7, 3));
        assertEquals(true, instance.getValueAt(7, 4));
        assertEquals(Color.GREEN, getBackgroundColor(7, 4));

        assertEquals("System09", instance.getValueAt(8, 0));
        assertEquals(true, instance.getValueAt(8, 1));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(8, 1));
        assertEquals(false, instance.getValueAt(8, 2));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(8, 2));
        assertEquals(false, instance.getValueAt(8, 3));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(8, 3));
        assertEquals(false, instance.getValueAt(8, 4));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(8, 4));

        assertEquals("System10", instance.getValueAt(9, 0));
        assertEquals(true, instance.getValueAt(9, 1));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(9, 1));
        assertEquals(false, instance.getValueAt(9, 2));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(9, 2));
        assertEquals(false, instance.getValueAt(9, 3));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(9, 3));
        assertEquals(true, instance.getValueAt(9, 4));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(9, 4));

        assertEquals("System11", instance.getValueAt(10, 0));
        assertEquals(true, instance.getValueAt(10, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(10, 1));
        assertEquals(false, instance.getValueAt(10, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(10, 2));
        assertEquals(true, instance.getValueAt(10, 3));
        assertEquals(instance.getBackground(), getBackgroundColor(10, 3));
        assertEquals(false, instance.getValueAt(10, 4));
        assertEquals(instance.getBackground(), getBackgroundColor(10, 4));

        assertEquals("System12", instance.getValueAt(11, 0));
        assertEquals(true, instance.getValueAt(11, 1));
        assertEquals(Color.GREEN, getBackgroundColor(11, 1));
        assertEquals(false, instance.getValueAt(11, 2));
        assertEquals(Color.GREEN, getBackgroundColor(11, 2));
        assertEquals(true, instance.getValueAt(11, 3));
        assertEquals(Color.GREEN, getBackgroundColor(11, 3));
        assertEquals(true, instance.getValueAt(11, 4));
        assertEquals(Color.GREEN, getBackgroundColor(11, 4));

        assertEquals("System13", instance.getValueAt(12, 0));
        assertEquals(true, instance.getValueAt(12, 1));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(12, 1));
        assertEquals(true, instance.getValueAt(12, 2));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(12, 2));
        assertEquals(false, instance.getValueAt(12, 3));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(12, 3));
        assertEquals(false, instance.getValueAt(12, 4));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(12, 4));

        assertEquals("System14", instance.getValueAt(13, 0));
        assertEquals(true, instance.getValueAt(13, 1));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(13, 1));
        assertEquals(true, instance.getValueAt(13, 2));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(13, 2));
        assertEquals(false, instance.getValueAt(13, 3));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(13, 3));
        assertEquals(true, instance.getValueAt(13, 4));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(13, 4));

        assertEquals("System15", instance.getValueAt(14, 0));
        assertEquals(true, instance.getValueAt(14, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(14, 1));
        assertEquals(true, instance.getValueAt(14, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(14, 2));
        assertEquals(true, instance.getValueAt(14, 3));
        assertEquals(instance.getBackground(), getBackgroundColor(14, 3));
        assertEquals(false, instance.getValueAt(14, 4));
        assertEquals(instance.getBackground(), getBackgroundColor(14, 4));

        assertEquals("System16", instance.getValueAt(15, 0));
        assertEquals(true, instance.getValueAt(15, 1));
        assertEquals(Color.GREEN, getBackgroundColor(15, 1));
        assertEquals(true, instance.getValueAt(15, 2));
        assertEquals(Color.GREEN, getBackgroundColor(15, 2));
        assertEquals(true, instance.getValueAt(15, 3));
        assertEquals(Color.GREEN, getBackgroundColor(15, 3));
        assertEquals(true, instance.getValueAt(15, 4));
        assertEquals(Color.GREEN, getBackgroundColor(15, 4));

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
        contSystems.add(new MonitoredSystem("System1", SUPPORTED_COMPLETE, 0, 1));
        contSystems.add(new MonitoredSystem("System2", SUPPORTED_NOT_COMPLETE, 0, 2));
        contSystems.add(new MonitoredSystem("System3", NOT_SUPPORTED_COMPLETE, 0, 3));
        contSystems.add(new MonitoredSystem("System4", NOT_SUPPORTED_NOT_COMPLETE, 0, 4));
        when(packet.getContinuouslyMonitoredSystems()).thenReturn(contSystems);

        List<MonitoredSystem> nonContSystems = new ArrayList<>();
        nonContSystems.add(new MonitoredSystem("System5", SUPPORTED_COMPLETE, 0, 5));
        nonContSystems.add(new MonitoredSystem("System6", SUPPORTED_NOT_COMPLETE, 0, 6));
        nonContSystems.add(new MonitoredSystem("System7", NOT_SUPPORTED_COMPLETE, 0, 7));
        nonContSystems.add(new MonitoredSystem("System8", NOT_SUPPORTED_NOT_COMPLETE, 0, 8));
        when(packet.getNonContinuouslyMonitoredSystems()).thenReturn(nonContSystems);

        instance.process(packet);

        assertEquals(8, instance.getRowCount());

        assertEquals("System1", instance.getValueAt(0, 0));
        assertEquals(null, instance.getValueAt(0, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(0, 1));
        assertEquals(null, instance.getValueAt(0, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(0, 2));
        assertEquals(true, instance.getValueAt(0, 3));
        assertEquals(instance.getBackground(), getBackgroundColor(0, 3));
        assertEquals(true, instance.getValueAt(0, 4));
        assertEquals(instance.getBackground(), getBackgroundColor(0, 4));

        assertEquals("System2", instance.getValueAt(1, 0));
        assertEquals(null, instance.getValueAt(1, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(1, 1));
        assertEquals(null, instance.getValueAt(1, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(1, 2));
        assertEquals(true, instance.getValueAt(1, 3));
        assertEquals(instance.getBackground(), getBackgroundColor(1, 3));
        assertEquals(false, instance.getValueAt(1, 4));
        assertEquals(instance.getBackground(), getBackgroundColor(1, 4));

        assertEquals("System3", instance.getValueAt(2, 0));
        assertEquals(null, instance.getValueAt(2, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(2, 1));
        assertEquals(null, instance.getValueAt(2, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(2, 2));
        assertEquals(false, instance.getValueAt(2, 3));
        assertEquals(instance.getBackground(), getBackgroundColor(2, 3));
        assertEquals(true, instance.getValueAt(2, 4));
        assertEquals(instance.getBackground(), getBackgroundColor(2, 4));

        assertEquals("System4", instance.getValueAt(3, 0));
        assertEquals(null, instance.getValueAt(3, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(3, 1));
        assertEquals(null, instance.getValueAt(3, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(3, 2));
        assertEquals(false, instance.getValueAt(3, 3));
        assertEquals(instance.getBackground(), getBackgroundColor(3, 3));
        assertEquals(false, instance.getValueAt(3, 4));
        assertEquals(instance.getBackground(), getBackgroundColor(3, 4));

        assertEquals("System5", instance.getValueAt(4, 0));
        assertEquals(null, instance.getValueAt(4, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(4, 1));
        assertEquals(null, instance.getValueAt(4, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(4, 2));
        assertEquals(true, instance.getValueAt(4, 3));
        assertEquals(instance.getBackground(), getBackgroundColor(4, 3));
        assertEquals(true, instance.getValueAt(4, 4));
        assertEquals(instance.getBackground(), getBackgroundColor(4, 4));

        assertEquals("System6", instance.getValueAt(5, 0));
        assertEquals(null, instance.getValueAt(5, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(5, 1));
        assertEquals(null, instance.getValueAt(5, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(5, 2));
        assertEquals(true, instance.getValueAt(5, 3));
        assertEquals(instance.getBackground(), getBackgroundColor(5, 3));
        assertEquals(false, instance.getValueAt(5, 4));
        assertEquals(instance.getBackground(), getBackgroundColor(5, 4));

        assertEquals("System7", instance.getValueAt(6, 0));
        assertEquals(null, instance.getValueAt(6, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(6, 1));
        assertEquals(null, instance.getValueAt(6, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(6, 2));
        assertEquals(false, instance.getValueAt(6, 3));
        assertEquals(instance.getBackground(), getBackgroundColor(6, 3));
        assertEquals(true, instance.getValueAt(6, 4));
        assertEquals(instance.getBackground(), getBackgroundColor(6, 4));

        assertEquals("System8", instance.getValueAt(7, 0));
        assertEquals(null, instance.getValueAt(7, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(7, 1));
        assertEquals(null, instance.getValueAt(7, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(7, 2));
        assertEquals(false, instance.getValueAt(7, 3));
        assertEquals(instance.getBackground(), getBackgroundColor(7, 3));
        assertEquals(false, instance.getValueAt(7, 4));
        assertEquals(instance.getBackground(), getBackgroundColor(7, 4));

        assertTrue(tableUpdated);
        assertFalse(rowsAdded);
    }

    @Test
    public void testProcessDM5AndDM26() {
        DM5DiagnosticReadinessPacket dm5Packet = mock(DM5DiagnosticReadinessPacket.class);
        {
            when(dm5Packet.getSourceAddress()).thenReturn(0);

            List<MonitoredSystem> contSystems = new ArrayList<>();
            contSystems.add(new MonitoredSystem("System01", NOT_SUPPORTED_NOT_COMPLETE, 0, 1));
            contSystems.add(new MonitoredSystem("System02", NOT_SUPPORTED_COMPLETE, 0, 2));
            contSystems.add(new MonitoredSystem("System03", SUPPORTED_NOT_COMPLETE, 0, 3));
            contSystems.add(new MonitoredSystem("System04", SUPPORTED_COMPLETE, 0, 4));
            contSystems.add(new MonitoredSystem("System05", NOT_SUPPORTED_NOT_COMPLETE, 0, 5));
            contSystems.add(new MonitoredSystem("System06", NOT_SUPPORTED_COMPLETE, 0, 6));
            contSystems.add(new MonitoredSystem("System07", SUPPORTED_NOT_COMPLETE, 0, 7));
            contSystems.add(new MonitoredSystem("System08", SUPPORTED_COMPLETE, 0, 8));
            when(dm5Packet.getContinuouslyMonitoredSystems()).thenReturn(contSystems);

            List<MonitoredSystem> nonContSystems = new ArrayList<>();
            nonContSystems.add(new MonitoredSystem("System09", NOT_SUPPORTED_NOT_COMPLETE, 0, 9));
            nonContSystems.add(new MonitoredSystem("System10", NOT_SUPPORTED_COMPLETE, 0, 10));
            nonContSystems.add(new MonitoredSystem("System11", SUPPORTED_NOT_COMPLETE, 0, 11));
            nonContSystems.add(new MonitoredSystem("System12", SUPPORTED_COMPLETE, 0, 12));
            nonContSystems.add(new MonitoredSystem("System13", NOT_SUPPORTED_NOT_COMPLETE, 0, 13));
            nonContSystems.add(new MonitoredSystem("System14", NOT_SUPPORTED_COMPLETE, 0, 14));
            nonContSystems.add(new MonitoredSystem("System15", SUPPORTED_NOT_COMPLETE, 0, 15));
            nonContSystems.add(new MonitoredSystem("System16", SUPPORTED_COMPLETE, 0, 16));
            when(dm5Packet.getNonContinuouslyMonitoredSystems()).thenReturn(nonContSystems);
        }
        instance.process(dm5Packet);

        DM26TripDiagnosticReadinessPacket dm26Packet = mock(DM26TripDiagnosticReadinessPacket.class);
        {
            when(dm26Packet.getSourceAddress()).thenReturn(0);

            List<MonitoredSystem> contSystems = new ArrayList<>();
            contSystems.add(new MonitoredSystem("System01", NOT_ENABLED_NOT_COMPLETE, 0, 1));
            contSystems.add(new MonitoredSystem("System02", NOT_ENABLED_NOT_COMPLETE, 0, 2));
            contSystems.add(new MonitoredSystem("System03", NOT_ENABLED_NOT_COMPLETE, 0, 3));
            contSystems.add(new MonitoredSystem("System04", NOT_ENABLED_NOT_COMPLETE, 0, 4));
            contSystems.add(new MonitoredSystem("System05", NOT_ENABLED_COMPLETE, 0, 5));
            contSystems.add(new MonitoredSystem("System06", NOT_ENABLED_COMPLETE, 0, 6));
            contSystems.add(new MonitoredSystem("System07", NOT_ENABLED_COMPLETE, 0, 7));
            contSystems.add(new MonitoredSystem("System08", NOT_ENABLED_COMPLETE, 0, 8));
            when(dm26Packet.getContinuouslyMonitoredSystems()).thenReturn(contSystems);

            List<MonitoredSystem> nonContSystems = new ArrayList<>();
            nonContSystems.add(new MonitoredSystem("System09", ENABLED_NOT_COMPLETE, 0, 9));
            nonContSystems.add(new MonitoredSystem("System10", ENABLED_NOT_COMPLETE, 0, 10));
            nonContSystems.add(new MonitoredSystem("System11", ENABLED_NOT_COMPLETE, 0, 11));
            nonContSystems.add(new MonitoredSystem("System12", ENABLED_NOT_COMPLETE, 0, 12));
            nonContSystems.add(new MonitoredSystem("System13", ENABLED_COMPLETE, 0, 13));
            nonContSystems.add(new MonitoredSystem("System14", ENABLED_COMPLETE, 0, 14));
            nonContSystems.add(new MonitoredSystem("System15", ENABLED_COMPLETE, 0, 15));
            nonContSystems.add(new MonitoredSystem("System16", ENABLED_COMPLETE, 0, 16));
            when(dm26Packet.getNonContinuouslyMonitoredSystems()).thenReturn(nonContSystems);
        }
        instance.process(dm26Packet);

        assertEquals(16, instance.getRowCount());

        assertEquals("System01", instance.getValueAt(0, 0));
        assertEquals(false, instance.getValueAt(0, 1));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(0, 1));
        assertEquals(false, instance.getValueAt(0, 2));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(0, 2));
        assertEquals(false, instance.getValueAt(0, 3));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(0, 3));
        assertEquals(false, instance.getValueAt(0, 4));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(0, 4));

        assertEquals("System02", instance.getValueAt(1, 0));
        assertEquals(false, instance.getValueAt(1, 1));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(1, 1));
        assertEquals(false, instance.getValueAt(1, 2));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(1, 2));
        assertEquals(false, instance.getValueAt(1, 3));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(1, 3));
        assertEquals(true, instance.getValueAt(1, 4));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(1, 4));

        assertEquals("System03", instance.getValueAt(2, 0));
        assertEquals(false, instance.getValueAt(2, 1));
        assertEquals(Color.RED, getBackgroundColor(2, 1));
        assertEquals(false, instance.getValueAt(2, 2));
        assertEquals(Color.RED, getBackgroundColor(2, 2));
        assertEquals(true, instance.getValueAt(2, 3));
        assertEquals(Color.RED, getBackgroundColor(2, 3));
        assertEquals(false, instance.getValueAt(2, 4));
        assertEquals(Color.RED, getBackgroundColor(2, 4));

        assertEquals("System04", instance.getValueAt(3, 0));
        assertEquals(false, instance.getValueAt(3, 1));
        assertEquals(Color.GREEN, getBackgroundColor(3, 1));
        assertEquals(false, instance.getValueAt(3, 2));
        assertEquals(Color.GREEN, getBackgroundColor(3, 2));
        assertEquals(true, instance.getValueAt(3, 3));
        assertEquals(Color.GREEN, getBackgroundColor(3, 3));
        assertEquals(true, instance.getValueAt(3, 4));
        assertEquals(Color.GREEN, getBackgroundColor(3, 4));

        assertEquals("System05", instance.getValueAt(4, 0));
        assertEquals(false, instance.getValueAt(4, 1));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(4, 1));
        assertEquals(true, instance.getValueAt(4, 2));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(4, 2));
        assertEquals(false, instance.getValueAt(4, 3));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(4, 3));
        assertEquals(false, instance.getValueAt(4, 4));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(4, 4));

        assertEquals("System06", instance.getValueAt(5, 0));
        assertEquals(false, instance.getValueAt(5, 1));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(5, 1));
        assertEquals(true, instance.getValueAt(5, 2));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(5, 2));
        assertEquals(false, instance.getValueAt(5, 3));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(5, 3));
        assertEquals(true, instance.getValueAt(5, 4));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(5, 4));

        assertEquals("System07", instance.getValueAt(6, 0));
        assertEquals(false, instance.getValueAt(6, 1));
        assertEquals(Color.RED, getBackgroundColor(6, 1));
        assertEquals(true, instance.getValueAt(6, 2));
        assertEquals(Color.RED, getBackgroundColor(6, 2));
        assertEquals(true, instance.getValueAt(6, 3));
        assertEquals(Color.RED, getBackgroundColor(6, 3));
        assertEquals(false, instance.getValueAt(6, 4));
        assertEquals(Color.RED, getBackgroundColor(6, 4));

        assertEquals("System08", instance.getValueAt(7, 0));
        assertEquals(false, instance.getValueAt(7, 1));
        assertEquals(Color.GREEN, getBackgroundColor(7, 1));
        assertEquals(true, instance.getValueAt(7, 2));
        assertEquals(Color.GREEN, getBackgroundColor(7, 2));
        assertEquals(true, instance.getValueAt(7, 3));
        assertEquals(Color.GREEN, getBackgroundColor(7, 3));
        assertEquals(true, instance.getValueAt(7, 4));
        assertEquals(Color.GREEN, getBackgroundColor(7, 4));

        assertEquals("System09", instance.getValueAt(8, 0));
        assertEquals(true, instance.getValueAt(8, 1));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(8, 1));
        assertEquals(false, instance.getValueAt(8, 2));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(8, 2));
        assertEquals(false, instance.getValueAt(8, 3));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(8, 3));
        assertEquals(false, instance.getValueAt(8, 4));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(8, 4));

        assertEquals("System10", instance.getValueAt(9, 0));
        assertEquals(true, instance.getValueAt(9, 1));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(9, 1));
        assertEquals(false, instance.getValueAt(9, 2));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(9, 2));
        assertEquals(false, instance.getValueAt(9, 3));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(9, 3));
        assertEquals(true, instance.getValueAt(9, 4));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(9, 4));

        assertEquals("System11", instance.getValueAt(10, 0));
        assertEquals(true, instance.getValueAt(10, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(10, 1));
        assertEquals(false, instance.getValueAt(10, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(10, 2));
        assertEquals(true, instance.getValueAt(10, 3));
        assertEquals(instance.getBackground(), getBackgroundColor(10, 3));
        assertEquals(false, instance.getValueAt(10, 4));
        assertEquals(instance.getBackground(), getBackgroundColor(10, 4));

        assertEquals("System12", instance.getValueAt(11, 0));
        assertEquals(true, instance.getValueAt(11, 1));
        assertEquals(Color.GREEN, getBackgroundColor(11, 1));
        assertEquals(false, instance.getValueAt(11, 2));
        assertEquals(Color.GREEN, getBackgroundColor(11, 2));
        assertEquals(true, instance.getValueAt(11, 3));
        assertEquals(Color.GREEN, getBackgroundColor(11, 3));
        assertEquals(true, instance.getValueAt(11, 4));
        assertEquals(Color.GREEN, getBackgroundColor(11, 4));

        assertEquals("System13", instance.getValueAt(12, 0));
        assertEquals(true, instance.getValueAt(12, 1));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(12, 1));
        assertEquals(true, instance.getValueAt(12, 2));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(12, 2));
        assertEquals(false, instance.getValueAt(12, 3));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(12, 3));
        assertEquals(false, instance.getValueAt(12, 4));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(12, 4));

        assertEquals("System14", instance.getValueAt(13, 0));
        assertEquals(true, instance.getValueAt(13, 1));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(13, 1));
        assertEquals(true, instance.getValueAt(13, 2));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(13, 2));
        assertEquals(false, instance.getValueAt(13, 3));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(13, 3));
        assertEquals(true, instance.getValueAt(13, 4));
        assertEquals(Color.LIGHT_GRAY, getBackgroundColor(13, 4));

        assertEquals("System15", instance.getValueAt(14, 0));
        assertEquals(true, instance.getValueAt(14, 1));
        assertEquals(instance.getBackground(), getBackgroundColor(14, 1));
        assertEquals(true, instance.getValueAt(14, 2));
        assertEquals(instance.getBackground(), getBackgroundColor(14, 2));
        assertEquals(true, instance.getValueAt(14, 3));
        assertEquals(instance.getBackground(), getBackgroundColor(14, 3));
        assertEquals(false, instance.getValueAt(14, 4));
        assertEquals(instance.getBackground(), getBackgroundColor(14, 4));

        assertEquals("System16", instance.getValueAt(15, 0));
        assertEquals(true, instance.getValueAt(15, 1));
        assertEquals(Color.GREEN, getBackgroundColor(15, 1));
        assertEquals(true, instance.getValueAt(15, 2));
        assertEquals(Color.GREEN, getBackgroundColor(15, 2));
        assertEquals(true, instance.getValueAt(15, 3));
        assertEquals(Color.GREEN, getBackgroundColor(15, 3));
        assertEquals(true, instance.getValueAt(15, 4));
        assertEquals(Color.GREEN, getBackgroundColor(15, 4));

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
