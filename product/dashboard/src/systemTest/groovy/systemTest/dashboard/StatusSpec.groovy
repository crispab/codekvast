package systemTest.dashboard

class StatusSpec extends BaseSpec {
    def 'Status page should render correctly when authenticated'() {
        given:
        addSessionTokenCookie()

        when:
        to StatusPage

        then:
        header == 'Status'
    }

    def 'Methods page should redirect to NotLoggedInPage when unauthenticated'() {
        given:
        deleteAllCookies()

        when:
        to StatusPage

        then:
        at NotLoggedInPage
    }

}
