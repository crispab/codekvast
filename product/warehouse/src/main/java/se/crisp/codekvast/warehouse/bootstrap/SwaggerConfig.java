package se.crisp.codekvast.warehouse.bootstrap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.UiConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
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
                           "http://codekvast.crisp.se",
                           new Contact("Olle Hallin", "http://codekvast.crisp.se", "olle.hallin@crisp.se"),
                           "MIT",
                           "https://opensource.org/licenses/MIT");

    }

    @Bean
    UiConfiguration swaggerUiConfig() {
        // disable validationUrl
        return new UiConfiguration(null);
    }

    @Bean
    public Docket businessV1Docket(ApiInfo apiInfo) {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo)
                .groupName("business-endpoints-v1")
                .select()
                .paths(path -> path.startsWith("/api/v1"))
                .build();
    }

    @Bean
    public Docket managementDocket(@Value("${management.contextPath}") String managementPath) {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("management-endpoints")
                .select()
                .paths(path -> path.startsWith(managementPath))
                .build();
    }
}
