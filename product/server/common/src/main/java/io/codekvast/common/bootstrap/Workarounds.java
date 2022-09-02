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
package io.codekvast.common.bootstrap;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.Provider;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/** @author olle.hallin@crisp.se */
@Configuration
@Slf4j
public class Workarounds {

  /**
   * Patches a memory leak in JceSecurity.
   *
   * <p>The bug is provoked by using spring-boot-starter-mail and
   * spring.mail.properties.mail.smtp.starttls.enabled=true. Each time /management/health/mail is
   * invoked, some memory is leaked.
   *
   * @see <a href="https://bugs.openjdk.java.net/browse/JDK-8168469">JDK-8168469</a>
   */
  @SneakyThrows
  @PostConstruct
  public void patchMemoryLeakIn_javax_crypto_JceSecurity() {
    Class<?> cl = Class.forName("javax.crypto.JceSecurity");
    logger.info("Monkey patching memory leak in {}", cl.getName());

    Field field = cl.getDeclaredField("verificationResults");
    field.setAccessible(true);

    Field modifiersField = Field.class.getDeclaredField("modifiers");
    modifiersField.setAccessible(true);
    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

    Map<Provider, Object> map = Collections.synchronizedMap(new WeakHashMap<>());
    //noinspection unchecked
    Map<Provider, Object> verificationResults = (Map<Provider, Object>) field.get(null);
    map.putAll(verificationResults);
    field.set(null, map);
  }
}
