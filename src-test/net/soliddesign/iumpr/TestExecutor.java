/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.Executor;

/**
 * {@link Executor} used for tests to capture the command so it can be run when
 * desired.
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class TestExecutor implements Executor {

    private Runnable command;

    @Override
    public void execute(Runnable command) {
        this.command = command;
    }

    public void run() {
        assertNotNull(command);
        command.run();
    }

}
