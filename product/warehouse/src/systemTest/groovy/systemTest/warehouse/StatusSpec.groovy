package systemTest.warehouse

import geb.spock.GebSpec

class StatusSpec extends GebSpec {
    def 'Status page should render correctly'() {
        when:
        to StatusPage
        report 'status'

        then:
        header == 'Status'
    }
}