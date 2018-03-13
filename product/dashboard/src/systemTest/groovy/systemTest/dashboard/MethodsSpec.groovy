package systemTest.dashboard

import geb.spock.GebSpec
import io.codekvast.common.security.WebappCredentials
import io.codekvast.common.security.impl.SecurityServiceImpl
import io.codekvast.dashboard.bootstrap.CodekvastDashboardSettings
import org.openqa.selenium.Cookie

class MethodsSpec extends GebSpec {
    def 'Methods page should render correctly'() {
        when:
        to MethodsPage
        report 'methods'

        then:
        signatureField.text() == ''
    }
}