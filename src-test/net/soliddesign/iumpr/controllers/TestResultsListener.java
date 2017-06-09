/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.controllers;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.soliddesign.iumpr.IUMPR;

/**
 * Helper class used as a {@link ResultsListener} for testing
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class TestResultsListener implements ResultsListener {

    private boolean complete = false;

    private int lastStep = 0;

    private final List<String> messages = new ArrayList<>();

    private final List<String> results = new ArrayList<>();

    private boolean success;

    public String getMessages() {
        return messages.stream().collect(Collectors.joining(IUMPR.NL));
    }

    public String getResults() {
        StringBuilder sb = new StringBuilder();
        results.stream().forEachOrdered(t -> sb.append(t).append(IUMPR.NL));
        return sb.toString();
    }

    public boolean isComplete() {
        return complete;
    }

    public boolean isSuccess() {
        if (!complete) {
            throw new IllegalStateException("Complete was not received yet");
        }
        return success;
    }

    @Override
    public void onComplete(boolean success) {
        complete = true;
        this.success = success;
    }

    @Override
    public void onProgress(int currentStep, int totalSteps, String message) {
        if (currentStep < lastStep) {
            fail("Steps went backwards");
        } else if (currentStep != lastStep + 1) {
            fail("Steps skipped");
        } else if (currentStep > totalSteps) {
            fail("Steps exceed maximum");
        }

        lastStep = currentStep;
        messages.add(message);
    }

    @Override
    public void onProgress(String message) {
        messages.add(message);
    }

    @Override
    public void onResult(List<String> results) {
        this.results.addAll(results);
    }

    @Override
    public void onResult(String result) {
        results.add(result);
    }

    @Override
    public void onUrgentMessage(String message, String title, int type) {
        fail("Method not implemented");
    }

}
