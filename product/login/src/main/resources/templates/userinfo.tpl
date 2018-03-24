layout '_layout.tpl', true,
    title: 'User',
    bodyContents: contents {
        if (user.customerData) {
            div(class: 'alert alert-success', role="alert", "You are logged in as $user.email")
            table(class: 'table') {
                tr {
                    th("Select Codekvast project to view:")
                }
                for (c in user.customerData) {
                    tr {
                        td {
                            form(method: 'POST', action: "/launch/$c.customerId") {
                                 button(type: 'submit', class: 'btn btn-link', c.customerName)
                            }
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
    }
