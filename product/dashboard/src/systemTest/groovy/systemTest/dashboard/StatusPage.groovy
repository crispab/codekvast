package systemTest.dashboard

import geb.Page

class StatusPage extends Page {
    static url = '/status'

    static at = {
        title == "Codekvast Status"
    }
}