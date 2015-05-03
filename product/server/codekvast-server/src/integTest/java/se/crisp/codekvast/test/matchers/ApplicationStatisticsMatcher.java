package se.crisp.codekvast.test.matchers;

import lombok.RequiredArgsConstructor;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.hamcrest.TypeSafeMatcher;
import se.crisp.codekvast.server.codekvast_server.model.event.display.ApplicationStatisticsDisplay;
import se.crisp.codekvast.server.codekvast_server.model.event.display.WebSocketMessage;

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
        if (!(item instanceof WebSocketMessage)) {
            mismatchReason = "\n    is not an WebSocketMessage";
            return false;
        }

        WebSocketMessage wsm = (WebSocketMessage) item;
        if (wsm.getApplicationStatistics() == null) {
            mismatchReason = "\n    WebSocketMessage with null application statistics displays";
            return false;
        }

        if (wsm.getApplicationStatistics().size() != displayMatchers.length) {
            mismatchReason = String.format("\n     WebSocketMessage with wrong number of application statistics displays. " +
                                                   "Expected = %d, actual = %d",
                                           displayMatchers.length, wsm.getApplications().size());
            return false;
        }

        Iterator<ApplicationStatisticsDisplay> iterator = wsm.getApplicationStatistics().iterator();
        for (int i = 0; i < displayMatchers.length; i++) {
            Matcher matcher = displayMatchers[i];
            ApplicationStatisticsDisplay display = iterator.next();
            if (!matcher.matches(display)) {
                Description description = new StringDescription();
                matcher.describeMismatch(display, description);
                mismatchReason = "\n    application statistics display " + i + " does not match: " + description;
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
        description.appendText("a WebSocketMessage");
    }

    public static Matcher<Object> isApplicationStatistics(Matcher<?>... applicationDisplayMatchers) {
        return new ApplicationStatisticsMatcher(applicationDisplayMatchers);
    }
}
