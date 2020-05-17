/*
 * Copyright (c) 2015-2020 Hallin Information Technology AB
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
package io.codekvast.backoffice.http

import io.codekvast.common.bootstrap.CodekvastCommonSettings
import io.codekvast.common.util.LoggerDelegate
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestHeader

/** @author olle.hallin@crisp.se
 */
@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
class ModelAttributes(val commonSettings: CodekvastCommonSettings) {

  val logger by LoggerDelegate()

  @ModelAttribute("settings")
  fun getSettings() = commonSettings

  @ModelAttribute("cookieConsent")
  fun getCookieConsent(
    @CookieValue(name = "cookieConsent", defaultValue = "FALSE") cookieConsent: Boolean?): Boolean {
    logger.trace("cookieConsent={}", cookieConsent)
    return cookieConsent ?: false
  }

  @ModelAttribute("cookieDomain")
  fun cookieDomain(@RequestHeader("Host") requestHost: String): String {
    logger.trace("requestHost={}", requestHost)
    return if (requestHost.startsWith("localhost")) "localhost" else ".codekvast.io"
  }

}
