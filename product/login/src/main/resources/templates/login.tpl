layout '_layout.tpl', true,
    title: 'Login',
    bodyContents: contents {
        h1 'Welcome!'
        p 'Log in with one of'
        p a(href: '/oauth2/authorization/google', 'Google')
        p a(href: '/oauth2/authorization/github', 'Github')
        p a(href: '/oauth2/authorization/facebook', 'Facebook')
        div(class: 'card') {
            div(class: 'card-body') {
                h5 class: 'card-title', 'Heroku'
                div(class: 'card-text') {
                    p {
                        yield 'As a Heroku user you open the Codekvast Dashboard with the CLI command '
                        code 'heroku addons:open codekvast'
                        yield ' from your Heroku workspace or by clicking on Codekvast in the '
                        a href: 'https://dashboard.heroku.com/apps/', target: '_new', 'Heroku Dashboard'
                        yield '.'
                    }
                }
            }
        }
    }
