/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.ui;

import java.util.concurrent.Executor;

import javax.swing.SwingUtilities;

/**
 * {@link Executor} that wraps {@link SwingUtilities}.invokeLater so the call
 * can be unit tested and run immediately
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class SwingExecutor implements Executor {

    @Override
    public void execute(Runnable command) {
        SwingUtilities.invokeLater(command);
    }
}
