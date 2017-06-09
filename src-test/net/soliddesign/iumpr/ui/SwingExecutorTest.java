/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the {@link SwingExecutor} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class SwingExecutorTest {

    private SwingExecutor instance;

    @Before
    public void setUp() throws Exception {
        instance = new SwingExecutor();
    }

    @Test
    public void testExecute() throws Exception {
        int[] count = new int[] { 0 };
        instance.execute(() -> count[0] = count[0] + 1);
        Thread.sleep(300);
        assertEquals(1, count[0]);
    }

}
