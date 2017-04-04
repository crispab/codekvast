package systemTest.warehouse

import geb.Page

class SwaggerPage extends Page {
    static url = '/swagger-ui.html'

    static content = {
        info_title(wait: true) { $('div.info_title').text() }
    }
}