/*
 * Copyright (c) 2015-2018 Hallin Information Technology AB
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
package io.codekvast.dashboard.dashboard;

import io.codekvast.common.security.SecurityService;
import io.codekvast.dashboard.bootstrap.CodekvastDashboardSettings;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * A CORS-enabled REST controller that participates in the login dance with the codekvast-login app.
 *
 * @author olle.hallin@crisp.se
 */
@RestController
@CrossOrigin(origins = {"http://localhost:8080", "http://localhost:8088", "https://login-staging.codekvast.io",
                        "https://login.codekvast.io"},
    allowCredentials = "true")
@RequiredArgsConstructor
@Slf4j
public class DashboardLaunchController {

    private final SecurityService securityService;
    private final CodekvastDashboardSettings settings;

    @RequestMapping(method = POST, path = "/dashboard/launch/{code}")
    public String launch(@PathVariable("code") String code,
                         @RequestParam(name = "navData", required = false, defaultValue = "") String navData,
                         HttpServletResponse response) {

        logger.debug("Handling launch request, code={}", code);

        String sessionToken = securityService.tradeCodeToWebappToken(code);

        response.addCookie(createCookie(CookieNames.SESSION_TOKEN, sessionToken));
        response.addCookie(createCookie(CookieNames.NAV_DATA, sessionToken == null ? null : navData));
        return String.format("%s/%s", settings.getDashboardBaseUrl(), sessionToken == null ? "not-logged-in" : "home");
    }

    @RequestMapping(method = GET, path="/dashboard/loginUrl")
    public String getLoginUrl() {
        return settings.getLoginBaseUrl();
    }

    @SneakyThrows(UnsupportedEncodingException.class)
    Cookie createCookie(String name, String value) {
        Cookie cookie = new Cookie(name, value == null || value.isEmpty() ? "" : URLEncoder.encode(value, "UTF-8"));
        cookie.setPath("/");
        cookie.setMaxAge(value == null ? 0 : -1);
        return cookie;
    }

}
