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
 * Unit tests for the {@link CheckBoxRenderer}
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class CheckBoxRendererTest {

    private CheckBoxRenderer instance;

    private JTable table;

    @Mock
    private TableModel tableModel;

    @Before
    public void setUp() throws Exception {
        table = new JTable(tableModel) {
            private static final long serialVersionUID = 1L;

            @Override
            public int convertRowIndexToModel(int viewRowIndex) {
                return 27;
            }
        };
        instance = new CheckBoxRenderer(1);
    }

    @Test
    public void testNullColorNotSelected() {
        when(tableModel.getValueAt(27, 1)).thenReturn(null);
        instance.getTableCellRendererComponent(table, true, false, false, 32, 19);
        assertEquals(table.getBackground(), instance.getBackground());
        assertEquals(true, instance.isSelected());
    }

    @Test
    public void testNullColorSelected() {
        when(tableModel.getValueAt(27, 1)).thenReturn(null);
        instance.getTableCellRendererComponent(table, true, true, false, 32, 19);
        assertEquals(table.getSelectionBackground(), instance.getBackground());
        assertEquals(true, instance.isSelected());
    }

    @Test
    public void testWithColorNotSelected() {
        when(tableModel.getValueAt(27, 1)).thenReturn(Color.BLACK);

        instance.getTableCellRendererComponent(table, false, false, false, 32, 19);
        assertEquals(Color.BLACK, instance.getBackground());
        assertEquals(false, instance.isSelected());
    }

    @Test
    public void testWithColorSelected() {
        when(tableModel.getValueAt(27, 1)).thenReturn(Color.BLACK);

        instance.getTableCellRendererComponent(table, false, true, false, 32, 19);
        assertEquals(table.getSelectionBackground(), instance.getBackground());
        assertEquals(false, instance.isSelected());
    }
}
