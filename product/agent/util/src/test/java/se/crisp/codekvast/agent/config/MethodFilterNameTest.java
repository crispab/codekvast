package se.crisp.codekvast.agent.config;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Method;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
@RunWith(Parameterized.class)
public class MethodFilterNameTest {

    @EqualsAndHashCode
    @Getter
    @Setter
    private static class TestClass {

        private int foo;

        public void setFoo(int x, int y) {
            // no-op
        }

        public void getFoo2() {
        }

        public int getFoo3(int x) {
            return 17;
        }

        public void equals(Object p1, Object p2) {

        }

        public boolean equals(int x) {
            return false;
        }

        public int hashCode(int x) {
            return x;
        }
    }


    @Parameterized.Parameters(name = "{index}: {0}{1}")
    public static Object[][] data() {
        return new Object[][]{
                {"getFoo", "()", new Class[]{}, true, false, false, false},
                {"getFoo2", "()", new Class[]{}, false, false, false, false},
                {"getFoo3", "(int)", new Class[]{int.class}, false, false, false, false},
                {"setFoo", "(int)", new Class[]{int.class}, false, true, false, false},
                {"setFoo", "(int, int)", new Class[]{int.class, int.class}, false, false, false, false},
                {"equals", "(Object)", new Class[]{Object.class}, false, false, true, false},
                {"equals", "(int)", new Class[]{int.class}, false, false, true, false},
                {"equals", "(Object, Object)", new Class[]{Object.class, Object.class}, false, false, false, false},
                {"hashCode", "()", new Class[]{}, false, false, false, true},
                {"hashCode", "(int)", new Class[]{int.class}, false, false, false, false},
        };
    }

    ;

    @Parameterized.Parameter(0)
    public String name;

    @Parameterized.Parameter(1)
    public String prettyArgs;

    @Parameterized.Parameter(2)
    public Class<?> parameterTypes[];

    @Parameterized.Parameter(3)
    public boolean expectedGetter;

    @Parameterized.Parameter(4)
    public boolean expectedSetter;

    @Parameterized.Parameter(5)
    public boolean expectedEquals;

    @Parameterized.Parameter(6)
    public boolean expectedHashCode;


    MethodFilter filter = new MethodFilter("all");

    @Test
    public void testRealGetter() throws Exception {
        Method m = TestClass.class.getMethod(name, parameterTypes);
        assertThat(name + prettyArgs + " should be a getter", filter.isGetter(m), is(expectedGetter));
        assertThat(name + prettyArgs + " should be a setter", filter.isSetter(m), is(expectedSetter));
        assertThat(name + prettyArgs + " should be equals", filter.isEquals(m), is(expectedEquals));
        assertThat(name + prettyArgs + " should be hashCode", filter.isHashCode(m), is(expectedHashCode));
    }
}
