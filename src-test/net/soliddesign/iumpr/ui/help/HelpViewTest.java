/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.ui.help;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    @Before
    public void setUp() throws Exception {
        instance = new HelpView();
    }

    @Test
    public void test() {
        assertEquals(false, instance.isVisible());
        instance.setVisible(true);
        assertEquals(true, instance.isVisible());

        // We *could* verify the entire text, but make sure there's something
        // there
        assertTrue(instance.getEditorPane().getText().contains("IUMPR Data Collection Tool"));

        instance.getCloseButton().doClick();

        assertEquals(false, instance.isVisible());
    }

}
