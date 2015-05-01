/*  Copyright (c) 2000-2006 hamcrest.org
 */
package se.crisp.codekvast.test.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;


/**
 * Is the value a number inside a range?
 */
public class LongIsInRangeMatcher extends TypeSafeMatcher<Long> {
    private final Long low;
    private final Long high;

    public LongIsInRangeMatcher(Long low, Long high) {
        this.low = low;
        this.high = high;
    }

    @Override
    public boolean matchesSafely(Long item) {
        return item >= low && item <= high;
    }

    @Override
    public void describeMismatchSafely(Long item, Description mismatchDescription) {
        mismatchDescription.appendValue(item)
                           .appendText(String.format(" is outside the range [%d..%d]", low, high));
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(String.format("a long value within [%d..%d]", low, high));
    }

    @Factory
    public static Matcher<Long> inRange(Long low, Long high) {
        return new LongIsInRangeMatcher(low, high);
    }
}
