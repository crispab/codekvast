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

import com.google.gson.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.json.Json;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.UiConfiguration;
import springfox.documentation.swagger.web.UiConfigurationBuilder;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.lang.reflect.Type;
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
    UiConfiguration swaggerUiConfig() {
        // disable validationUrl
        return UiConfigurationBuilder.builder().validatorUrl(null).build();
    }

    @Bean
    public Docket agentDocket(CodekvastDashboardSettings settings) {
        return new Docket(DocumentationType.SWAGGER_2)
            .apiInfo(getApiInfo(settings, "Endpoints used by the Java agent"))
            .groupName("Java agent endpoints")
            .select()
            .paths(path -> path.startsWith("/javaagent"))
            .build();
    }

    @Bean
    public Docket webappDocket(CodekvastDashboardSettings settings) {
        return new Docket(DocumentationType.SWAGGER_2)
            .apiInfo(getApiInfo(settings, "Endpoints used by the dashboard web app"))
            .groupName("Dashboard endpoints")
            .select()
            .paths(path -> path.startsWith("/dashboard"))
            .build();
    }

    private ApiInfo getApiInfo(CodekvastDashboardSettings settings, String description) {
        return new ApiInfo(settings.getApplicationName(),
                           description,
                           settings.getDisplayVersion(),
                           "http://www.codekvast.io/pages/terms-of-service.html",
                           new Contact("Codekvast", "http://www.codekvast.io", "codekvast-support@hit.se"),
                           "Licensed under the MIT license",
                           "https://opensource.org/licenses/MIT",
                           new ArrayList<>());
    }

    // Hack to make swagger-ui.html work with Gson instead of Jackson
    @Bean
    public Gson gson() {
        return new GsonBuilder().registerTypeAdapter(Json.class, new SpringfoxJsonToGsonAdapter()).create();
    }

    public static class SpringfoxJsonToGsonAdapter implements JsonSerializer<Json> {
        @Override
        public JsonElement serialize(Json json, Type type, JsonSerializationContext context) {
            final JsonParser parser = new JsonParser();
            return parser.parse(json.value());
        }
    }
}
