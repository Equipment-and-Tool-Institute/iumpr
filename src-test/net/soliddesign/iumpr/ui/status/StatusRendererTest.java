/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.ui.status;

import static org.junit.Assert.assertEquals;

import java.awt.Color;

import javax.swing.JTable;
import javax.swing.SwingConstants;

import org.junit.Before;
import org.junit.Test;

import net.soliddesign.iumpr.bus.j1939.packets.MonitoredSystem.Status;

/**
 * Unit tests for the {@link StatusRenderer}
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class StatusRendererTest {

    private StatusRenderer instance;

    private JTable table;

    @Before
    public void setUp() throws Exception {
        table = new JTable();
        instance = new StatusRenderer();
        assertEquals(SwingConstants.CENTER, instance.getHorizontalAlignment());
    }

    @Test
    public void testCompleteNotSelected() {
        instance.getTableCellRendererComponent(table, Status.COMPLETE, false, false, 32, 19);
        assertEquals(Color.GREEN, instance.getBackground());
        assertEquals("complete", instance.getText());
    }

    @Test
    public void testCompleteSelected() {
        instance.getTableCellRendererComponent(table, Status.COMPLETE, true, false, 32, 19);
        assertEquals(Color.GREEN, instance.getBackground());
        assertEquals("complete", instance.getText());
    }

    @Test
    public void testNotCompleteNotSelected() {
        instance.getTableCellRendererComponent(table, Status.NOT_COMPLETE, false, false, 32, 19);
        assertEquals(table.getBackground(), instance.getBackground());
        assertEquals("not complete", instance.getText());
    }

    @Test
    public void testNotCompleteSelected() {
        instance.getTableCellRendererComponent(table, Status.NOT_COMPLETE, true, false, 32, 19);
        assertEquals(table.getSelectionBackground(), instance.getBackground());
        assertEquals("not complete", instance.getText());
    }

    @Test
    public void testNotSupportedNotSelected() {
        instance.getTableCellRendererComponent(table, Status.NOT_SUPPORTED, false, false, 32, 19);
        assertEquals(Color.LIGHT_GRAY, instance.getBackground());
        assertEquals("not enabled", instance.getText());
    }

    @Test
    public void testNotSupportedSelected() {
        instance.getTableCellRendererComponent(table, Status.NOT_SUPPORTED, true, false, 32, 19);
        assertEquals(Color.LIGHT_GRAY, instance.getBackground());
        assertEquals("not enabled", instance.getText());
    }

    @Test
    public void testNullNotSelected() {
        instance.getTableCellRendererComponent(table, null, false, false, 32, 19);
        assertEquals(table.getBackground(), instance.getBackground());
        assertEquals("", instance.getText());
    }

    @Test
    public void testNullSelected() {
        instance.getTableCellRendererComponent(table, null, true, false, 32, 19);
        assertEquals(table.getSelectionBackground(), instance.getBackground());
        assertEquals("", instance.getText());
    }
}
