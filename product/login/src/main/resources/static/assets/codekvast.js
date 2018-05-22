(function (codekvast, $) {

    codekvast.giveCookieConsent = function (cookieDomain) {
        console.log('Setting cookieConsent cookie on domain %o', cookieDomain);
        $('#cookieConsentAlert').addClass('invisible');
        Cookies.set('cookieConsent', 'TRUE', {domain: cookieDomain});
    }

}(window.codekvast = window.codekvast || {}, jQuery));
