package systemTest.dashboard

class HomeSpec extends BaseSpec {
    def 'Home page should show Gradle-injected properties'() {
        when:
        to HomePage

        then:
        codekvastVersion == System.getProperty('expectedCodekvastVersion')
        apiDocsHref == System.getProperty('geb.build.baseUrl') + '/swagger-ui.html'
    }
}