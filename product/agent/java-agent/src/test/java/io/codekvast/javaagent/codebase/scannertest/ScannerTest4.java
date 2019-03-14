package io.codekvast.javaagent.codebase.scannertest;

/**
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings({"UnusedDeclaration", "UseOfSystemOutOrSystemErr"})
public class ScannerTest4 extends java.util.Date {

    public ScannerTest4(long date) {
        super(date);
    }

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

    private void m5(String s) {
        System.out.printf("m5(String)");
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return "ScannerTest4";
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

