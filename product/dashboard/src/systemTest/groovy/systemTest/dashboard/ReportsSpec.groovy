package systemTest.dashboard

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