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

    @Override
    public void execute(Runnable command) {
        assertNotNull(command);
        command.run();
    }

    public void run() {
    }

}
