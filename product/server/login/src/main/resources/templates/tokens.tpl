package templates

import io.codekvast.common.customer.CustomerService

layout '_layout.tpl', true,
    title: 'Tokens',
    bodyContents: contents {
        h1("$user.email")

        if (customerData) {
            p("${customerData.customerName} has Heroku API details:")
            p {
                yield "Callback URL:"
                code(class: 'ml-3', callbackUrl)
            }
            p {
                yield "Access token:"
                code(class: 'ml-3', "-H 'Authorization: Bearer $accessToken'")
            }
            p {
                a(href: 'javascript:window.history.back()', "Back")
            }
        } else {
            h2("Heroku customers")

            ul {
                for (c in customers) {
                    if (c.source == CustomerService.Source.HEROKU) {
                        li {
                            yield c.customerName
                            span(class: 'ml-2') {
                                a(href: "/tokens/accessToken/${c.customerId}", "Show access token")
                            }
                        }
                    }
                }
            }
        }

        form(class: "form", method: "POST", action: "/logout") {
            button("Log in as another user")
        }

    }
