package systemTest.dashboard

import geb.Page

/**
 * @author olle.hallin@crisp.se
 */
class NotLoggedInPage extends Page {
    static url = '/not-logged-in'

    static at = {
        $("#header").text() == "Not logged in."
    }
}
