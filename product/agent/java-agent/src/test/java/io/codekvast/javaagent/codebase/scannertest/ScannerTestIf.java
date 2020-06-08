package io.codekvast.javaagent.codebase.scannertest;

/** @author olle.hallin@crisp.se */
public interface ScannerTestIf {
  void m1();

  default String defaultMethod_m2() {
    return "defaultMethod_m2";
  }
}
