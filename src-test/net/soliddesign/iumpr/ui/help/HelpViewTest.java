/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.ui.help;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the {@link HelpView} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class HelpViewTest {

    private HelpView instance;

    String txt;

    @Before
    public void setUp() throws Exception {
        instance = new HelpView();
    }

    @Test
    public void test() throws InvocationTargetException, InterruptedException {
        assertEquals(false, instance.isVisible());
        instance.setVisible(true);
        assertEquals(true, instance.isVisible());
        // loading the page is async, so try a couple of times.

        // We *could* verify the entire text, but make sure there's something
        // there
        while (txt == null) {
            SwingUtilities.invokeAndWait(() -> {
                txt = instance.getEditorPane().getText();
            });
        }

        assertTrue(txt.contains("IUMPR Data Collection Tool"));

        instance.getCloseButton().doClick();

        assertEquals(false, instance.isVisible());
    }

}
