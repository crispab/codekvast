package systemTest.dashboard

import geb.Page

class StatusPage extends Page {
    static url = '/status'

    static content = {
        header { $('ck-collection-status h2').text() }
    }
}