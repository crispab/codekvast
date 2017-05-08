package io.codekvast.warehouse.heroku;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.bind.DatatypeConverter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author olle.hallin@crisp.se
 */
@RestController
@Slf4j
public class HerokuController {

    @RequestMapping( path = "/heroku/resources", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<HerokuProvisionResponse> provision(@RequestHeader("Authorization") String auth, @RequestHeader HttpHeaders headers) {
        log.debug("auth={}", auth);
        log.debug("headers={}", headers);

        if (!validAuth(auth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(HerokuProvisionResponse
                                     .builder()
                                     .id("4711") // TODO: pick from customers table
                                     .config(getConfig())
                                     .build());

    }

    private boolean validAuth(String auth) {
        // The password is defined in addon-manifest.json

        String expected = "Basic " + DatatypeConverter.printBase64Binary("codekvast:2e54f4269dc7d2acdbe6c5d737d5371c".getBytes());
        return auth.equals(expected);
    }

    private Map<String, String> getConfig() {
        Map<String, String> result = new HashMap<>();
        result.put("CODEKVAST_URL", "http://localhost:8080");
        result.put("CODEKVAST_LICENSE_KEY", "");
        return result;
    }
}
