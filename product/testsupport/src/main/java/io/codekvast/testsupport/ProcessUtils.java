/*
 * Copyright (c) 2015-2017 Hallin Information Technology AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.testsupport;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * @author olle.hallin@crisp.se
 */
@UtilityClass
@Slf4j
public class ProcessUtils {

    public static String executeCommand(List<String> command) throws RuntimeException, IOException, InterruptedException {
        log.debug("Attempting to execute '{}' ...", command);
        Process process = new ProcessBuilder().command(command).redirectErrorStream(true).start();
        int exitCode = process.waitFor();
        String output = collectProcessOutput(process.getInputStream());
        if (exitCode != 0) {
            throw new RuntimeException(String.format("Could not execute '%s': %s%nExit code=%d", command, output, exitCode));
        }

        return output;
    }

    public static String executeCommand(String command) throws RuntimeException, IOException, InterruptedException {
        log.debug("Attempting to execute '{}' ...", command);
        Process process = Runtime.getRuntime().exec(command);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String error = collectProcessOutput(process.getErrorStream());
            throw new RuntimeException(String.format("Could not execute '%s': %s%nExit code=%d", command, error, exitCode));
        }

        return collectProcessOutput(process.getInputStream());
    }

    private static String collectProcessOutput(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String newLine = "";
        while ((line = reader.readLine()) != null) {
            sb.append(newLine).append(line);
            newLine = String.format("%n");
        }
        return sb.toString();
    }


}
