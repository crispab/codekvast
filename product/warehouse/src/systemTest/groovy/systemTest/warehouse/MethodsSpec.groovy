package systemTest.warehouse

import geb.spock.GebSpec

class MethodsSpec extends GebSpec {
    def 'Methods page should render correctly'() {
        when:
        to MethodsPage
        report 'methods'

        then:
        signatureField.text() == ''
    }
}