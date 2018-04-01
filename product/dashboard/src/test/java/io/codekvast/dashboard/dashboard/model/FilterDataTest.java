package io.codekvast.dashboard.dashboard.model;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class FilterDataTest {

    @Test
    public void should_have_working_builder() {
        assertThat(FilterData.sample().toString(), is("FilterData(applications=[app1, app2], environments=[env1, env2])"));
    }
}