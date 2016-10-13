package se.crisp.codekvast.testsupport;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author olle.hallin@crisp.se
 */
@UtilityClass
@Slf4j
public class ProcessUtils {

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
