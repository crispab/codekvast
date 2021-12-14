package io.codekvast.javaagent.config;

import static io.codekvast.javaagent.config.MethodAnalyzerTest.TestCase.ofInput;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.codekvast.javaagent.util.SignatureUtils;
import io.codekvast.junit5.extensions.CaptureSystemOutput;
import io.codekvast.junit5.extensions.CaptureSystemOutput.OutputCapture;
import java.util.Arrays;
import java.util.List;
import lombok.ToString;
import lombok.Value;
import lombok.With;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@CaptureSystemOutput
class MethodAnalyzerTest {

  @ParameterizedTest
  @MethodSource("testCases")
  void should_parseVisibility(TestCase testCase, OutputCapture outputCapture) {
    // Given, When
    MethodAnalyzer analyzer = new MethodAnalyzer(testCase.input);

    // Then
    assertThat("Should select public", analyzer.selectsPublicMethods(), is(true));

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
        ofInput(null),
        ofInput("   "),
        ofInput("public"),
        ofInput(" public "),
        ofInput("PuBlIc"),
        ofInput("  foobar ")
            .withExpectedSystemErr(
                "Unrecognized value for methodVisibility: \"foobar\", assuming \"public\""),
        ofInput("protected")
            .withSelectsProtected(true)
            .withExpectedToString(SignatureUtils.PROTECTED),
        ofInput("package-private")
            .withSelectsProtected(true)
            .withSelectsPackagePrivate(true)
            .withExpectedToString(SignatureUtils.PACKAGE_PRIVATE),
        ofInput("!private")
            .withSelectsProtected(true)
            .withSelectsPackagePrivate(true)
            .withExpectedToString(SignatureUtils.PACKAGE_PRIVATE),
        ofInput("private")
            .withSelectsProtected(true)
            .withSelectsPackagePrivate(true)
            .withSelectsPrivate(true)
            .withExpectedToString(SignatureUtils.PRIVATE),
        ofInput("all")
            .withSelectsProtected(true)
            .withSelectsPackagePrivate(true)
            .withSelectsPrivate(true)
            .withExpectedToString(SignatureUtils.PRIVATE));
  }

  @Value
  @ToString(of = "input")
  static class TestCase {
    String input;
    @With boolean selectsProtected;
    @With boolean selectsPackagePrivate;
    @With boolean selectsPrivate;
    @With String expectedToString;
    @With String expectedSystemErr;

    static TestCase ofInput(String input) {
      return new TestCase(input, false, false, false, SignatureUtils.PUBLIC, null);
    }
  }
}
