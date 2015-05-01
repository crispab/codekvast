package se.crisp.codekvast.test.matchers;

import lombok.RequiredArgsConstructor;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.hamcrest.TypeSafeMatcher;
import se.crisp.codekvast.server.codekvast_server.model.event.display.ApplicationStatisticsDisplay;
import se.crisp.codekvast.server.codekvast_server.model.event.display.ApplicationStatisticsMessage;

import java.util.Iterator;

/**
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings({"CastToConcreteClass", "InstanceofInterfaces"})
@RequiredArgsConstructor
public class ApplicationStatisticsMatcher extends TypeSafeMatcher<Object> {

    private final Matcher<?>[] displayMatchers;
    private String mismatchReason;

    @Override
    protected boolean matchesSafely(Object item) {
        if (!(item instanceof ApplicationStatisticsMessage)) {
            mismatchReason = "\n    is not an ApplicationStatisticsMessage";
            return false;
        }

        ApplicationStatisticsMessage asm = (ApplicationStatisticsMessage) item;
        if (asm.getApplications() == null) {
            mismatchReason = "\n    ApplicationStatisticsMessage with null application displays";
            return false;
        }

        if (asm.getApplications().size() != displayMatchers.length) {
            mismatchReason = String.format("\n     ApplicationStatisticsMessage with wrong number of application displays. Expected = %d," +
                                                   " actual = %d",
                                           displayMatchers.length, asm.getApplications().size());
            return false;
        }

        Iterator<ApplicationStatisticsDisplay> iterator = asm.getApplications().iterator();
        for (int i = 0; i < displayMatchers.length; i++) {
            Matcher matcher = displayMatchers[i];
            ApplicationStatisticsDisplay display = iterator.next();
            if (!matcher.matches(display)) {
                Description description = new StringDescription();
                matcher.describeMismatch(display, description);
                mismatchReason = "\n    application display " + i + " does not match: " + description;
                return false;
            }
        }

        return true;
    }

    @Override
    public void describeMismatchSafely(Object item, Description mismatchDescription) {
        mismatchDescription.appendValue(item).appendText(mismatchReason);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("an application statistics message");
    }

    public static Matcher<Object> isApplicationStatistics(Matcher<?>... applicationDisplayMatchers) {
        return new ApplicationStatisticsMatcher(applicationDisplayMatchers);
    }
}
