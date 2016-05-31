package se.crisp.codekvast.agent.lib.util;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * @author olle.hallin@crisp.se
 */
public class ComputerIDTest {

    @Test
    public void should_ignore_interfaces_without_mac_address() throws Exception {

        // given
        ComputerID id = ComputerID.compute();

        // when

        // then
        assertThat(id.toString().matches("^[0-9a-h]{4,20}$"), is(true));
    }
}
