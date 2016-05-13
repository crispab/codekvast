package se.crisp.codekvast.warehouse.functest;

import net.serenitybdd.core.annotations.findby.By;
import net.serenitybdd.core.pages.PageObject;
import net.thucydides.core.annotations.DefaultUrl;
import net.thucydides.core.annotations.WhenPageOpens;
import org.openqa.selenium.WebDriver;

/**
 * @author olle.hallin@crisp.se
 */
@DefaultUrl("http://localhost:8080")
public class WarehouseHomePage extends PageObject {

    public WarehouseHomePage(WebDriver driver) {
        super(driver);
    }

    @WhenPageOpens
    public void waitUntilVersionAppears() {
        element(By.id("codekvastVersion")).waitUntilVisible();
    }

    public String codekvastVersion() {
        return find(By.id("codekvastVersion")).getText();
    }

    public String apiDocsHref() {
        return find(By.id("api-docs")).getAttribute("href");
    }
}
