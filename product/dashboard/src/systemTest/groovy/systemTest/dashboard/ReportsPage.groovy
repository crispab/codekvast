package systemTest.dashboard

import geb.Page

class ReportsPage extends Page {
    static url = '/reports'

    static content = {
        header { $('ck-report-generator h1').text() }
    }
}