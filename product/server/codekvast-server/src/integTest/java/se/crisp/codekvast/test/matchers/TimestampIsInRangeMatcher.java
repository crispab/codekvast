/*  Copyright (c) 2000-2006 hamcrest.org
 */
package se.crisp.codekvast.test.matchers;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.Date;


/**
 * Is the timestamp inside the range?
 */
public class TimestampIsInRangeMatcher extends TypeSafeMatcher<Long> {
    private final long low;
    private final long high;

    public TimestampIsInRangeMatcher(long low, long high) {
        this.low = low;
        this.high = high;
    }

    @Override
    public boolean matchesSafely(Long item) {
        return item >= low && item <= high;
    }

    @Override
    public void describeMismatchSafely(Long item, Description mismatchDescription) {
        mismatchDescription.appendText(
                String.format("%1$tF %1$tT is outside the range [%2$tF %2$tT .. %3$tF %3$tT]",
                              new Date(item), new Date(low), new Date(high)));
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(String.format("a timestamp (long) value within [%1$tF %1$tT .. %2$tF %2$tT]",
                                             new Date(low), new Date(high)));
    }

    public static Matcher<Long> timestampInRange(long lowerEndMillis, long intervalLengthMillis) {
        return new TimestampIsInRangeMatcher(lowerEndMillis, lowerEndMillis + intervalLengthMillis);
    }
}
