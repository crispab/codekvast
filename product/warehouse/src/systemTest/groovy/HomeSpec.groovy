import geb.spock.GebSpec

class HomeSpec extends GebSpec {
    def "Home page should show Gradle-injected properties"() {
        when:
        to HomePage

        then:
        codekvastVersion == System.getProperty('expectedCodekvastVersion')
        apiDocsHref.startsWith System.getProperty('geb.build.baseUrl') + "/"
    }
}