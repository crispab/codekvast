package io.codekvast.common.logback

import ch.qos.logback.core.PropertyDefinerBase
import java.net.InetAddress

/**
 * A Logback property definer that makes the basename of localhost available in logback*.xml
 *
 * Example:
 *
 * <pre>{@code
 *   <configuration>
 *     <define name="hostname" class="io.codekvast.common.logback.HostnamePropertyDefiner"/>
 *     <property scope="context" name="host" value="${hostname}" />
 *     ...
 *   </configuration>
 * }</pre>
 *
 * @author olle.hallin@crisp.se
 */
@Suppress("unused")
class HostnamePropertyDefiner : PropertyDefinerBase() {
    override fun getPropertyValue(): String = InetAddress.getLocalHost().hostName.substringBefore('.')
}