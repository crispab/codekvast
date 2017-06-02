/*
 * Copyright (c) 2015-2017 Hallin Information Technology AB
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
package io.codekvast.warehouse.bootstrap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.UiConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;

/**
 * Configure the embedded Swagger API docs stuff.
 *
 * @author olle.hallin@crisp.se
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    ApiInfo swaggerApiInfo(CodekvastSettings settings) {
        return new ApiInfo(settings.getApplicationName(),
                           "Codekvast Warehouse",
                           settings.getDisplayVersion(),
                           "http://www.codekvast.io",
                           new Contact("Olle Hallin", "http://www.codekvast.io", "olle.hallin@crisp.se"),
                           "MIT",
                           "https://opensource.org/licenses/MIT",
                           new ArrayList<>());

    }

    @Bean
    UiConfiguration swaggerUiConfig() {
        // disable validationUrl
        return new UiConfiguration(null);
    }

    @Bean
    public Docket agentDocket(ApiInfo apiInfo) {
        return new Docket(DocumentationType.SWAGGER_2)
            .apiInfo(apiInfo)
            .groupName("javaagent-endpoints")
            .select()
            .paths(path -> path.startsWith("/javaagent"))
            .build();
    }

    @Bean
    public Docket webappDocket(ApiInfo apiInfo) {
        return new Docket(DocumentationType.SWAGGER_2)
            .apiInfo(apiInfo)
            .groupName("webapp-endpoints")
            .select()
            .paths(path -> path.startsWith("/webapp"))
            .build();
    }

    @Bean
    public Docket managementDocket(ApiInfo apiInfo, @Value("${management.contextPath}") String managementPath) {
        return new Docket(DocumentationType.SWAGGER_2)
            .apiInfo(apiInfo)
            .groupName("management-endpoints")
            .select()
            .paths(path -> path.startsWith(managementPath))
            .build();
    }
}
