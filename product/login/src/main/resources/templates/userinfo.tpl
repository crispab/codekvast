import java.time.Instant

layout '_layout.tpl', true,
    title: 'Projects',
    bodyContents: contents {
        if (user.customerData) {
            div(class: 'alert alert-success', role = "alert", "$user.email has access to the following Codekvast projects:")
            p(class: "small text-muted", "Click on the project name to view the collected data")
            table(class: 'table table-striped table-hover table-sm') {
                thead(class: 'thead-light') {
                    tr {
                        th(scope: "col", "Project")
                        th(scope: "col", "Created at")
                        th(scope: "col", "Collected since")
                        th(scope: "col", "Comments")
                    }
                }

                tbody {
                    def now = Instant.now()
                    for (c in user.customerData) {

                        def collectedSinceClass = c.collectionStartedAt == null ? "" : "table-success"
                        def comment = ""
                        def delimiter = ""
                        def commentClass = ""
                        if (c.collectionStartedAt == null) {
                            comment = "No data has yet been collected."
                            delimiter = "<br>"
                            commentClass = "table-warning"
                        }
                        if (c.isTrialPeriodExpired(now)) {
                            comment += delimiter + "Trial period ended at ${c.trialPeriodEndsAt}."
                            delimiter = "<br>"
                            commentClass = "table-danger"
                        } else if (c.trialPeriodEndsAt != null) {
                            comment += delimiter + "Trial period ends at ${c.trialPeriodEndsAt}."
                            delimiter = "<br>"
                            commentClass = "table-light"
                        }

                        tr {
                            td {
                                form(method: 'POST', action: "/launch/$c.customerId") {
                                    button(type: 'submit', class: 'btn btn-link', c.displayName)
                                }
                            }

                            td(c.createdAt)
                            td(class: collectedSinceClass, c.collectionStartedAt)
                            td(class: commentClass, comment)
                        }
                    }
                }
            }
        } else {
            div(class: "alert alert-danger", role: "alert", "$user.email hasn't access to any Codekvast project")
        }

        form(class: "form", method: "POST", action: "/logout") {
            button("Log in as another user")
        }

        if (roles.contains("ROLE_ADMIN")) {
            hr()

            p {
                a(href: '/tokens', "Show Heroku access tokens")
            }
        }
    }
