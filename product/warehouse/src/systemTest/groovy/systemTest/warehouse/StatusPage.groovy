package systemTest.warehouse

import geb.Page

class StatusPage extends Page {
    static url = '/status'

    static content = {
        header { $('ck-collection-status h1').text() }
    }
}