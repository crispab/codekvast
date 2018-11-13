package systemTest.dashboard

class StatusSpec extends BaseSpec {
    def 'Status page should render correctly when authenticated'() {
        given:
        to HomePage
        addSessionTokenCookie()

        when:
        to StatusPage

        then:
        at StatusPage
    }

    def 'Methods page should redirect to NotLoggedInPage when unauthenticated'() {
        given:
        deleteAllCookies()

        when:
        via StatusPage

        then:
        at NotLoggedInPage
    }

}
