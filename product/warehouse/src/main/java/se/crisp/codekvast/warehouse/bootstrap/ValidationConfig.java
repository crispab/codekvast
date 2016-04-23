package se.crisp.codekvast.warehouse.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

/**
 * Enable support for JSR-303 also on service layer.
 *
 * @author olle.hallin@crisp.se
 */
@Configuration
public class ValidationConfig {

    @Bean
    MethodValidationPostProcessor methodValidationPostProcessor() {
        return new MethodValidationPostProcessor();
    }
}
