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
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Wrapper for environment properties codekvast.*
 *
 * @author olle.hallin@crisp.se
 */
@Component("codekvastSettings")
@ConfigurationProperties(prefix = "codekvast")
@Data
@Slf4j
@ToString(exclude = "janrainApiKey")
@SuppressWarnings("ClassWithTooManyMethods")
public class CodekvastLoginSettings {

    /**
     * The name of the application, injected from the build system.
     */
    private String applicationName;

    /**
     * The version of the application, injected from the build system.
     */
    private String displayVersion;

    /**
     * The name of the person doing the last commit, injected from the build system.
     */
    private String committer;

    /**
     * The date of the last commit, injected from the build system.
     */
    private String commitDate;

    /**
     * The last commit message, injected from the build system.
     */
    private String commitMessage;

    /**
     * To where should we redirect after a login?
     */
    private String redirectAfterLoginTarget;

    /**
     * To where should Janrain post the token after a successful login?
     */
    private String janrainTokenUrl;

    /**
     * What is the URL for getting auth info from Janrain?
     */
    private String janrainAuthInfoUrl;

    /**
     * What API key shall we use when accessing Janrain?
     */
    private String janrainApiKey;

    @PostConstruct
    public void logStartup() {

        //noinspection UseOfSystemOutOrSystemErr
        System.out.printf("%s v%s (%s) started%n", applicationName, displayVersion, commitDate);
        logger.info("{} v{} ({}) starts", applicationName, displayVersion, commitDate);
    }

    @PreDestroy
    public void logShutdown() {
        //noinspection UseOfSystemOutOrSystemErr
        System.out.printf("%s v%s (%s) shuts down%n", applicationName, displayVersion, commitDate);
        logger.info("{} v{} ({}) shuts down", applicationName, displayVersion, commitDate);
    }

}
