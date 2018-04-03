package systemTest.dashboard

class MethodsSpec extends BaseSpec {
    def 'Methods page should render correctly when authenticated'() {
        given:
        to HomePage
        addSessionTokenCookie()

        when:
        to MethodsPage

        then:
        at MethodsPage
    }

    def 'Methods page should redirect to NotLoggedInPage when unauthenticated'() {
        given:
        deleteAllCookies()

        when:
        via MethodsPage

        then:
        at NotLoggedInPage
    }

}