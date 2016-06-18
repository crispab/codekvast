package se.crisp.codekvast.warehouse.systemtest;

import net.serenitybdd.junit.runners.SerenityRunner;
import net.thucydides.core.annotations.Managed;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
@RunWith(SerenityRunner.class)
public class WhenBrowsingWarehouse {

    private final String webDriverBaseUrl = System.getProperty("webdriver.base.url");
    private final String expectedVersion = System.getProperty("expectedCodekvastVersion");

    @Managed
    WebDriver driver;

    WarehouseHomePage warehouseHomePage;

    @Before
    public void beforeTest() throws Exception {
        warehouseHomePage.open();
    }

    /**
     * Verify that the app has started in a Docker container and that Gradle has defined the base url...
     */
    @Test
    public void should_have_system_properties() {
        assertThat(expectedVersion, not(nullValue()));
        assertThat(webDriverBaseUrl, not(nullValue()));
        assertThat(webDriverBaseUrl, startsWith("http://0.0.0.0:"));
    }

    @Test
    public void should_see_codekvast_version_on_home_page() {
        assertThat(warehouseHomePage.codekvastVersion(), is(expectedVersion));
    }

    @Test
    public void should_have_correct_api_href() {
        assertThat(warehouseHomePage.apiDocsHref(), startsWith(webDriverBaseUrl + "/"));
    }

}
