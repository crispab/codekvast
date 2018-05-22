yieldUnescaped '<!DOCTYPE html>'; newLine()
html {
    comment " Codekvast Login version ${settings.displayVersion} "; newLine()
    head {
        meta('http-equiv': '"Content-Type" content="text/html; charset=utf-8"'); newLine()
        meta('http-equiv': 'x-ua-compatible', content: 'ie=edge'); newLine()
        meta(content: 'width=device-width, initial-scale=1', name: 'viewport'); newLine()
        meta(charset: 'utf-8'); newLine()

        title("Codekvast $title"); newLine()

        link(rel: 'icon', href: '/favicon.ico', type: 'image/ico'); newLine()

        link(rel: 'stylesheet', type: 'text/css', href: 'https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css'); newLine()
        link(rel: 'stylesheet', type: 'text/css', href: 'https://use.fontawesome.com/releases/v5.0.8/css/solid.css'); newLine()
        link(rel: 'stylesheet', type: 'text/css', href: 'https://use.fontawesome.com/releases/v5.0.8/css/fontawesome.css'); newLine()
        link(rel: 'stylesheet', type: 'text/css', href: '/assets/codekvast.css'); newLine()

        script(type: 'text/javascript', src: 'https://code.jquery.com/jquery-3.3.1.slim.min.js', integrity: 'sha256-3edrmyuQ0w65f8gfBsqowzjJe2iM6n0nKciPUp8y+7E=', crossorigin: 'anonymous') { yield('') }; newLine()
        script(type: 'text/javascript', src: 'https://www.google-analytics.com/analytics.js') { yield('') }; newLine()

    }
    newLine()
    body {
        div(id: 'app') {
            header(id: 'top-nav', class: 'container') {
                div(class: 'row align-items-end pt-1 pl-3') {
                    div(class: 'col-1') {
                        a(href: '/') {
                            img(src: '/assets/logo-feathers-60x104.png', class: 'img', alt: 'logotype')
                        }
                    }
                    div(class: 'col-3') {
                        h1('Codekvast')
                        p(class: 'small text-muted', 'The Truly Dead Code Detector')
                    }

                    div(class: 'col-8')
                }
            }

            main(class: 'container') {

                div(class: 'alert alert-warning alert-dismissible fade show', role: 'alert'){
                      p('Codekvast uses cookies.')
                      p {
                            yield('Read more about the ')
                            a(href:'http://www.codekvast.io/pages/privacy-policy.html', target:'_new', 'Codekvast Privacy Policy')
                      }

                      button(type: 'button', class: 'close', 'data-dismiss': 'alert', 'aria-label': 'Close') {
                          span('aria-hidden': 'true', '&times;')
                      }
                }

                bodyContents()
            }

            footer(class: 'bg-info small') {
                ul(class: 'nav justify-content-end mr-1') {
                    li(class: 'nav-item px-3') {
                        span(class: 'btn disabled') {
                            yield 'Codekvast Login '
                            span(id: 'codekvastVersion', settings.displayVersion)
                        }
                    }
                }
            }
        }
    }
}
