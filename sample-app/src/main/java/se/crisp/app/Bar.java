package se.crisp.app;

/**
 * @author Olle Hallin
 */
public class Bar {

    public int m1() {
        if (true) {
            privateMethodUsed();
        } else {
            privateMethodNeverUsed();
            System.out.println(new Object() {
                @Override
                public String toString() {
                    return "anonymous class never called";
                }
            });
        }
        return 17;
    }

    private Bar privateMethodNeverUsed() {
        return null;
    }

    private String privateMethodUsed() {
        return "called";
    }

    public int m2() {
        return 4711;
    }

    private class InnerClassNeverUsed {
        int methodInInnerClassNeverUsed() {
            return 1;
        }
    }

}
