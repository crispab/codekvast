package systemTest.dashboard

import geb.spock.GebSpec
import io.codekvast.common.security.WebappCredentials
import io.codekvast.common.security.impl.SecurityServiceImpl
import org.openqa.selenium.Cookie

class MethodsSpec extends GebSpec {
    def 'Methods page should render correctly when logged in'() {
        given:
        driver.manage().addCookie(new Cookie('sessionToken',
            SecurityServiceImpl.TokenFactory.builder()
                .jwtExpirationHours(1)
                .jwtSecret("secret")
                .build()
                .createWebappToken(1L, WebappCredentials.sample())))

        when:
        to MethodsPage
        report 'methods'

        then:
        signatureField.text() == ''
    }
}