package systemTest.dashboard

class ReportsSpec extends BaseSpec {
    def 'Reports page should render correctly'() {
        when:
        to ReportsPage

        then:
        header == 'Reports'
    }
}