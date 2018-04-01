package systemTest.dashboard

import geb.spock.GebReportingSpec
import io.codekvast.common.security.WebappCredentials
import io.codekvast.common.security.impl.SecurityServiceImpl
import org.openqa.selenium.Cookie

/**
 * @author olle.hallin@crisp.se
 */
class BaseSpec extends GebReportingSpec {

    protected addSessionTokenCookie() {
        driver.manage().addCookie(
            new Cookie('sessionToken',
                SecurityServiceImpl.TokenFactory.builder()
                    .jwtExpirationHours(1)
                    .jwtSecret("secret")
                    .build()
                    .createWebappToken(1L, WebappCredentials.sample()),
                "/"))
    }

    protected deleteAllCookies() {
        driver.manage().deleteAllCookies()
    }

}
