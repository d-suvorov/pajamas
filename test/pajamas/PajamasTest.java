package pajamas;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static pajamas.Pajamas.*;

class PajamasTest {
  @Test
  void testOk() {
    var input = "abacaba";
    mustParse(input, ok(), Top.T, input);
  }

  @Test
  void matchEnd() {
    mustParse("", end(), Top.T, "");
  }

  @Test
  void notEnd() {
    mustFail("abacaba", end());
  }

  @Test
  public void charMatch() {
    var input = "abacaba";
    mustParse(input, chr('a'), input.charAt(0), input.substring(1));
  }

  @Test
  public void charNotMatch() {
    mustFail("abacaba", chr('b'));
  }

  @Test
  public void charEmpty() {
    mustFail("", chr('a'));
  }

  @Test
  void oneOfMatch() {
    var input = "abacaba";
    mustParse(input, oneOf("lamp"), 'a', input.substring(1));
  }

  @Test
  void oneOfNotMatch() {
    var input = "abacaba";
    mustFail(input, oneOf("xyz"));
  }

  @Test
  void stringMatch() {
    var input = "abacaba";
    int pivot = 3;
    var prefix = input.substring(0, pivot);
    var suffix = input.substring(pivot);
    mustParse(input, str(prefix), prefix, suffix);
  }

  @Test
  void stringNotMatch() {
    mustFail("abacaba", str("abc"));
  }

  @Test
  void stringEof() {
    mustFail("aba", str("abac"));
  }

  @Test
  void optionMatch() {
    var input = "abacaba";
    var pivot = 3;
    var prefix = input.substring(0, pivot);
    var suffix = input.substring(pivot);
    mustParse(input, option(str(prefix)), Optional.of(prefix), suffix);
  }

  @Test
  void optionMatchEmpty() {
    var input = "abacaba";
    mustParse(input, option(str("aca")), Optional.empty(), input);
  }

  @Test
  void starMatch() {
    var input = "aaab";
    mustParse(input, star(chr('a')), List.of('a', 'a', 'a'), input.substring(3));
  }

  @Test
  void starEmpty() {
    var input = "aaab";
    mustParse(input, star(chr('b')), Collections.emptyList(), input);
  }

  @Test
  void plusMatchOne() {
    var input = "abbb";
    mustParse(input, plus(chr('a')), List.of('a'), input.substring(1));
  }

  @Test
  void plusMatchMore() {
    var input = "aaab";
    mustParse(input, plus(chr('a')), List.of('a', 'a', 'a'), input.substring(3));
  }

  @Test
  void plusNotMatch() {
    var input = "aaab";
    mustFail(input, plus(chr('b')));
  }

  private static <R> void mustParse(String input, Parser<R> parser,
                                    R expectedResult, String expectedInput) {
    var maybeResult = run(parser, input);
    Assertions.assertTrue(maybeResult.isPresent());
    var result = maybeResult.get();
    Assertions.assertEquals(expectedResult, result.result());
    Assertions.assertEquals(expectedInput, result.state().str());
  }

  private static <R> void mustFail(String input, Parser<R> parser) {
    var maybeResult = run(parser, input);
    Assertions.assertTrue(maybeResult.isEmpty());
  }

  private static <R> Optional<ParseResult<R>> run(Parser<R> p, String s) {
    return p.run(State.init(s));
  }
}
