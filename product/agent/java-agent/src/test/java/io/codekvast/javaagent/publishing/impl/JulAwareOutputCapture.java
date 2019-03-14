package io.codekvast.javaagent.publishing.impl;

import org.springframework.boot.test.rule.OutputCapture;

import java.io.IOException;
import java.util.Locale;
import java.util.logging.LogManager;

/**
 * @author olle.hallin@crisp.se
 */
public class JulAwareOutputCapture extends OutputCapture {

    private Locale oldLocale = Locale.getDefault();

    @Override
    protected void captureOutput() {
        super.captureOutput();
        Locale.setDefault(Locale.ENGLISH);
        try {
            LogManager.getLogManager().readConfiguration();
        } catch (IOException e) {
            throw new RuntimeException("Failed to execute java.util.logging.LogManager.getLogManager().readConfiguration()", e);
        }
    }

    @Override
    protected void releaseOutput() {
        Locale.setDefault(oldLocale);
        super.releaseOutput();
    }

}
