package duck.spike.sensor;

import java.io.IOException;

/**
 * @author Olle Hallin
 */
public class NullOutputStream extends java.io.OutputStream {

    @Override
    public void write(int b) throws IOException {
        // no-op
    }
}
