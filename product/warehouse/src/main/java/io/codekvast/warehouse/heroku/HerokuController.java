/*
 * Copyright (c) 2015-2017 Crisp AB
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
package io.codekvast.warehouse.heroku;

import io.codekvast.warehouse.bootstrap.CodekvastSettings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.xml.bind.DatatypeConverter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author olle.hallin@crisp.se
 */
@RestController
@Slf4j
public class HerokuController {

    private final CodekvastSettings settings;

    @Inject
    public HerokuController(CodekvastSettings settings) {
        this.settings = settings;
    }

    @RequestMapping(path = "/heroku/resources", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<HerokuProvisionResponse> provision(@Valid @RequestBody HerokuProvisionRequest request,
                                                             @RequestHeader("Authorization") String auth) {
        log.debug("request={}", request);
        log.debug("auth={}", auth);

        if (!validAuth(auth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // TODO: implement provisioning

        return ResponseEntity.ok(HerokuProvisionResponse
                                     .builder()
                                     .id("4711") // TODO: pick from customers table
                                     .config(getConfig())
                                     .build());

    }

    @RequestMapping(path = "/heroku/resources/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<String> deprovision(@PathVariable("id") String id,
                                              @RequestHeader("Authorization") String auth) {
        log.debug("id={}", id);

        if (!validAuth(auth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // TODO: implement deprovisioning

        return ResponseEntity.ok("{}");
    }

    private boolean validAuth(String auth) {
        // The password is also defined in src/heroku/addon-manifest.json which has been uploaded to Heroku.

        String credentials = "codekvast:" + settings.getHerokuApiPassword();
        String expected = "Basic " + DatatypeConverter.printBase64Binary(credentials.getBytes());
        return auth.equals(expected);
    }

    private Map<String, String> getConfig() {
        // TODO: implement
        Map<String, String> result = new HashMap<>();
        result.put("CODEKVAST_APP_NAME", "The name of the instrumented app");
        result.put("CODEKVAST_URL", "http://localhost:8080");
        result.put("CODEKVAST_LICENSE_KEY", "");
        return result;
    }
}
