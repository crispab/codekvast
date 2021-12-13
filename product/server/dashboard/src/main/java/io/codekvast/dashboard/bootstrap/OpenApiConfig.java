/*
 * Copyright (c) 2015-2021 Hallin Information Technology AB
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.dashboard.bootstrap;

import io.codekvast.common.bootstrap.CodekvastCommonSettings;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.GroupedOpenApi;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configure the embedded Open API docs stuff.
 *
 * @author olle.hallin@crisp.se
 */
@Configuration
public class OpenApiConfig {

  @Bean
  public GroupedOpenApi agentApi(CodekvastCommonSettings settings) {
    return GroupedOpenApi.builder()
        .group("Java agent endpoints")
        .pathsToMatch("/javaagent/**")
        .addOpenApiCustomiser(getOpenApiCustomizer(settings, "Codekvast Java agent intake"))
        .build();
  }

  @Bean
  public GroupedOpenApi dashboardApi(CodekvastCommonSettings settings) {
    return GroupedOpenApi.builder()
        .group("Dashboard endpoints")
        .pathsToMatch("/dashboard/**")
        .addOpenApiCustomiser(getOpenApiCustomizer(settings, "Codekvast dashboard"))
        .build();
  }

  private OpenApiCustomiser getOpenApiCustomizer(CodekvastCommonSettings settings, String service) {

    return openApi -> {
      openApi.setInfo(
          new Info()
              .title(service + " API")
              .description("Endpoints used by the Codekvast " + service)
              .version(settings.getDisplayVersion())
              .termsOfService("https://www.codekvast.io/pages/terms-of-service.html")
              .contact(
                  new Contact()
                      .name("Codekvast")
                      .url("https://www.codekvast.io")
                      .email("support@codekvast.io"))
              .license(
                  new License()
                      .name("Licensed under the MIT license")
                      .url("https://opensource.org/licenses/MIT")));
      openApi.setExternalDocs(
          new ExternalDocumentation()
              .description("Codekvast Getting Started")
              .url("https://www.codekvast.io/pages/getting-started.html"));
    };
  }
}
