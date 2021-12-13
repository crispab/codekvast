package io.codekvast.javaagent.config;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.codekvast.javaagent.util.SignatureUtils;
import io.codekvast.junit5.extensions.CaptureSystemOutput;
import io.codekvast.junit5.extensions.CaptureSystemOutput.OutputCapture;
import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@CaptureSystemOutput
public class MethodAnalyzerTest {

  @ParameterizedTest
  @MethodSource("testCases")
  void should_parseVisibility(TestCase testCase, OutputCapture outputCapture) {
    // Given, When
    MethodAnalyzer analyzer = new MethodAnalyzer(testCase.input);

    // Then
    assertThat("Should select public", analyzer.selectsPublicMethods(), is(testCase.selectsPublic));
    assertThat(
        "Should select protected",
        analyzer.selectsProtectedMethods(),
        is(testCase.selectsProtected));
    assertThat(
        "Should select package private",
        analyzer.selectsPackagePrivateMethods(),
        is(testCase.selectsPackagePrivate));
    assertThat(
        "Should select private", analyzer.selectsPrivateMethods(), is(testCase.selectsPrivate));

    assertThat("Should normalize toString()", analyzer.toString(), is(testCase.expectedToString));

    if (testCase.expectedSystemErr != null) {
      outputCapture.expect(containsString(testCase.expectedSystemErr));
    } else {
      outputCapture.expect(is(""));
    }
  }

  static List<TestCase> testCases() {
    return Arrays.asList(
        TestCase.builder()
            .input(null)
            .selectsPublic(true)
            .expectedToString(SignatureUtils.PUBLIC)
            .build(),
        TestCase.builder()
            .input("   ")
            .selectsPublic(true)
            .expectedToString(SignatureUtils.PUBLIC)
            .build(),
        TestCase.builder()
            .input("public")
            .selectsPublic(true)
            .expectedToString(SignatureUtils.PUBLIC)
            .build(),
        TestCase.builder()
            .input(" public ")
            .selectsPublic(true)
            .expectedToString(SignatureUtils.PUBLIC)
            .build(),
        TestCase.builder()
            .input("PuBlIc")
            .selectsPublic(true)
            .expectedToString(SignatureUtils.PUBLIC)
            .build(),
        TestCase.builder()
            .input("  foobar ")
            .selectsPublic(true)
            .expectedToString(SignatureUtils.PUBLIC)
            .expectedSystemErr(
                "Unrecognized value for methodVisibility: \"foobar\", assuming \"public\"")
            .build(),
        TestCase.builder()
            .input("protected")
            .selectsPublic(true)
            .selectsProtected(true)
            .expectedToString(SignatureUtils.PROTECTED)
            .build(),
        TestCase.builder()
            .input("package-private")
            .selectsPublic(true)
            .selectsProtected(true)
            .selectsPackagePrivate(true)
            .expectedToString(SignatureUtils.PACKAGE_PRIVATE)
            .build(),
        TestCase.builder()
            .input("!private")
            .selectsPublic(true)
            .selectsProtected(true)
            .selectsPackagePrivate(true)
            .expectedToString(SignatureUtils.PACKAGE_PRIVATE)
            .build(),
        TestCase.builder()
            .input("private")
            .selectsPublic(true)
            .selectsProtected(true)
            .selectsPackagePrivate(true)
            .selectsPrivate(true)
            .expectedToString(SignatureUtils.PRIVATE)
            .build(),
        TestCase.builder()
            .input("all")
            .selectsPublic(true)
            .selectsProtected(true)
            .selectsPackagePrivate(true)
            .selectsPrivate(true)
            .expectedToString(SignatureUtils.PRIVATE)
            .build());
  }

  @Value
  @Builder
  @ToString(of = "input")
  static class TestCase {
    String input;
    boolean selectsPublic;
    boolean selectsProtected;
    boolean selectsPackagePrivate;
    boolean selectsPrivate;
    String expectedToString;
    String expectedSystemErr;
  }
}
