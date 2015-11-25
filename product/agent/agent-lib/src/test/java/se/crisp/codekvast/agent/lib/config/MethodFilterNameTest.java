package se.crisp.codekvast.agent.lib.config;

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

    @Getter
    @Setter
    static class TestClass implements Comparable<TestClass> {

        private int foo;

        public void setFoo(int x, int y) {
            // no-op
        }

        public TestClass setFoo2(int x) {
            return this;
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

        @Override
        public int compareTo(TestClass o) {
            return 0;
        }

        public int compareTo(int x, int y) {
            return 0;
        }

        public String toString(int i) {
            return null;
        }
    }


    @Parameterized.Parameters(name = "{index}: {0}{1}")
    public static Object[][] data() {
        return new Object[][]{
                {"getFoo", "()", new Class[]{}, true, false, false, false, false, false},
                {"getFoo2", "()", new Class[]{}, false, false, false, false, false, false},
                {"getFoo3", "(int)", new Class[]{int.class}, false, false, false, false, false, false},
                {"setFoo", "(int)", new Class[]{int.class}, false, true, false, false, false, false},
                {"setFoo2", "(int)", new Class[]{int.class}, false, false, false, false, false, false},
                {"setFoo", "(int, int)", new Class[]{int.class, int.class}, false, false, false, false, false, false},
                {"equals", "(Object)", new Class[]{Object.class}, false, false, true, false, false, false},
                {"equals", "(int)", new Class[]{int.class}, false, false, true, false, false, false},
                {"equals", "(Object, Object)", new Class[]{Object.class, Object.class}, false, false, false, false, false, false},
                {"hashCode", "()", new Class[]{}, false, false, false, true, false, false},
                {"hashCode", "(int)", new Class[]{int.class}, false, false, false, false, false, false},
                {"compareTo", "(Object)", new Class[]{Object.class}, false, false, false, false, true, false},
                {"compareTo", "(TestClass)", new Class[]{TestClass.class}, false, false, false, false, true, false},
                {"compareTo", "(int, int)", new Class[]{int.class, int.class}, false, false, false, false, false, false},
                {"toString", "()", new Class[]{}, false, false, false, false, false, true},
                {"toString", "(int)", new Class[]{int.class}, false, false, false, false, false, false},
        };
    }

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

    @Parameterized.Parameter(7)
    public boolean expectedCompareTo;

    @Parameterized.Parameter(8)
    public boolean expectedToString;

    MethodFilter filter = new MethodFilter("all");

    @Test
    public void testIsMethod() throws Exception {
        Method m = TestClass.class.getMethod(name, parameterTypes);
        assertThat(name + prettyArgs + " should be a getter", filter.isGetter(m), is(expectedGetter));
        assertThat(name + prettyArgs + " should be a setter", filter.isSetter(m), is(expectedSetter));
        assertThat(name + prettyArgs + " should be equals()", filter.isEquals(m), is(expectedEquals));
        assertThat(name + prettyArgs + " should be hashCode()", filter.isHashCode(m), is(expectedHashCode));
        assertThat(name + prettyArgs + " should be compareTo()", filter.isCompareTo(m), is(expectedCompareTo));
        assertThat(name + prettyArgs + " should be toString()", filter.isToString(m), is(expectedToString));
    }
}
