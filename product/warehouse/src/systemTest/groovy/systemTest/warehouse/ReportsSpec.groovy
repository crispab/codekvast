package systemTest.warehouse

import geb.spock.GebSpec

class ReportsSpec extends GebSpec {
    def 'Reports page should render correctly'() {
        when:
        to ReportsPage
        report 'reports'

        then:
        header == 'Reports'
    }
}