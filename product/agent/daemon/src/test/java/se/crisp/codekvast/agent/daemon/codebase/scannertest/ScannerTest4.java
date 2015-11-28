package se.crisp.codekvast.agent.daemon.codebase.scannertest;

import lombok.extern.slf4j.Slf4j;

/**
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings({"UnusedDeclaration", "UseOfSystemOutOrSystemErr"})
@Slf4j
public class ScannerTest4 extends java.util.Date {
    @Override
    public long getTime() {
        return super.getTime() - 1;
    }

    public void m4(int i) {
        System.out.printf("m4(int)");
    }

    public void m4(long l) {
        System.out.printf("m4(long)");
    }

    public void m4(String s) {
        System.out.printf("m4(String)");
    }

    @Override
    public ScannerTest4 clone() {
        return (ScannerTest4) super.clone();
    }

    public class Inner {
        public void m5(String s) {
            System.out.printf("m5(String)");
        }
    }

    public static class StaticInner {
        public void m6(String s) {
            System.out.printf("m6(String)");
        }
    }
}

