package systemTest.dashboard

import geb.spock.GebSpec

class HomeSpec extends GebSpec {
    def 'Home page should show Gradle-injected properties'() {
        when:
        to HomePage
        report 'home'

        then:
        codekvastVersion == System.getProperty('expectedCodekvastVersion')
        apiDocsHref == System.getProperty('geb.build.baseUrl') + '/swagger-ui.html'
    }
}