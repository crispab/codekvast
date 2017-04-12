package systemTest.warehouse

import geb.Page

class ReportsPage extends Page {
    static url = '/reports'

    static content = {
        header { $('ck-reports h1').text() }
    }
}