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
package io.codekvast.login;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

/**
 * @author olle.hallin@crisp.se
 */
@Controller
@RequestMapping(path = "/login")
@Slf4j
@RequiredArgsConstructor
public class LoginController {

    private final CodekvastLoginSettings settings;
    private final RestTemplateBuilder restTemplateBuilder;

    private RestTemplate restTemplate;

    @PostConstruct
    public void postConstruct() {
        this.restTemplate = restTemplateBuilder.build();
    }

    @RequestMapping(path = "/janrain", method = RequestMethod.POST)
    public String janrainToken(@RequestParam("token") String token) {
        logger.info("Received Janrain token {}", token);

        String url = String.format("%s?apiKey=%s&token=%s", settings.getJanrainAuthInfoUrl(), settings.getJanrainApiKey(), token);
        AuthInfo authInfo = restTemplate.getForObject(url, AuthInfo.class);
        logger.debug("Received {}", authInfo);

        // TODO: check if user has a known email address
        // TODO: Member of more that one organisation?
        // TODO: create a JWT

        return "redirect:" + settings.getRedirectAfterLoginTarget();
    }

    @Data
    private static class AuthInfo {
        private String stat;
        private Profile profile;
    }

    @Data
    private static class Profile {
        private String identifier;
        private String email;
        private String preferredUsername;
    }
}
