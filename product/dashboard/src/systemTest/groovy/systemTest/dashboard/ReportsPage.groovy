package systemTest.dashboard

import geb.Page

class ReportsPage extends Page {
    static url = '/reports'

    static at = {
        title == "Codekvast Reports"
    }
}