package se.crisp.codekvast.warehouse.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.UiConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.inject.Inject;

/**
 * @author olle.hallin@crisp.se
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {

    private final CodekvastSettings settings;

    @Inject
    public SwaggerConfig(CodekvastSettings settings) {
        this.settings = settings;
    }

    @Bean
    public Docket swaggerDocket() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(swaggerApiInfo())
                .select()
                .paths(path -> path.contains("api/"))
                .build();
    }

    @Bean
    UiConfiguration swaggerUiConfig() {
        // disable validationUrl
        return new UiConfiguration(null);
    }

    @Bean
    ApiInfo swaggerApiInfo() {
        return new ApiInfo(settings.getApplicationName(),
                           "Codekvast Warehouse description",
                           settings.getDisplayVersion(),
                           "http://codekvast.crisp.se",
                           new Contact("Olle Hallin", "https://www.crisp.se/konsulter/olle.hallin", "olle.hallin@crisp.se"),
                           "MIT",
                           "https://opensource.org/licenses/MIT");

    }
}
