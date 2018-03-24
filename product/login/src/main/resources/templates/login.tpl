layout '_layout.tpl', true,
    title: 'Login',
    bodyContents: contents {
        h1 'Welcome!'
        p('Log in with one account that has an email address associated with your Codekvast project:')

        ul(class: 'list-group') {
            li(class: 'list-group-item') {
                a(href: '/oauth2/authorization/google', 'Google')
            }
            li(class: 'list-group-item') {
                a(href: '/oauth2/authorization/github', 'Github')
            }
            li(class: 'list-group-item') {
                a(href: '/oauth2/authorization/facebook', 'Facebook')
            }
            li(class: 'list-group-item') {
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
                            p '''You can also log in using any of the above social login providers, 
                                as long as it is associated with the same email address as your Heroku account.'''
                        }
                    }
                }
            }
        }
    }
