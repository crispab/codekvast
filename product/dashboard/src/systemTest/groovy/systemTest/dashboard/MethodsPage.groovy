package systemTest.dashboard

import geb.Page

class MethodsPage extends Page {
    static url = '/methods'

    static at = {
        title == "Codekvast Methods"
    }
}