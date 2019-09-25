/*
 * Copyright (c) 2015-2019 Hallin Information Technology AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
