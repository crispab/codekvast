package sample.app;

/**
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings("ALL")
public class Bar1 {

    public int declaredOnBar() {
        System.out.println("Invoked " + getClass().getName() + ".declaredOnBar()");
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

    public void m1() {
    }

    private Bar1 privateMethodNeverUsed() {
        return null;
    }

    private String privateMethodUsed() {
        return "called";
    }

    public int publicMethodUnusedDeclaredOnBar() {
        return 4711;
    }

    private class InnerClassNeverUsed {
        int methodInInnerClassNeverUsed() {
            return 1;
        }
    }

}
