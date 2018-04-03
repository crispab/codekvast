package systemTest.dashboard

import geb.Page

class HomePage extends Page {
    static url = '/'

    static at = {
        title == "Codekvast Home"
    }

    static content = {
        codekvastVersion { $('#codekvastVersion').text() }
        apiDocsHref { $('#api-docs').@href }
    }
}