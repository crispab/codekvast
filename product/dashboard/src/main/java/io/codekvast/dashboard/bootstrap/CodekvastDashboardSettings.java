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
package io.codekvast.dashboard.bootstrap;

import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;

/**
 * Wrapper for environment properties codekvast.*
 *
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings({"ClassWithTooManyFields", "ClassWithTooManyMethods", "OverlyComplexClass"})
@Component("codekvastSettings")
@ConfigurationProperties(prefix = "codekvast")
@Data
@Slf4j
@ToString(exclude = {"herokuApiPassword", "herokuApiSsoSalt", "webappJwtSecret", "slackWebHookToken"})
public class CodekvastDashboardSettings {

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
     * The path to the agent publication queue
     */
    private File queuePath;

    /**
     * How often to scan queuePath for new files.
     */
    private int queuePathPollIntervalSeconds = 60;

    /**
     * Should imported files be deleted after successful import?
     */
    private boolean deleteImportedFiles = true;

    /**
     * What password will Heroku use when contacting us?
     */
    private String herokuApiPassword;

    /**
     * What SSO salt value will Heroku use when launching the dashboard webapp via SSO?
     */
    private String herokuApiSsoSalt;

    /**
     * Which value should the Heroku add-on provide as CODEKVAST_URL?
     */
    private String herokuCodekvastUrl;

    /**
     * Should the webapp be secured?
     * Leaving this empty will enable running the webapp in demo mode, with only one customerId=1
     */
    private String webappJwtSecret;

    /**
     * How long shall a webapp authentication token live?
     */
    private Long webappJwtExpirationHours = 8760L;

    /**
     * Which is the Slack Incoming Webhook URL?
     */
    private String slackWebHookUrl;

    /**
     * Which is the token to use when posting to slackWebHookUrl?
     */
    private String slackWebHookToken;

    /**
     * What is my server's CNAME?
     */
    private String dnsCname;

    /**
     * @return true unless the webapp is secured
     */
    public boolean isDemoMode() {
        return webappJwtSecret == null || webappJwtSecret.trim().isEmpty();
    }

    /**
     * @return The customerId to use for unauthenticated data queries. Will return -1 if running in secure mode and an unauthenticated
     * request is received.
     */
    public Long getDemoCustomerId() {
        return isDemoMode() ? 1L : -1L;
    }

    @PostConstruct
    public void logStartup() {
        String demoMode = isDemoMode() ? " in demo mode" : "";

        //noinspection UseOfSystemOutOrSystemErr
        System.out.printf("%s v%s (%s) started%s%n", applicationName, displayVersion, commitDate, demoMode);
        logger.info("{} v{} ({}) starts{}", applicationName, displayVersion, commitDate, demoMode);
    }

    @PreDestroy
    public void logShutdown() {
        //noinspection UseOfSystemOutOrSystemErr
        System.out.printf("%s v%s (%s) shuts down%n", applicationName, displayVersion, commitDate);
        logger.info("{} v{} ({}) shuts down", applicationName, displayVersion, commitDate);
    }

}
