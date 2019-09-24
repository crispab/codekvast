package io.codekvast.login.http;

import io.codekvast.login.bootstrap.CodekvastLoginSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

/**
 * @author olle.hallin@crisp.se
 */
@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class ModelAttributes {
    private final CodekvastLoginSettings settings;

    @ModelAttribute("settings")
    public CodekvastLoginSettings getCodekvastSettings() {
        return settings;
    }

    @ModelAttribute("cookieConsent")
    public Boolean getCookieConsent(@CookieValue(name = "cookieConsent", defaultValue = "FALSE") Boolean cookieConsent) {
        logger.trace("cookieConsent={}", cookieConsent);
        return Optional.ofNullable(cookieConsent).orElse(Boolean.FALSE);
    }

    @ModelAttribute("cookieDomain")
    public String cookieDomain(@RequestHeader("Host") String requestHost) {
        logger.trace("requestHost={}", requestHost);
        return requestHost.startsWith("localhost") ? "localhost" : ".codekvast.io";
    }

    @ModelAttribute("serverHostName")
    public String serverHostName() {
        try {
            String hostName = InetAddress.getLocalHost().getCanonicalHostName();
            logger.trace("hostName={}", hostName);
            return hostName;
        } catch (UnknownHostException e) {
            return "<unknown>";
        }
    }

}
