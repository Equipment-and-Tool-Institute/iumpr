/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.ui.status;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

/**
 * Renders a Boolean value as a checkbox. Also colors the background based upon
 * the value in another column
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class CheckBoxRenderer extends JCheckBox implements TableCellRenderer {

    private static final long serialVersionUID = -6118007176058637306L;

    private final int colorColumn;

    /**
     * Constructor
     *
     * @param colorColumn
     *            the column that will contain the color for the background
     */
    public CheckBoxRenderer(int colorColumn) {
        setHorizontalAlignment(SwingConstants.CENTER);
        this.colorColumn = colorColumn;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        int modelRow = table.convertRowIndexToModel(row);
        Color color = (Color) table.getModel().getValueAt(modelRow, colorColumn);
        setBackground(isSelected ? table.getSelectionBackground() : (color == null ? table.getBackground() : color));
        setSelected((value != null && ((Boolean) value).booleanValue()));
        return this;
    }
}