layout '_layout.tpl', true,
    title: 'Login',
    bodyContents: contents {
        h1("Welcome to Codekvast!")
        p("Log in with one of")
        p(a(href: "/oauth2/authorization/google", "Google"))
        p(a(href: "/oauth2/authorization/github", "Github"))
        p(a(href: "/oauth2/authorization/facebook", "Facebook"))
    }
