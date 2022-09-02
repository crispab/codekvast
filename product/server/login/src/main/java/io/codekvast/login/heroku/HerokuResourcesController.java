/*
 * Copyright (c) 2015-2022 Hallin Information Technology AB
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
package io.codekvast.login.heroku;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import io.codekvast.login.bootstrap.CodekvastLoginSettings;
import io.codekvast.login.heroku.model.HerokuChangePlanRequest;
import io.codekvast.login.heroku.model.HerokuProvisionRequest;
import io.codekvast.login.heroku.model.HerokuProvisionResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;

/**
 * CRUD REST endpoints invoked by Heroku when doing 'heroku addons:create codekvast' etc.
 *
 * @author olle.hallin@crisp.se
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class HerokuResourcesController {

  private final CodekvastLoginSettings settings;
  private final HerokuService herokuService;

  @ExceptionHandler
  public ResponseEntity<String> onBadCredentialsException(BadCredentialsException e) {
    logger.warn("Invalid credentials");
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  }

  @ExceptionHandler
  public ResponseEntity<String> onHerokuException(HerokuException e) {
    logger.warn("Bad request");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
  }

  @PostMapping(path = "/heroku/resources")
  public ResponseEntity<HerokuProvisionResponse> provision(
      @Valid @RequestBody HerokuProvisionRequest request,
      @RequestHeader(AUTHORIZATION) String authorization)
      throws HerokuException {
    logger.debug("request={}", request);

    validateBasicAuth(authorization);

    return ResponseEntity.ok(herokuService.provision(request));
  }

  @PutMapping(path = "/heroku/resources/{id}")
  public ResponseEntity<String> changePlan(
      @PathVariable("id") String id,
      @Valid @RequestBody HerokuChangePlanRequest request,
      @RequestHeader(AUTHORIZATION) String auth)
      throws HerokuException {
    logger.debug("id={}, request={}", id, request);

    validateBasicAuth(auth);

    herokuService.changePlan(id, request);
    return ResponseEntity.ok("{}");
  }

  @DeleteMapping(path = "/heroku/resources/{id}")
  public ResponseEntity<String> deprovision(
      @PathVariable("id") String id, @RequestHeader(AUTHORIZATION) String auth)
      throws HerokuException {
    logger.debug("id={}", id);

    validateBasicAuth(auth);

    herokuService.deprovision(id);

    return ResponseEntity.ok("{}");
  }

  void validateBasicAuth(String authentication) throws BadCredentialsException {
    logger.debug("authentication={}", authentication);

    // The password is defined in <rootDir>/deploy/vars/secrets.yml, and it has been pushed to
    // Heroku by means
    // of <rootDir>/deploy/push-addon-manifest-to-heroku.sh

    val credentials = "codekvast:" + settings.getHerokuApiPassword();
    val expected =
        "Basic "
            + new String(
                Base64.getEncoder().encode(credentials.getBytes()), StandardCharsets.UTF_8);
    val normalizedAuth = authentication.replaceAll("^[Bb][Aa][Ss][Ii][Cc] ", "Basic ");
    if (!normalizedAuth.equals(expected)) {
      throw new BadCredentialsException("Invalid credentials: " + authentication);
    }
  }

  /** A filter which logs the raw request body in requests to /heroku/resources. */
  @Component
  @Slf4j
  public static class HerokuResourcesRequestLoggingFilter extends OncePerRequestFilter
      implements Ordered {

    @Override
    public int getOrder() {
      return Ordered.LOWEST_PRECEDENCE - 8;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
      val wrappedRequest = new ContentCachingRequestWrapper(request);

      filterChain.doFilter(wrappedRequest, response);

      val path = request.getRequestURI();
      if (path.startsWith("/heroku/resources")) {
        HerokuResourcesRequestLoggingFilter.logger.debug(
            "{} {}, body=\n{}", request.getMethod(), path, getBody(wrappedRequest));
      }
    }

    private String getBody(ContentCachingRequestWrapper request) {
      String payload = null;
      val wrapper = WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
      if (wrapper != null) {
        byte[] buf = wrapper.getContentAsByteArray();
        if (buf.length > 0) {
          try {
            payload = new String(buf, 0, buf.length, wrapper.getCharacterEncoding());
          } catch (UnsupportedEncodingException ex) {
            payload = "[unknown]";
          }
        }
      }
      return payload;
    }
  }
}
