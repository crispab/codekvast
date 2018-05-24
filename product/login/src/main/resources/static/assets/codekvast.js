(function (codekvast, $) {

    codekvast.giveCookieConsent = function (cookieDomain) {
        console.log('Setting cookieConsent cookie on domain %o', cookieDomain);
        $('#cookieConsentAlert').addClass('d-none');
        Cookies.set('cookieConsent', 'TRUE', {domain: cookieDomain});
    }

}(window.codekvast = window.codekvast || {}, jQuery));
