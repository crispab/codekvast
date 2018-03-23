yieldUnescaped '<!DOCTYPE html>'; newLine()
html {
    comment " Codekvast Login version ${settings.displayVersion} "; newLine()
    head {
        meta('http-equiv':'"Content-Type" content="text/html; charset=utf-8"'); newLine()
        title("Codekvast $title")
    }
    newLine()
    body {
        bodyContents()
    }
}
