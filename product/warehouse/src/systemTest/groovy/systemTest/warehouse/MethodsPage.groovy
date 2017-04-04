package systemTest.warehouse

import geb.Page

class MethodsPage extends Page {
    static url = '/methods'

    static content = {
        signatureField(wait: true) { $('ck-methods #signature') }
    }
}