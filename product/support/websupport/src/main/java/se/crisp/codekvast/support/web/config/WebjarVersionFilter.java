package se.crisp.codekvast.support.web.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A filter that detects webjars in classpath and rewrites requests for version-less webjar resources to their canonical form. <br/> <br/>
 * It makes it possible to declare webjar versions only once, in build.gradle. This reduces risk for errors when updating dependencies.
 * Forgetting to update the html templates will most likely be detected very late in the deployment pipeline. <br/> <br/> The HTML file can
 * use e.g.,
 * <code><pre>
 * <p/>
 *   &lt;script src="/webjars/angularjs/angular.js"&gt;&lt;/script&gt;
 * <p/>
 * </pre> </code>
 * instead of
 * <code><pre>
 * <p/>
 *   &lt;script src="/webjars/angularjs/<b>1.3.10</b>/angular.js"&gt;&lt;/script&gt;
 * <p/>
 * </pre> </code>
 * where <b>1.3.10</b> also appears in the build.gradle that brought in angular-1.3.10.jar in the first place.
 *
 * @author olle.hallin@crisp.se
 */
@Slf4j
@WebFilter(urlPatterns = "/*")
@Component
public class WebjarVersionFilter implements Filter {

    @Getter(AccessLevel.MODULE)
    private final Map<String, String> versions = new HashMap<>();

    final Pattern requestUriPattern = Pattern.compile("^(/webjars/)([\\w-]+)(/\\D.*)$");

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        scanWebjars();
    }

    @Override
    public void destroy() {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String requestURI = ((HttpServletRequest) request).getRequestURI();
        String expandedRequestURI = expandRequestURI(requestURI);
        if (expandedRequestURI != null) {
            log.debug("Forwarding {} to {}", requestURI, expandedRequestURI);
            request.getRequestDispatcher(expandedRequestURI).forward(request, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    String expandRequestURI(String requestURI) {
        if (requestURI == null) {
            return null;
        }

        Matcher matcher = requestUriPattern.matcher(requestURI);
        if (matcher.find()) {
            String jarName = matcher.group(2);
            String version = versions.get(jarName);
            if (version != null) {
                return matcher.group(1) + jarName + "/" + version + matcher.group(3);
            }
        }
        return null;
    }

    private void scanWebjars() {
        ClassLoader cl = WebjarVersionFilter.class.getClassLoader();

        if (!(cl instanceof URLClassLoader)) {
            throw new UnsupportedOperationException("Don't know how to scan classpath from " + cl.getClass().getName());
        }

        log.debug("Scanning classpath for webjars...");

        URLClassLoader ucl = (URLClassLoader) cl;
        Pattern pattern = Pattern.compile(".*webjars/(.*?)/(.*?)/.*");

        // For each URL in classpath
        for (URL url : ucl.getURLs()) {
            try {
                analyzeJar(url, pattern);
            } catch (IOException e) {
                log.warn("Cannot analyze " + url, e);
            }
        }
        log.debug("Found {} webjars", versions.size());
    }

    private void analyzeJar(URL jarUrl, Pattern pattern) throws IOException {
        // Look for entries that match the pattern...
        JarInputStream inputStream = new JarInputStream(jarUrl.openStream());

        JarEntry jarEntry = inputStream.getNextJarEntry();
        while (jarEntry != null) {
            log.trace("Considering {}", jarEntry);

            Matcher matcher = pattern.matcher(jarEntry.getName());
            if (matcher.matches()) {
                String key = matcher.group(1);
                String value = matcher.group(2);
                versions.put(key, value);
                log.debug("{} is a webjar, adding {}={} to map", basename(jarUrl.getPath()), key, value);
                return;
            }
            jarEntry = inputStream.getNextJarEntry();
        }
    }

    private String basename(String path) {
        int slash = path.replace('\\', '/').lastIndexOf('/');
        return path.substring(slash + 1);
    }
}
