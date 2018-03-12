package systemTest.dashboard

import geb.spock.GebSpec
import io.codekvast.common.security.WebappCredentials
import io.codekvast.common.security.impl.SecurityServiceImpl
import io.codekvast.dashboard.bootstrap.CodekvastDashboardSettings
import org.openqa.selenium.Cookie

class MethodsSpec extends GebSpec {
    def 'Methods page should render correctly'() {
        given:
        def settings = new CodekvastDashboardSettings();
        settings.setDashboardJwtSecret("secret")
        def securityManager = new SecurityServiceImpl(settings, null);
        securityManager.postConstruct()
        def sessionToken = securityManager.createWebappToken(1L, WebappCredentials.sample())

        println """

============ sessionToken = $sessionToken


"""
        def options = driver.manage()
        options.addCookie(new Cookie('sessionToken', sessionToken))

        when:
        to MethodsPage
        report 'methods'

        then:
        signatureField.text() == ''
    }
}