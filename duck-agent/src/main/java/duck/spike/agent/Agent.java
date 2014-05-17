package duck.spike.agent;


import duck.spike.util.Configuration;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Olle Hallin
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class Agent extends TimerTask {
    private static final String MY_NAME = Agent.class.getSimpleName();

    private final Configuration config;
    private final Timer timer = new Timer();

    private void start() {
        long intervalMillis = config.getWarehouseUploadIntervalSeconds() * 1000L;
        timer.scheduleAtFixedRate(this, intervalMillis, intervalMillis);
        System.out.printf("%s started, config=%s%n", MY_NAME, config);
    }

    @Override
    public void run() {
        System.out.printf("%s: Uploading usage data for '%s' to %s%n", MY_NAME, config.getAppName(), config.getWarehouseUri());
    }

    public static void main(String[] args) {
        Configuration config = Configuration.parseConfigFile("/path/to/duck.properties");
        Agent agent = new Agent(config);
        agent.start();
    }

}
