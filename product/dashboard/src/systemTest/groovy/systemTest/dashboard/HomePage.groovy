package systemTest.dashboard

import geb.Page

class HomePage extends Page {
    static url = '/'

    static content = {
        codekvastVersion { $('#codekvastVersion').text() }
        apiDocsHref { $('#api-docs').@href }
    }
}