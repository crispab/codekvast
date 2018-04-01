package systemTest.dashboard

class MethodsSpec extends BaseSpec {
    def 'Methods page should render correctly when authenticated'() {
        given:
        addSessionTokenCookie()

        when:
        to MethodsPage

        then:
        signatureField.text() == ''
    }

    def 'Methods page should redirect to NotLoggedInPage when unauthenticated'() {
        given:
        deleteAllCookies()

        when:
        to MethodsPage

        then:
        at NotLoggedInPage
    }

}