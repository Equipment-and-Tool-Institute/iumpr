/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.ui.status;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.SwingConstants;

import net.soliddesign.iumpr.NumberFormatter;

/**
 * Render for decimal numbers so the rendering is consistent with the rest of
 * the application
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 */
public class DecimalRenderer extends HighlightRenderer {

    private static final long serialVersionUID = -8221017950187375931L;

    public DecimalRenderer(int changedColumn) {
        super(changedColumn);
        setHorizontalAlignment(SwingConstants.RIGHT);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        setValue(value == null ? "" : NumberFormatter.format((Number) value));
        return this;
    }
}
