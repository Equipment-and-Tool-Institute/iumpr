/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.ui.status;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.awt.Color;

import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for the {@link DecimalRenderer} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class DecimalRendererTest {

    private DecimalRenderer instance;

    private JTable table;

    @Mock
    private TableModel tableModel;

    @Before
    public void setUp() throws Exception {
        table = new JTable(tableModel);
        instance = new DecimalRenderer(0);
    }

    @Test
    public void testFalseNotSelected() {
        int row = 32;
        when(tableModel.getValueAt(row, 0)).thenReturn(false);

        instance.getTableCellRendererComponent(table, 18.0, false, false, row, 19);
        assertEquals(Color.RED, instance.getBackground());
        assertEquals("18", instance.getText());
    }

    @Test
    public void testFalseSelected() {
        int row = 32;
        when(tableModel.getValueAt(row, 0)).thenReturn(false);

        instance.getTableCellRendererComponent(table, 19.2, true, false, row, 19);
        assertEquals(Color.RED, instance.getBackground());
        assertEquals("19.2", instance.getText());
    }

    @Test
    public void testNullNotSelected() {
        int row = 32;
        when(tableModel.getValueAt(row, 0)).thenReturn(null);

        instance.getTableCellRendererComponent(table, null, false, false, row, 19);
        assertEquals(table.getBackground(), instance.getBackground());
        assertEquals("", instance.getText());
    }

    @Test
    public void testNullSelected() {
        int row = 32;
        when(tableModel.getValueAt(row, 0)).thenReturn(null);

        instance.getTableCellRendererComponent(table, null, true, false, row, 19);
        assertEquals(table.getSelectionBackground(), instance.getBackground());
        assertEquals("", instance.getText());
    }

    @Test
    public void testTrueNotSelected() {
        int row = 32;
        when(tableModel.getValueAt(row, 0)).thenReturn(true);

        instance.getTableCellRendererComponent(table, 78.333333333333333333333333333, false, false, row, 19);
        assertEquals(Color.GREEN, instance.getBackground());
        assertEquals("78.333", instance.getText());
    }

    @Test
    public void testTrueSelected() {
        int row = 32;
        when(tableModel.getValueAt(row, 0)).thenReturn(true);

        instance.getTableCellRendererComponent(table, -456789.6789, true, false, row, 19);
        assertEquals(Color.GREEN, instance.getBackground());
        assertEquals("-456,789.679", instance.getText());
    }
}
