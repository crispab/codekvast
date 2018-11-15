package systemTest.dashboard

class SwaggerSpec extends BaseSpec {
    def 'Swagger UI should render correctly'() {
        when:
        to SwaggerPage

        then:
        info_title =~ /(?s)codekvast-dashboard.*${System.getProperty('expectedCodekvastVersion')}/
    }
}