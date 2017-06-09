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
 * Unit tests for the {@link HighlightRenderer} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class HighlightRendererTest {

    private HighlightRenderer instance;

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
        instance = new HighlightRenderer(0);
    }

    @Test
    public void testFalseNotSelected() {
        when(tableModel.getValueAt(27, 0)).thenReturn(false);

        instance.getTableCellRendererComponent(table, null, false, false, 32, 19);
        assertEquals(Color.RED, instance.getBackground());
    }

    @Test
    public void testFalseSelected() {
        when(tableModel.getValueAt(27, 0)).thenReturn(false);

        instance.getTableCellRendererComponent(table, null, true, false, 32, 19);
        assertEquals(Color.RED, instance.getBackground());
    }

    @Test
    public void testNullNotSelected() {
        when(tableModel.getValueAt(27, 0)).thenReturn(null);

        instance.getTableCellRendererComponent(table, null, false, false, 32, 19);
        assertEquals(table.getBackground(), instance.getBackground());
    }

    @Test
    public void testNullSelected() {
        when(tableModel.getValueAt(27, 0)).thenReturn(null);

        instance.getTableCellRendererComponent(table, null, true, false, 32, 19);
        assertEquals(table.getSelectionBackground(), instance.getBackground());
    }

    @Test
    public void testTrueNotSelected() {
        when(tableModel.getValueAt(27, 0)).thenReturn(true);

        instance.getTableCellRendererComponent(table, null, false, false, 32, 19);
        assertEquals(Color.GREEN, instance.getBackground());
    }

    @Test
    public void testTrueSelected() {
        when(tableModel.getValueAt(27, 0)).thenReturn(true);

        instance.getTableCellRendererComponent(table, null, true, false, 32, 19);
        assertEquals(Color.GREEN, instance.getBackground());
    }

}
