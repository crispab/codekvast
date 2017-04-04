package systemTest.warehouse

import geb.Page

class StatusPage extends Page {
    static url = '/status'

    static content = {
        header { $('ck-status #header').text() }
    }
}