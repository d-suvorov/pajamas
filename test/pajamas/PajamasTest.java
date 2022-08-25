package pajamas;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pajamas.Pajamas.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

class PajamasTest {
  @Test
  void ok() {
    var input = "abacaba";
    mustParse(input, new Ok(), Top.T, input);
  }

  @Test
  void eof() {
    mustParse("", new End(), Top.T, "");
  }

  @Test
  void notEof() {
    mustFail("abacaba", new End());
  }

  @Test
  public void charMatch() {
    var input = "abacaba";
    mustParse(input, new Char('a'), input.charAt(0), input.substring(1));
  }

  @Test
  public void charNotMatch() {
    mustFail("abacaba", new Char('b'));
  }

  @Test
  public void charEmpty() {
    mustFail("", new Char('a'));
  }

  @Test
  void oneOfMatch() {
    var input = "abacaba";
    mustParse(input, new OneOf("lamp"), 'a', input.substring(1));
  }

  @Test
  void oneOfNotMatch() {
    var input = "abacaba";
    mustFail(input, new OneOf("xyz"));
  }

  @Test
  void stringMatch() {
    var input = "abacaba";
    int pivot = 3;
    var prefix = input.substring(0, pivot);
    var suffix = input.substring(pivot);
    mustParse(input, new Str(prefix), prefix, suffix);
  }

  @Test
  void stringNotMatch() {
    mustFail("abacaba", new Str("abc"));
  }

  @Test
  void stringEof() {
    mustFail("aba", new Str("abac"));
  }

  @Test
  void optionMatch() {
    var input = "abacaba";
    var pivot = 3;
    var prefix = input.substring(0, pivot);
    var suffix = input.substring(pivot);
    mustParse(input, new Option<>(new Str(prefix)), Optional.of(prefix), suffix);
  }

  @Test
  void optionMatchEmpty() {
    var input = "abacaba";
    mustParse(input, new Option<>(new Str("aca")), Optional.empty(), input);
  }

  @Test
  void starMatch() {
    var input = "aaab";
    mustParse(input, new Star<>(new Char('a')), List.of('a', 'a', 'a'), input.substring(3));
  }

  @Test
  void starEmpty() {
    var input = "aaab";
    mustParse(input, new Star<>(new Char('b')), Collections.emptyList(), input);
  }

  @Test
  void plusMatchOne() {
    var input = "abbb";
    mustParse(input, new Plus<>(new Char('a')), List.of('a'), input.substring(1));
  }

  @Test
  void plusMatchMore() {
    var input = "aaab";
    mustParse(input, new Plus<>(new Char('a')), List.of('a', 'a', 'a'), input.substring(3));
  }

  @Test
  void plusNotMatch() {
    var input = "aaab";
    mustFail(input, new Plus<>(new Char('b')));
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
