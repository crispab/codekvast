package se.crisp.codekvast.web.config;

import com.planetj.servlet.filter.compression.CompressingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;

/**
 * Configures the servlet container..
 *
 * @author Olle Hallin
 */
@Configuration
public class ServletContainerConfig {

    @Bean
    public Filter compressingFilter() {
        return new CompressingFilter();
    }

}
