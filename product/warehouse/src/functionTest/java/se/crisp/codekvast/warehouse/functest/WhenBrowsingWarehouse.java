package se.crisp.codekvast.warehouse.functest;

import net.serenitybdd.junit.runners.SerenityRunner;
import net.thucydides.core.annotations.Managed;
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

    /**
     * Verify that the app has started in a Docker container and that Gradle has defined the base url...
     */
    @Test
    public void should_have_webdriver_base_url() {
        assertThat(webDriverBaseUrl, startsWith("http://0.0.0.0:"));
    }

    /**
     * Verify that Gradle filtering of index.html works and that Angular2 starts...
     */
    @Test
    public void should_see_codekvast_version_on_home_page() {
        // given

        // when
        warehouseHomePage.open();

        // then
        assertThat(warehouseHomePage.codekvastVersion(), is(expectedVersion));
        assertThat(warehouseHomePage.apiDocsHref(), startsWith(webDriverBaseUrl + "/"));
        assertThat(warehouseHomePage.apiDocsHref(), not(startsWith("http:localhost:8080/")));
    }

}
