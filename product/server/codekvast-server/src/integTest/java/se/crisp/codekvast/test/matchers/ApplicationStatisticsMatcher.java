package se.crisp.codekvast.test.matchers;

import lombok.RequiredArgsConstructor;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import se.crisp.codekvast.server.codekvast_server.model.event.display.ApplicationStatisticsDisplay;
import se.crisp.codekvast.server.codekvast_server.model.event.display.ApplicationStatisticsMessage;

/**
 * @author olle.hallin@crisp.se
 */
@RequiredArgsConstructor
public class ApplicationStatisticsMatcher extends TypeSafeMatcher<Object> {

    private final long firstDataReceivedAtMillis;
    private final long lastDataReceivedAtMillis;
    private final long tolerance;

    private String mismatchReason;

    @Override
    protected boolean matchesSafely(Object item) {
        if (!(item instanceof ApplicationStatisticsMessage)) {
            mismatchReason = " is not an application statistics message";
            return false;
        }

        ApplicationStatisticsMessage asm = (ApplicationStatisticsMessage) item;
        ApplicationStatisticsDisplay stats = asm.getApplications().iterator().next();

        long first = stats.getFirstDataReceivedAtMillis();
        if (first < firstDataReceivedAtMillis) {
            mismatchReason = " firstDataReceivedAtMillis " + first + " < " + firstDataReceivedAtMillis;
            return false;
        }
        if (first > firstDataReceivedAtMillis + tolerance) {
            mismatchReason = " firstDataReceivedAtMillis " + first + " > " + (firstDataReceivedAtMillis + tolerance);
            return false;
        }

        long last = stats.getLastDataReceivedAtMillis();
        if (last < lastDataReceivedAtMillis) {
            mismatchReason = " lastDataReceivedAtMillis " + last + " < " + lastDataReceivedAtMillis;
            return false;
        }
        if (last > lastDataReceivedAtMillis + tolerance) {
            mismatchReason = " lastDataReceivedAtMillis " + last + " > " + (lastDataReceivedAtMillis + tolerance);
            return false;
        }
        return true;
    }

    @Override
    public void describeMismatchSafely(Object item, Description mismatchDescription) {
        mismatchDescription.appendValue(item).appendText(mismatchReason);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(String.format("an application statistics with firstReceivedAt near %d and lastReceivedAt near %d",
                                             firstDataReceivedAtMillis,
                                             lastDataReceivedAtMillis));

    }

    public static Matcher<Object> isApplicationStatistics(long firstDataReceivedAtMillis, long lastDataReceivedAtMillis, long tolerance) {
        return new ApplicationStatisticsMatcher(firstDataReceivedAtMillis, lastDataReceivedAtMillis, tolerance);
    }
}
