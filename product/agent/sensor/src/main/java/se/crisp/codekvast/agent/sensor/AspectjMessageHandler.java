package se.crisp.codekvast.agent.sensor;

import org.aspectj.bridge.AbortException;
import org.aspectj.bridge.IMessage;
import org.aspectj.bridge.IMessageHandler;

import java.io.*;

/**
 * This is a message handler which is plugged in to aspectjweaver so that messages from aspectjweaver are written to the correct log file.
 *
 * @author Olle Hallin
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class AspectjMessageHandler implements IMessageHandler {

    private final Writer writer;

    public AspectjMessageHandler() {
        this.writer = openLogWriter();
    }

    private Writer openLogWriter() {
        File logFile = UsageRegistry.instance.getConfig().getSensorLogFile();
        try {
            return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile)));
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    @Override
    public boolean handleMessage(IMessage message) throws AbortException {
        if (writer != null) {
            try {
                if (!message.getKind().equals(IMessage.DEBUG)) {
                    writer.write(message.toString());
                    writer.write('\n');
                    writer.flush();
                }
                return true;
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }
        return false;
    }

    @Override
    public boolean isIgnoring(IMessage.Kind kind) {
        return false;
    }

    @Override
    public void dontIgnore(IMessage.Kind kind) {
    }

    @Override
    public void ignore(IMessage.Kind kind) {
    }
}
