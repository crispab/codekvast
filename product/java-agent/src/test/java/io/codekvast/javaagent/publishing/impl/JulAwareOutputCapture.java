package io.codekvast.javaagent.publishing.impl;

import org.springframework.boot.test.rule.OutputCapture;

import java.io.IOException;
import java.util.logging.LogManager;

/**
 * @author olle.hallin@crisp.se
 */
public class JulAwareOutputCapture extends OutputCapture {

    @Override
    protected void captureOutput() {
        super.captureOutput();
        try {
            LogManager.getLogManager().readConfiguration();
        } catch (IOException e) {
            throw new RuntimeException("Failed to execute java.util.logging.LogManager.getLogManager().readConfiguration()", e);
        }
    }
}
