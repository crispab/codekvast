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
package io.codekvast.javaagent;

import io.codekvast.javaagent.config.AgentConfig;
import io.codekvast.javaagent.config.AgentConfigFactory;
import io.codekvast.javaagent.config.AgentConfigLocator;
import io.codekvast.javaagent.config.MethodAnalyzer;
import io.codekvast.javaagent.publishing.impl.CodeBasePublisherFactoryImpl;
import io.codekvast.javaagent.publishing.impl.InvocationDataPublisherFactoryImpl;
import io.codekvast.javaagent.scheduler.ConfigPollerImpl;
import io.codekvast.javaagent.scheduler.Scheduler;
import io.codekvast.javaagent.scheduler.SystemClockImpl;
import io.codekvast.javaagent.util.FileUtils;
import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import lombok.extern.java.Log;
import org.aspectj.bridge.Constants;

/**
 * This is the Java javaagent that hooks up Codekvast to the app.
 *
 * <p>Invocation: Add the following options to the Java command line:
 *
 * <pre><code>
 *    -javaagent:/path/to/codekvast-agent-n.n.jar
 * </code></pre>
 *
 * <p>CodekvastAgent could also be initialized from a statically woven aspect.
 *
 * <p>In that case, the aspect should have a static block that locates the config and initializes
 * the agent:
 *
 * <pre><code>
 *     public aspect MethodExecutionAspect extends AbstractMethodExecutionAspect {
 *
 *         static {
 *             AgentConfig config = ...
 *             CodekvastAgent.initialize(config);
 *         }
 *
 *         public pointcut methodExecution: execution(public * *..*(..)) &amp;&amp; within(foo..*)
 *
 *     }
 * </code></pre>
 *
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings("unused")
@Log
public class CodekvastAgent {

  private static final String NAME = "Codekvast";

  // AspectJ uses this system property for defining the list of names of load-time weaving config
  // files to locate...
  private static final String ASPECTJ_WEAVER_CONFIGURATION =
      "org.aspectj.weaver.loadtime.configuration";

  private static Scheduler scheduler;

  private CodekvastAgent() {
    // Not possible to instantiate a javaagent
  }

  /**
   * This method is invoked by the JVM as part of bootstrapping the -javaagent
   *
   * @param args The string after the equals sign in -javaagent:codekvast-agent.jar=args. Is used as
   *     overrides to the agent configuration file.
   * @param instrumentation The standard instrumentation hook.
   */
  public static void premain(String args, Instrumentation instrumentation) {
    AgentConfig config =
        AgentConfigFactory.parseAgentConfig(AgentConfigLocator.locateConfig(), args, true);

    initialize(config);

    if (shouldStart(config)) {
      // Weave io.codekvast.javaagent.MethodExecutionAgent
      org.aspectj.weaver.loadtime.Agent.premain(args, instrumentation);
    } else if (config != null) {
      logger.info(String.format("%s is disabled", NAME));
    }
  }

  private static boolean shouldStart(AgentConfig config) {
    return config != null && config.isEnabled();
  }

  /**
   * Initializes CodekvastAgent. Before this method has been invoked, no method invocations are
   * recorded.
   *
   * @param config The configuration object. May be null, in which case Codekvast is disabled. Also,
   *     if config.isEnabled() == false, Codekvast is disabled.
   */
  public static void initialize(AgentConfig config) {
    if (!shouldStart(config)) {
      if (scheduler != null) {
        scheduler.shutdown();
        scheduler = null;
      }
      InvocationRegistry.initialize(null);
      return;
    }

    if (scheduler != null) {
      // Already initialized from -javaagent. Let it be.
      return;
    }

    try {
      defineAspectjLoadTimeWeaverConfig(config);

      InvocationRegistry.initialize(config);

      scheduler =
          new Scheduler(
                  config,
                  new ConfigPollerImpl(config),
                  new CodeBasePublisherFactoryImpl(),
                  new InvocationDataPublisherFactoryImpl(),
                  new SystemClockImpl())
              .start();

      Runtime.getRuntime().addShutdownHook(new MyShutdownHook());

      logger.info(
          String.format(
              "%s is ready to detect used code in %s %s within %s.",
              NAME,
              config.getAppName(),
              config.getResolvedAppVersion(),
              getPrettyPackages(config)));
    } catch (Exception e) {
      logger.log(
          Level.WARNING,
          String.format(
              "%1$s could not add generated META-INF/aop.xml to the system class loader. %1$s will not start",
              NAME),
          e);
    }
  }

  private static String getPrettyPackages(AgentConfig config) {
    List<String> prefixes = config.getNormalizedPackages();
    return prefixes.size() == 1 ? "package " + prefixes.get(0) : "packages " + prefixes.toString();
  }

  /**
   * Creates a concrete implementation of the AbstractMethodExecutionAspect, using the packages for
   * specifying the abstract pointcut 'scope'.
   *
   * <p>It does this by creating a temporary directory, and in that directory a META-INF/aop.xml.
   *
   * <p>Adds the containing META-INF/ to the system class loader, where it will be picked up by the
   * Aspectj Weaver. If running under Java 9+, it will instead set some system properties recognized
   * by AspectJ, to make it load the generated aop.xml.
   */
  private static void defineAspectjLoadTimeWeaverConfig(AgentConfig config) throws Exception {
    String messageHandlerClass =
        config.isBridgeAspectjMessagesToJUL()
            ? String.format("-XmessageHandlerClass:%s ", AspectjMessageHandler.class.getName())
            : "";

    String xml =
        String.format(
            "<aspectj>\n"
                + "  <aspects>\n"
                + "    <concrete-aspect name='%1$s.MethodExecutionAspect'\n"
                + "                     extends='%2$s'>\n"
                + "      <pointcut name='methodExecution' expression='%3$s'/>\n"
                + "    </concrete-aspect>\n"
                + "  </aspects>\n"
                + "  <weaver options='%4$s'>\n"
                + "%5$s"
                + "%6$s"
                + "  </weaver>\n"
                + "</aspectj>\n",
            AbstractMethodExecutionAspect.class.getPackage().getName(),
            AbstractMethodExecutionAspect.class.getName(),
            toMethodExecutionPointcut(config.getMethodAnalyzer()),
            messageHandlerClass + config.getAspectjOptions(),
            getIncludeExcludeElements("include", config.getNormalizedPackages()),
            getIncludeExcludeElements(
                "exclude", config.getNormalizedExcludePackages(), "io.codekvast.javaagent", "ck"));
    logger.finest("aop.xml=" + xml);

    createAopXmlAndMakeItVisibleToAspectjWeaver(xml);
  }

  private static void createAopXmlAndMakeItVisibleToAspectjWeaver(String xml) throws Exception {
    File tmpDir = File.createTempFile("codekvast-", "");
    // tmpDir is now a file with a unique name. Delete it, so that the name can be reused as a
    // directory name.
    tmpDir.delete();

    File aopXml = new File(tmpDir, "META-INF/aop.xml");

    logger.fine("META-INF/aop.xml = " + aopXml.getAbsolutePath());

    FileUtils.writeToFile(xml, aopXml);

    makeAopXmlVisibleToAspectjWeaver(tmpDir, aopXml);

    tmpDir.deleteOnExit();
    aopXml.getParentFile().deleteOnExit();
    aopXml.deleteOnExit();
  }

  private static void makeAopXmlVisibleToAspectjWeaver(File dir, File aopXml) throws Exception {
    try {
      URLClassLoader systemClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
      Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
      method.setAccessible(true);
      method.invoke(systemClassLoader, dir.toURI().toURL());
    } catch (ClassCastException e) {
      logger.fine(
          "Running on Java 9+. Not possible to augment system class loader. Setting AspectJ system properties instead.");

      // Make aop.xml visible to AspectJ Weaver by prepending the search path with an absolute file:
      // URL
      System.setProperty(
          ASPECTJ_WEAVER_CONFIGURATION,
          "file:"
              + aopXml.getAbsolutePath()
              + ";"
              + Constants.AOP_USER_XML
              + ";"
              + Constants.AOP_AJC_XML
              + ";"
              + Constants.AOP_OSGI_XML);
      // Prevent the PlatformClassLoader from trying to load the aspect (since it is defined in a
      // child class loader)...
      System.setProperty(
          "aj.weaving.loadersToSkip", "jdk.internal.loader.ClassLoaders$PlatformClassLoader");
    }
  }

  private static String getIncludeExcludeElements(
      String element, List<String> packages, String... extraPrefixes) {
    StringBuilder sb = new StringBuilder();

    Set<String> prefixes = new HashSet<>(packages);
    Collections.addAll(prefixes, extraPrefixes);

    for (String prefix : prefixes) {
      sb.append(String.format("    <%s within='%s..*' />\n", element, prefix));
    }
    return sb.toString();
  }

  private static String toMethodExecutionPointcut(MethodAnalyzer filter) {
    if (filter.selectsPrivateMethods()) {
      return "execution(* *..*(..)) || execution(*..new(..))";
    }
    if (filter.selectsPackagePrivateMethods()) {
      return "execution(!private * *..*(..)) || execution(!private *..new(..))";
    }
    if (filter.selectsProtectedMethods()) {
      return "execution(public * *..*(..)) || execution(protected * *..*(..)) "
          + "|| execution(public *..new(..)) || execution(protected *..new(..))";
    }
    return "execution(public * *..*(..)) || execution(public *..new(..))";
  }

  private static class MyShutdownHook extends Thread {

    MyShutdownHook() {
      setName(NAME + " shutdown hook");

      setContextClassLoader(null);

      //noinspection InnerClassTooDeeplyNested,AnonymousInnerClass
      setUncaughtExceptionHandler(
          new UncaughtExceptionHandler() {

            @SuppressWarnings("UseOfSystemOutOrSystemErr")
            @Override
            public void uncaughtException(Thread t, Throwable e) {
              System.err.println(NAME + " Uncaught exception in  " + t.getName());
              e.printStackTrace(System.err);
            }
          });
    }

    @Override
    public void run() {
      // Cannot use logger here, since logging could have been shut down already

      //noinspection UseOfSystemOutOrSystemErr
      System.err.println(NAME + " is shutting down...");
      long startedAt = System.currentTimeMillis();

      initialize(null);

      long elapsed = System.currentTimeMillis() - startedAt;

      //noinspection UseOfSystemOutOrSystemErr
      System.err.println(NAME + " shutdown completed in " + elapsed + " ms");
    }
  }
}
