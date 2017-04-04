package systemTest.warehouse

import geb.spock.GebSpec

class SwaggerSpec extends GebSpec {
    def 'Swagger UI should render correctly'() {
        when:
        to SwaggerPage
        report 'swagger-ui'

        then:
        info_title == 'codekvast-warehouse'
    }
}