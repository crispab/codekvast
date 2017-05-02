package sample.app;

import lombok.extern.slf4j.Slf4j;

/**
 * @author olle.hallin@crisp.se
 */
@Slf4j
public class SampleApp {
    private final int dummy;

    public SampleApp() {
        dummy = 17;
    }

    public int add(int p1, int p2) {
        return p1 + p2;
    }

    public static void main(String[] args) {
        log.info("2+2={}", new SampleApp().add(2, 2));
    }
}
