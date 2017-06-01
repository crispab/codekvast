package sample.app;

import lombok.extern.java.Log;

/**
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings("ALL")
@Log
public class SampleApp {
    private final int dummy;

    public SampleApp() {
        dummy = 17;
    }

    public int add(int p1, int p2) {
        return p1 + p2;
    }

    public static void main(String[] args) throws InterruptedException {
        log.info("2+2=" + new SampleApp().add(2, 2));
        Thread.sleep(1_500L);
        log.info("Exit");
    }
}
