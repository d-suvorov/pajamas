package pajamas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public /* namespace */ class Pajamas {
  public record State(String input, int offset) {
    public boolean eof() {
      return offset == input.length();
    }

    public Pair<Character, State> read() {
      if (eof()) throw new IllegalStateException("reading from an empty state");
      return Pair.of(input.charAt(offset), new State(input, offset + 1));
    }

    public String str() {
      return input.substring(offset);
    }

    public static State init(String s) {
      return new State(s, 0);
    }
  }

  public record ParseResult<R>(State state, R result) {
    public static <R> ParseResult<R> of(State state, R result) {
      return new ParseResult<>(state, result);
    }
  }

  public interface Parser<R> {
    Optional<ParseResult<R>> run(State state);

    static <R> Parser<R> ret(R result) {
      return state -> Optional.of(new ParseResult<>(state, result));
    }

    static <R1, R2> Parser<R2> chain(Parser<R1> p,
                                     Function<R1, Parser<R2>> f) {
      return state -> {
        var maybeRes1 = p.run(state);
        if (maybeRes1.isEmpty()) {
          return Optional.empty();
        } else {
          var res1 = maybeRes1.get();
          var p2 = f.apply(res1.result);
          return p2.run(res1.state);
        }
      };
    }
  }

  public static class Seq<R> implements Parser<List<R>> {
    private final List<? extends Parser<R>> parsers;

    public Seq(List<? extends Parser<R>> parsers) {
      this.parsers = parsers;
    }

    @Override
    public Optional<ParseResult<List<R>>> run(State state) {
      var result = new ArrayList<R>();
      var current = state;
      for (Parser<R> p : parsers) {
        var maybeRes = p.run(current);
        if (maybeRes.isEmpty()) return Optional.empty();
        var res = maybeRes.get();
        result.add(res.result);
        current = res.state();
      }
      return Optional.of(ParseResult.of(current, result));
    }
  }

  public static class Ok implements Parser<Top> {
    @Override
    public Optional<ParseResult<Top>> run(State state) {
      return Optional.of(ParseResult.of(state, Top.T));
    }
  }

  public static class End implements Parser<Top> {
    @Override
    public Optional<ParseResult<Top>> run(State state) {
      return state.eof() ? Optional.of(ParseResult.of(state, Top.T))
                         : Optional.empty();
    }
  }

  public static class Like implements Parser<Character> {
    private final Predicate<Character> like;

    public Like(Predicate<Character> like) {
      this.like = like;
    }

    @Override
    public Optional<ParseResult<Character>> run(State state) {
      if (state.eof()) return Optional.empty();
      Pair<Character, State> read = state.read();
      return like.test(read.fst) ? Optional.of(ParseResult.of(read.snd, read.fst))
                                 : Optional.empty();
    }
  }

  public static class OneOf implements Parser<Character> {
    private final String set;

    public OneOf(String set) {
      this.set = set;
    }

    @Override
    public Optional<ParseResult<Character>> run(State state) {
      return new Like(next -> set.indexOf(next) != -1).run(state);
    }
  }

  public static class Or<R> implements Parser<R> {
    private final Parser<R> left;
    private final Parser<R> right;

    public Or(Parser<R> left, Parser<R> right) {
      this.left  = left;
      this.right = right;
    }

    @Override
    public Optional<ParseResult<R>> run(State state) {
      return left.run(state)
                 .or(() -> right.run(state));
    }
  }

  public static class Star<R> implements Parser<List<R>> {
    private final Parser<R> p;

    public Star(Parser<R> p) {
      this.p = p;
    }

    @Override
    public Optional<ParseResult<List<R>>> run(State state) {
      return new Or<>(new Plus<>(p), Parser.ret(Collections.emptyList())).run(state);
    }
  }

  public static class Plus<R> implements Parser<List<R>> {
    private final Parser<R> p;

    public Plus(Parser<R> p) {
      this.p = p;
    }

    @Override
    public Optional<ParseResult<List<R>>> run(State state) {
      Function<R, Parser<List<R>>> f = (R r1) -> (State s2) -> {
        var r2 = new Star<>(p).run(s2).orElseThrow();
        return Optional.of(ParseResult.of(r2.state, cons(r1, r2.result)));
      };
      return Parser.chain(p, f).run(state);
    }

    private List<R> cons(R head, List<R> tail) {
      var result = new ArrayList<R>(tail.size() + 1);
      result.add(head);
      result.addAll(tail);
      return result;
    }
  }

  public static class Option<R> implements Parser<Optional<R>> {
    private final Parser<R> p;

    public Option(Parser<R> p) {
      this.p = p;
    }

    @Override
    public Optional<ParseResult<Optional<R>>> run(State state) {
      Parser<Optional<R>> one = Parser.chain(p, r -> Parser.ret(Optional.of(r)));
      Parser<Optional<R>> zero = Parser.ret(Optional.empty());
      var res = new Or<>(one, zero).run(state);
      if (res.isEmpty()) throw new AssertionError("Option should never fail");
      return res;
    }
  }

  public static class Char implements Parser<Character> {
    private final char like;

    public Char(char like) {
      this.like = like;
    }

    @Override
    public Optional<ParseResult<Character>> run(State state) {
      return new Like(next -> next == like).run(state);
    }
  }

  public static class Str implements Parser<String> {
    private final String str;

    public Str(String str) {
      this.str = str;
    }

    @Override
    public Optional<ParseResult<String>> run(State state) {
      List<Char> parsers = str.chars()
                              .mapToObj(c -> new Char((char) c))
                              .toList();
      return new Seq<>(parsers).run(state).map(r -> {
        var sb = new StringBuilder();
        r.result.forEach(sb::append);
        return ParseResult.of(r.state, sb.toString());
      });
    }
  }

  public static class Top {
    public static Top T = new Top();
    private Top() {}
  }

  public record Pair<A, B>(A fst, B snd) {
    public static <A, B> Pair<A, B> of(A fst, B snd) {
      return new Pair<>(fst, snd);
    }
  }

}
