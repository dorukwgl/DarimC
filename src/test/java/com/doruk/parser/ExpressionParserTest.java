package com.doruk.parser;

import com.doruk.dto.Token;
import com.doruk.lexer.Lexer;
import com.doruk.lexer.TokenType;
import com.doruk.parser.interfaces.Expr;
import com.doruk.parser.nodes.expr.*;
import com.doruk.parser.nodes.types.PrimitiveType;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * =====================================================================
 *  EXPRESSION PARSER — TDD SPEC (plain-java, no JUnit)
 * =====================================================================
 *  Pipeline per test:  source -> Lexer -> ParserState ->
 *  ExpressionParser.parseExpression() -> structural asserts on the AST.
 *  Prints PASS/FAIL per case (expected vs got via a compact dump),
 *  exits nonzero if anything fails.
 *
 *  TARGET API (doesn't exist yet — that's the TDD red phase):
 *      Optional<Expr> ExpressionParser.parseExpression()
 *      Parses ONE expression from the current token position.
 *      empty()  = syntax error. Trailing tokens belong to the caller
 *      (the statement parser consumes ';' etc).
 *
 *  ── LANGUAGE DESIGN DECISIONS (from the design Q&A) ──────────────────
 *   1. && / ||    Standard. Comparisons bind tighter than &&, which binds
 *                 tighter than ||. Relational (< <= > >=) binds tighter
 *                 than equality (== !=):
 *                       a < b == c   ->   (a < b) == c
 *                       a == b && c  ->   (a == b) && c
 *   2. bet syntax `x bet lo, hi`  (comma-separated bounds — NOT lo..hi).
 *                 BetExpr(pivot, lower, upper) = BetExpr(x, lo, hi).
 *   3. a += 1     NEW node:  CompoundAssignExpr(VariableExpr target,
 *                 Token operator, Expr value)  — keeps the compound op.
 *   4. x++ / ++x  UnaryExpr gains  boolean isPostfix  (3rd component).
 *                 x++ -> isPostfix=true ; ++x -> isPostfix=false.
 *   5. ! and truthiness
 *                 The parser ONLY builds UnaryExpr(NOT, expr). Deciding
 *                 that !5 is false (5 is truthy) is SEMANTIC — the job of
 *                 a later phase (type-check / evaluation), not the parser.
 *
 *  ── PRECEDENCE LADDER (loosest → tightest) ───────────────────────────
 *    =   +=  -=  *=  /=  ^=  %=   assignment — RIGHT-assoc
 *    ?:                            ternary — RIGHT-assoc
 *    ||                            or
 *    &&                            and
 *    bet (x bet lo, hi)            ternary infix, comma bounds
 *    ==  !=                        equality
 *    <  <=  >  >=                  relational
 *    ..                            range (BinaryExpr RANGE) — left
 *    +  -                          additive — left
 *    *  /  %  //                   multiplicative — left
 *    ^                             exponent — RIGHT-assoc
 *    as                            cast (tight)
 *    !  -  +  ++  --               prefix unary — ! tightest prefix
 *    ()  []  .  ++  --  f()        postfix — left, tightest
 *    literals  identifiers  ( )
 *
 *  ambiguous corners (chosen here, flippable if you disagree):
 *    - prefix unary binds TIGHTER than ^ :  -2 ^ 2  ->  (-2)^2  (= 4).
 *      consistent with "! highest". Flip to math -(2^2) if preferred.
 *    - as binds tighter than mult:  x as num * 2  ->  (x as num) * 2
 *    - .. sits between additive and relational:  1..5 + 1  ->  1..(5+1)
 *    - bet binds looser than ==:  x bet 1, 10 == y  ->  (x bet 1,10) == y
 *
 *  ── NODE / TOKEN GAPS this spec exposes ───────────────────────────────
 *    [ ] CompoundAssignExpr  — new node (shape above); compound tests
 *        below reference it and won't compile until it exists.
 *    [ ] UnaryExpr.isPostfix — add the boolean; inc/dec tests call it.
 *    [ ] TYPEOF token        — missing from TokenType/Lexer. `typeof`
 *        currently lexes as IDENTIFIER, so the typeof tests are RED until
 *        the token is added (marked inline). TypeOfExpr node already exists.
 *    [ ] AssignExpr.targets is a List — multi-assign meaning TBD;
 *        these tests use a single target only.
 *
 *  ── DEFERRED (comment-only, NOT runnable here) ────────────────────────
 *    new ...                 syntax undefined (array/tuple/map/set)
 *    match x { ... }         needs MatchCase + BlockExpr parsing
 *    func (...) { ... }      FuncLiteralExpr needs BlockStmt (statement parser)
 *    x in arr                for-loop construct, statement level
 *    num[5] (ArraySizedExpr) syntax undefined
 *
 *  ── COMPILE ORDER (TDD red phase) ─────────────────────────────────────
 *    Add the 3 items above (parseExpression, CompoundAssignExpr,
 *    isPostfix) -> this file compiles -> implement the parser -> all PASS.
 *
 *    Run:
 *      ./gradlew compileJava
 *      javac -cp build/classes/java/main -d build/classes/java/test \
 *        src/test/java/com/doruk/parser/ExpressionParserTest.java
 *      java -cp build/classes/java/main:build/classes/java/test \
 *        com.doruk.parser.ExpressionParserTest
 * =====================================================================
 */
public class ExpressionParserTest {

    static int passed = 0;
    static int failed = 0;
    static String current = "";

    public static void main(String[] args) {
        literals();
        variables();
        grouping();
        prefixUnary();
        incDec();
        multiplicative();
        additive();
        precedence();
        range();
        relationalEquality();
        logical();
        bet();
        ternary();
        assignment();
        calls();
        index();
        memberAccess();
        cast();
        typeOf();          // BLOCKED: no TYPEOF token yet — see header
        arrayLiteral();
        errors();

        System.out.println();
        System.out.println("PASSED: " + passed + "   FAILED: " + failed);
        System.exit(failed == 0 ? 0 : 1);
    }

    // ══════════════════════ harness ══════════════════════

    /** Lex + parse one expression; fail the case if the parser errored. */
    static Expr parse(String src) {
        var parser = new ExpressionParser(new ParserState(new Lexer("<expr>", src).tokens()));
        var result = parser.parseExpression();
        if (result.isEmpty())
            throw new AssertionError("parse returned empty (error) for: " + src);
        return result.get();
    }

    static Optional<Expr> tryParse(String src) {
        return new ExpressionParser(new ParserState(new Lexer("<expr>", src).tokens())).parseExpression();
    }

    /** Run one case. The body throws AssertionError on mismatch. */
    static void run(String src, Runnable body) {
        current = src;
        try {
            body.run();
            passed++;
            System.out.println("PASS: " + src);
        } catch (AssertionError e) {
            failed++;
            System.out.println("FAIL: " + src);
            System.out.println("      " + e.getMessage().replace("\n", "\n      "));
        }
    }

    /** Run one case that must fail to parse. */
    static void expectError(String src) {
        current = src;
        var result = tryParse(src);
        if (result.isPresent()) {
            failed++;
            System.out.println("FAIL: " + src + "   (expected parse error, got " + dump(result.get()) + ")");
        } else {
            passed++;
            System.out.println("PASS: " + src + "   (parse error as expected)");
        }
    }

    // ══════════════════════ assert helpers ══════════════════════

    static <T extends Expr> T as(Class<T> type, Expr e) {
        if (!type.isInstance(e))
            throw new AssertionError("expected " + type.getSimpleName() + ", got " + dump(e) + "   [" + current + "]");
        return type.cast(e);
    }

    static void op(Token t, TokenType type, String lexeme) {
        if (t.type() != type || !t.lexeme().equals(lexeme))
            throw new AssertionError("expected op " + type + "/'" + lexeme + "', got '" + t.lexeme() + "'   [" + current + "]");
    }

    static LiteralExpr lit(Expr e) { return as(LiteralExpr.class, e); }

    /** Compare a literal's value (BigDecimals by numeric value, else equals). */
    static void litVal(Expr e, Object value) {
        var actual = lit(e).token().literal();
        if (value instanceof BigDecimal want) {
            if (!(actual instanceof BigDecimal got) || got.compareTo(want) != 0)
                throw new AssertionError("expected literal " + want + ", got " + actual + "   [" + current + "]");
            return;
        }
        if (!java.util.Objects.equals(actual, value))
            throw new AssertionError("expected literal " + value + ", got " + actual + "   [" + current + "]");
    }

    static VariableExpr var(Expr e, String name) {
        var v = as(VariableExpr.class, e);
        if (!v.name().lexeme().equals(name))
            throw new AssertionError("expected var '" + name + "', got '" + v.name().lexeme() + "'   [" + current + "]");
        return v;
    }

    static BinaryExpr bin(Expr e, TokenType opType, String opLexeme) {
        var b = as(BinaryExpr.class, e);
        op(b.operator(), opType, opLexeme);
        return b;
    }

    static UnaryExpr unary(Expr e, TokenType opType, String opLexeme) {
        var u = as(UnaryExpr.class, e);
        op(u.operator(), opType, opLexeme);
        return u;
    }

    /** Compact AST printer — used in expected-vs-got messages. */
    static String dump(Expr e) {
        if (e == null) return "null";
        if (e instanceof LiteralExpr l) {
            var v = l.token().literal();
            return "Literal(" + (v == null ? "null" : v) + ")";
        }
        if (e instanceof VariableExpr v) return "Var(" + v.name().lexeme() + ")";
        if (e instanceof BinaryExpr b) return "Binary(" + b.operator().lexeme() + ", " + dump(b.left()) + ", " + dump(b.right()) + ")";
        if (e instanceof UnaryExpr u) return "Unary(" + u.operator().lexeme() + ", " + dump(u.expr()) + (u.isPostfix() ? ", post" : ", pre") + ")";
        if (e instanceof TernaryExpr t) return "Ternary(" + dump(t.condition()) + ", " + dump(t.trueValue()) + ", " + dump(t.falseValue()) + ")";
        if (e instanceof AssignExpr a) {
            var names = a.targets().stream().map(v -> v.name().lexeme()).toList();
            return "Assign(" + names + ", " + dump(a.value()) + ")";
        }
        if (e instanceof CompoundAssignExpr c) return "CompoundAssign(" + c.target().name().lexeme() + ", " + c.operator().lexeme() + ", " + dump(c.value()) + ")";
        if (e instanceof CallExpr c) return "Call(" + dump(c.callee()) + ", " + c.arguments().size() + " args)";
        if (e instanceof IndexExpr i) return "Index(" + dump(i.object()) + ", " + dump(i.index()) + ")";
        if (e instanceof MemberAccessExpr m) return "Member(" + dump(m.object()) + ", " + dump(m.member()) + ")";
        if (e instanceof CastExpr c) {
            var t = c.targetType();
            String tn = t instanceof PrimitiveType p ? p.type().lexeme() : t.getClass().getSimpleName();
            return "Cast(" + dump(c.value()) + " as " + tn + ")";
        }
        if (e instanceof TypeOfExpr t) return "TypeOf(" + dump(t.expression()) + ")";
        if (e instanceof BetExpr b) return "Bet(" + dump(b.pivot()) + ", " + dump(b.lower()) + ", " + dump(b.upper()) + ")";
        if (e instanceof ArrayLiteralExpr a) return "Array(" + a.elements().size() + " elems)";
        if (e instanceof ArraySizedExpr a) return "ArraySized(" + dump(a.size()) + ")";
        if (e instanceof NewExpr n) return "New(" + dump(n.expression()) + ")";
        if (e instanceof FuncLiteralExpr f) return "FuncLiteral(...)";
        if (e instanceof MatchExpr m) return "Match(...)";
        return e.getClass().getSimpleName();
    }

    // ══════════════════════ 1. literals ══════════════════════

    static void literals() {
        run("42", () -> litVal(parse("42"), new BigDecimal("42")));
        run("3.14", () -> litVal(parse("3.14"), new BigDecimal("3.14")));
        run("0", () -> litVal(parse("0"), BigDecimal.ZERO));
        run("10000000000", () -> litVal(parse("10000000000"), new BigDecimal("10000000000")));
        run("\"hi\"", () -> litVal(parse("\"hi\""), "hi"));
        run("\"a\\nb\"", () -> litVal(parse("\"a\\nb\""), "a\nb"));
        run("true", () -> litVal(parse("true"), Boolean.TRUE));
        run("false", () -> litVal(parse("false"), Boolean.FALSE));
        run("null", () -> {
            var l = lit(parse("null"));
            op(l.token(), TokenType.NULL, "null");
            if (l.token().literal() != null)
                throw new AssertionError("null literal should carry null value   [" + current + "]");
        });
    }

    // ══════════════════════ 2. variables ══════════════════════

    static void variables() {
        run("x", () -> var(parse("x"), "x"));
        run("_foo", () -> var(parse("_foo"), "_foo"));
        run("foo_bar", () -> var(parse("foo_bar"), "foo_bar"));
        run("x1", () -> var(parse("x1"), "x1"));
    }

    // ══════════════════════ 3. grouping (parens) ══════════════════════

    static void grouping() {
        run("(1 + 2) * 3", () -> {
            var b = bin(parse("(1 + 2) * 3"), TokenType.STAR, "*");
            var inner = bin(b.left(), TokenType.PLUS, "+");
            litVal(inner.left(), BigDecimal.ONE);
            litVal(inner.right(), new BigDecimal("2"));
            litVal(b.right(), new BigDecimal("3"));
        });
        run("((1))", () -> litVal(parse("((1))"), BigDecimal.ONE));
        run("!(a && b)", () -> {
            var u = unary(parse("!(a && b)"), TokenType.NOT, "!");
            var inner = bin(u.expr(), TokenType.AND, "&&");
            var(inner.left(), "a");
            var(inner.right(), "b");
        });
        expectError("()");
        expectError("(1");
        expectError("((1)");
    }

    // ══════════════════════ 4. prefix unary  !  -  + ══════════════════════

    static void prefixUnary() {
        // ! is a prefix op here — no postfix ! exists. isPostfix must be false.
        run("!true", () -> {
            var u = unary(parse("!true"), TokenType.NOT, "!");
            litVal(u.expr(), Boolean.TRUE);
            if (u.isPostfix()) throw new AssertionError("! is prefix, expected isPostfix=false   [" + current + "]");
        });
        run("!false", () -> {
            var u = unary(parse("!false"), TokenType.NOT, "!");
            litVal(u.expr(), Boolean.FALSE);
            if (u.isPostfix()) throw new AssertionError("! is prefix, expected isPostfix=false   [" + current + "]");
        });
        run("!x", () -> {
            var u = unary(parse("!x"), TokenType.NOT, "!");
            var(u.expr(), "x");
        });
        run("!!true", () -> {
            var u1 = unary(parse("!!true"), TokenType.NOT, "!");
            var u2 = unary(u1.expr(), TokenType.NOT, "!");
            litVal(u2.expr(), Boolean.TRUE);
        });
        // truthiness is semantics, not parsing: !5 builds Unary(NOT, 5).
        // That it evaluates to false (5 is truthy) is for a later phase.
        run("!5", () -> {
            var u = unary(parse("!5"), TokenType.NOT, "!");
            litVal(u.expr(), new BigDecimal("5"));
        });
        run("-x", () -> {
            var u = unary(parse("-x"), TokenType.MINUS, "-");
            var(u.expr(), "x");
        });
        run("+x", () -> {
            var u = unary(parse("+x"), TokenType.PLUS, "+");
            var(u.expr(), "x");
        });
        // two unary minuses need whitespace: - -x. Bare --x is prefix dec.
        run("- -x", () -> {
            var u = unary(parse("- -x"), TokenType.MINUS, "-");
            var inner = unary(u.expr(), TokenType.MINUS, "-");
            var(inner.expr(), "x");
        });
        expectError("!");
        expectError("-");
    }

    // ══════════════════════ 5. inc/dec  ++  --  (prefix & postfix) ══════════════════════

    static void incDec() {
        // x++ is postfix -> isPostfix=true. ++x is prefix -> false.
        // NOTE: UnaryExpr needs the isPostfix component (see header) to tell them apart.
        run("x++", () -> {
            var u = unary(parse("x++"), TokenType.PLUS_PLUS, "++");
            var(u.expr(), "x");
            if (!u.isPostfix()) throw new AssertionError("expected POSTFIX x++, got prefix   [" + current + "]");
        });
        run("++x", () -> {
            var u = unary(parse("++x"), TokenType.PLUS_PLUS, "++");
            var(u.expr(), "x");
            if (u.isPostfix()) throw new AssertionError("expected PREFIX ++x, got postfix   [" + current + "]");
        });
        run("x--", () -> {
            var u = unary(parse("x--"), TokenType.MINUS_MINUS, "--");
            var(u.expr(), "x");
            if (!u.isPostfix()) throw new AssertionError("expected POSTFIX x--, got prefix   [" + current + "]");
        });
        run("--x", () -> {
            var u = unary(parse("--x"), TokenType.MINUS_MINUS, "--");
            var(u.expr(), "x");
            if (u.isPostfix()) throw new AssertionError("expected PREFIX --x, got postfix   [" + current + "]");
        });
        // postfix applies to a whole index expression, not just the base var
        run("a[i]++", () -> {
            var u = unary(parse("a[i]++"), TokenType.PLUS_PLUS, "++");
            var ix = as(IndexExpr.class, u.expr());
            var(ix.object(), "a");
            litVal(ix.index(), BigDecimal.ZERO);
            if (!u.isPostfix()) throw new AssertionError("expected postfix   [" + current + "]");
        });
        // whitespace matters: "a - -b" is a minus applied to (-b)
        run("a - -b", () -> {
            var b = bin(parse("a - -b"), TokenType.MINUS, "-");
            var(b.left(), "a");
            var inner = unary(b.right(), TokenType.MINUS, "-");
            var(inner.expr(), "b");
        });
        // footgun (comment-only, not asserted): "a--b" lexes as `a -- b`
        // (MINUS_MINUS), so it becomes postfix `a--` with a stray `b`.
        // The statement parser will then trip on `b` where `;` is expected.
    }

    // ══════════════════════ 6. multiplicative  *  /  %  // ══════════════════════

    static void multiplicative() {
        run("2 * 3", () -> {
            var b = bin(parse("2 * 3"), TokenType.STAR, "*");
            litVal(b.left(), new BigDecimal("2"));
            litVal(b.right(), new BigDecimal("3"));
        });
        run("10 / 4", () -> {
            var b = bin(parse("10 / 4"), TokenType.SLASH, "/");
            litVal(b.left(), new BigDecimal("10"));
            litVal(b.right(), new BigDecimal("4"));
        });
        run("7 % 3", () -> bin(parse("7 % 3"), TokenType.MODULO, "%"));
        run("9 // 2", () -> bin(parse("9 // 2"), TokenType.SLASH_SLASH, "//")); // integer division
        // left-assoc: 8 / 2 / 2  ->  (8/2)/2
        run("8 / 2 / 2", () -> {
            var outer = bin(parse("8 / 2 / 2"), TokenType.SLASH, "/");
            var inner = bin(outer.left(), TokenType.SLASH, "/");
            litVal(inner.left(), new BigDecimal("8"));
            litVal(inner.right(), new BigDecimal("2"));
            litVal(outer.right(), new BigDecimal("2"));
        });
        run("2 * 3 * 4", () -> {
            var outer = bin(parse("2 * 3 * 4"), TokenType.STAR, "*");
            var inner = bin(outer.left(), TokenType.STAR, "*");
            litVal(inner.left(), new BigDecimal("2"));
            litVal(inner.right(), new BigDecimal("3"));
            litVal(outer.right(), new BigDecimal("4"));
        });
    }

    // ══════════════════════ 7. additive  +  - ══════════════════════

    static void additive() {
        run("1 + 2", () -> {
            var b = bin(parse("1 + 2"), TokenType.PLUS, "+");
            litVal(b.left(), BigDecimal.ONE);
            litVal(b.right(), new BigDecimal("2"));
        });
        run("10 - 4", () -> {
            var b = bin(parse("10 - 4"), TokenType.MINUS, "-");
            litVal(b.left(), new BigDecimal("10"));
            litVal(b.right(), new BigDecimal("4"));
        });
        // left-assoc: 10 - 4 - 3  ->  (10-4)-3
        run("10 - 4 - 3", () -> {
            var outer = bin(parse("10 - 4 - 3"), TokenType.MINUS, "-");
            var inner = bin(outer.left(), TokenType.MINUS, "-");
            litVal(inner.left(), new BigDecimal("10"));
            litVal(inner.right(), new BigDecimal("4"));
            litVal(outer.right(), new BigDecimal("3"));
        });
        run("1 + 2 + 3", () -> {
            var outer = bin(parse("1 + 2 + 3"), TokenType.PLUS, "+");
            var inner = bin(outer.left(), TokenType.PLUS, "+");
            litVal(inner.left(), BigDecimal.ONE);
            litVal(inner.right(), new BigDecimal("2"));
            litVal(outer.right(), new BigDecimal("3"));
        });
    }

    // ══════════════════════ 8. precedence matrix ══════════════════════

    static void precedence() {
        // * binds tighter than +
        run("1 + 2 * 3", () -> {
            var outer = bin(parse("1 + 2 * 3"), TokenType.PLUS, "+");
            litVal(outer.left(), BigDecimal.ONE);
            var m = bin(outer.right(), TokenType.STAR, "*");
            litVal(m.left(), new BigDecimal("2"));
            litVal(m.right(), new BigDecimal("3"));
        });
        run("1 * 2 + 3", () -> {
            var outer = bin(parse("1 * 2 + 3"), TokenType.PLUS, "+");
            var m = bin(outer.left(), TokenType.STAR, "*");
            litVal(m.left(), BigDecimal.ONE);
            litVal(m.right(), new BigDecimal("2"));
            litVal(outer.right(), new BigDecimal("3"));
        });
        run("1 + 2 + 3 * 4", () -> { // (1+2) + (3*4)
            var outer = bin(parse("1 + 2 + 3 * 4"), TokenType.PLUS, "+");
            var l = bin(outer.left(), TokenType.PLUS, "+");
            var r = bin(outer.right(), TokenType.STAR, "*");
            litVal(l.left(), BigDecimal.ONE);
            litVal(l.right(), new BigDecimal("2"));
            litVal(r.left(), new BigDecimal("3"));
            litVal(r.right(), new BigDecimal("4"));
        });
        // ^ is RIGHT-assoc
        run("2 ^ 3 ^ 2", () -> {
            var outer = bin(parse("2 ^ 3 ^ 2"), TokenType.CARET, "^");
            litVal(outer.left(), new BigDecimal("2"));
            var r = bin(outer.right(), TokenType.CARET, "^");
            litVal(r.left(), new BigDecimal("3"));
            litVal(r.right(), new BigDecimal("2"));
        });
        // ^ binds tighter than * and +
        run("1 + 2 ^ 3 * 4", () -> { // 1 + ((2^3) * 4)
            var outer = bin(parse("1 + 2 ^ 3 * 4"), TokenType.PLUS, "+");
            litVal(outer.left(), BigDecimal.ONE);
            var m = bin(outer.right(), TokenType.STAR, "*");
            var p = bin(m.left(), TokenType.CARET, "^");
            litVal(p.left(), new BigDecimal("2"));
            litVal(p.right(), new BigDecimal("3"));
            litVal(m.right(), new BigDecimal("4"));
        });
        // prefix unary binds TIGHTER than ^ (design choice): -2 ^ 2 -> (-2)^2
        run("-2 ^ 2", () -> {
            var p = bin(parse("-2 ^ 2"), TokenType.CARET, "^");
            var u = unary(p.left(), TokenType.MINUS, "-");
            litVal(u.expr(), new BigDecimal("2"));
            litVal(p.right(), new BigDecimal("2"));
        });
        // comparison binds LOOSER than additive
        run("5 >= 3 + 2", () -> {
            var b = bin(parse("5 >= 3 + 2"), TokenType.GREATER_EQUAL, ">=");
            litVal(b.left(), new BigDecimal("5"));
            var add = bin(b.right(), TokenType.PLUS, "+");
            litVal(add.left(), new BigDecimal("3"));
            litVal(add.right(), new BigDecimal("2"));
        });
    }

    // ══════════════════════ 9. range  .. ══════════════════════

    static void range() {
        run("1..5", () -> {
            var b = bin(parse("1..5"), TokenType.RANGE, "..");
            litVal(b.left(), BigDecimal.ONE);
            litVal(b.right(), new BigDecimal("5"));
        });
        run("a..b", () -> {
            var b = bin(parse("a..b"), TokenType.RANGE, "..");
            var(b.left(), "a");
            var(b.right(), "b");
        });
        // .. binds LOOSER than + : 1..5 + 1  ->  1..(5+1)
        run("1..5 + 1", () -> {
            var b = bin(parse("1..5 + 1"), TokenType.RANGE, "..");
            litVal(b.left(), BigDecimal.ONE);
            var add = bin(b.right(), TokenType.PLUS, "+");
            litVal(add.left(), new BigDecimal("5"));
            litVal(add.right(), BigDecimal.ONE);
        });
    }

    // ══════════════════════ 10. relational & equality ══════════════════════

    static void relationalEquality() {
        run("a < b", () -> {
            var b = bin(parse("a < b"), TokenType.LESS, "<");
            var(b.left(), "a");
            var(b.right(), "b");
        });
        run("a <= b", () -> {
            var b = bin(parse("a <= b"), TokenType.LESS_EQUAL, "<=");
            var(b.left(), "a");
            var(b.right(), "b");
        });
        run("a > b", () -> {
            var b = bin(parse("a > b"), TokenType.GREATER, ">");
            var(b.left(), "a");
            var(b.right(), "b");
        });
        run("a >= b", () -> {
            var b = bin(parse("a >= b"), TokenType.GREATER_EQUAL, ">=");
            var(b.left(), "a");
            var(b.right(), "b");
        });
        run("a == b", () -> {
            var b = bin(parse("a == b"), TokenType.EQUAL_EQUAL, "==");
            var(b.left(), "a");
            var(b.right(), "b");
        });
        run("a != b", () -> {
            var b = bin(parse("a != b"), TokenType.NOT_EQUAL, "!=");
            var(b.left(), "a");
            var(b.right(), "b");
        });
        // relational binds tighter than equality: a < b == c -> (a<b)==c
        run("a < b == c", () -> {
            var eq = bin(parse("a < b == c"), TokenType.EQUAL_EQUAL, "==");
            var lt = bin(eq.left(), TokenType.LESS, "<");
            var(lt.left(), "a");
            var(lt.right(), "b");
            var(eq.right(), "c");
        });
        // comparisons are left-assoc: a < b < c -> (a<b)<c
        run("a < b < c", () -> {
            var outer = bin(parse("a < b < c"), TokenType.LESS, "<");
            var inner = bin(outer.left(), TokenType.LESS, "<");
            var(inner.left(), "a");
            var(inner.right(), "b");
            var(outer.right(), "c");
        });
    }

    // ══════════════════════ 11. logical  &&  || ══════════════════════

    static void logical() {
        run("a && b", () -> {
            var b = bin(parse("a && b"), TokenType.AND, "&&");
            var(b.left(), "a");
            var(b.right(), "b");
        });
        run("a || b", () -> {
            var b = bin(parse("a || b"), TokenType.OR, "||");
            var(b.left(), "a");
            var(b.right(), "b");
        });
        // && binds tighter than ||: a && b || c -> (a&&b)||c
        run("a && b || c", () -> {
            var or = bin(parse("a && b || c"), TokenType.OR, "||");
            var and = bin(or.left(), TokenType.AND, "&&");
            var(and.left(), "a");
            var(and.right(), "b");
            var(or.right(), "c");
        });
        run("a || b && c", () -> { // a || (b&&c)
            var or = bin(parse("a || b && c"), TokenType.OR, "||");
            var(or.left(), "a");
            var and = bin(or.right(), TokenType.AND, "&&");
            var(and.left(), "b");
            var(and.right(), "c");
        });
        // comparisons bind tighter than &&: a == b && c < d -> (a==b)&&(c<d)
        run("a == b && c < d", () -> {
            var and = bin(parse("a == b && c < d"), TokenType.AND, "&&");
            var eq = bin(and.left(), TokenType.EQUAL_EQUAL, "==");
            var(eq.left(), "a");
            var(eq.right(), "b");
            var lt = bin(and.right(), TokenType.LESS, "<");
            var(lt.left(), "c");
            var(lt.right(), "d");
        });
        // ! binds tighter than &&: !a && b -> (!a)&&b
        run("!a && b", () -> {
            var and = bin(parse("!a && b"), TokenType.AND, "&&");
            var u = unary(and.left(), TokenType.NOT, "!");
            var(u.expr(), "a");
            var(and.right(), "b");
        });
    }

    // ══════════════════════ 12. bet  (x bet lo, hi) ══════════════════════

    static void bet() {
        // comma-separated bounds (design decision, see header)
        run("x bet 1, 10", () -> {
            var b = as(BetExpr.class, parse("x bet 1, 10"));
            op(b.keyword(), TokenType.BET, "bet");
            var(b.pivot(), "x");
            litVal(b.lower(), BigDecimal.ONE);
            litVal(b.upper(), new BigDecimal("10"));
        });
        run("x bet a, b + 1", () -> {
            var b = as(BetExpr.class, parse("x bet a, b + 1"));
            var(b.pivot(), "x");
            var(b.lower(), "a");
            var add = bin(b.upper(), TokenType.PLUS, "+");
            var(add.left(), "b");
            litVal(add.right(), BigDecimal.ONE);
        });
        // bet binds LOOSER than ==: x bet 1, 10 == y -> (x bet 1,10) == y
        run("x bet 1, 10 == y", () -> {
            var eq = bin(parse("x bet 1, 10 == y"), TokenType.EQUAL_EQUAL, "==");
            var b = as(BetExpr.class, eq.left());
            var(b.pivot(), "x");
            litVal(b.lower(), BigDecimal.ONE);
            litVal(b.upper(), new BigDecimal("10"));
            var(eq.right(), "y");
        });
    }

    // ══════════════════════ 13. ternary  ?: ══════════════════════

    static void ternary() {
        run("a ? b : c", () -> {
            var t = as(TernaryExpr.class, parse("a ? b : c"));
            var(t.condition(), "a");
            var(t.trueValue(), "b");
            var(t.falseValue(), "c");
        });
        // RIGHT-assoc: a ? b : c ? d : e  ->  a ? b : (c ? d : e)
        run("a ? b : c ? d : e", () -> {
            var t = as(TernaryExpr.class, parse("a ? b : c ? d : e"));
            var(t.condition(), "a");
            var(t.trueValue(), "b");
            var inner = as(TernaryExpr.class, t.falseValue());
            var(inner.condition(), "c");
            var(inner.trueValue(), "d");
            var(inner.falseValue(), "e");
        });
        // || binds tighter than ?: : a || b ? c : d -> (a||b)?c:d
        run("a || b ? c : d", () -> {
            var t = as(TernaryExpr.class, parse("a || b ? c : d"));
            var or = bin(t.condition(), TokenType.OR, "||");
            var(or.left(), "a");
            var(or.right(), "b");
            var(t.trueValue(), "c");
            var(t.falseValue(), "d");
        });
        // assignment binds LOOSER than ?: : a = b ? c : d
        run("a = b ? c : d", () -> {
            var a = as(AssignExpr.class, parse("a = b ? c : d"));
            var(a.targets().get(0), "a");
            var t = as(TernaryExpr.class, a.value());
            var(t.condition(), "b");
            var(t.trueValue(), "c");
            var(t.falseValue(), "d");
        });
        expectError("a ? b");        // missing the ':' branch
        expectError("a ? : c");      // missing the true branch
    }

    // ══════════════════════ 14. assignment  =  +=  -=  *=  /=  ^=  %= ══════════════════════

    static void assignment() {
        run("a = 5", () -> {
            var a = as(AssignExpr.class, parse("a = 5"));
            var(a.targets().get(0), "a");
            litVal(a.value(), new BigDecimal("5"));
        });
        // RIGHT-assoc: a = b = 5  ->  a = (b = 5)
        run("a = b = 5", () -> {
            var outer = as(AssignExpr.class, parse("a = b = 5"));
            var(outer.targets().get(0), "a");
            var inner = as(AssignExpr.class, outer.value());
            var(inner.targets().get(0), "b");
            litVal(inner.value(), new BigDecimal("5"));
        });
        // assignment is the loosest: a = 5 >= b  ->  a = (5 >= b)
        // (mirrors the statement-level `var a = 5 >= b;` -> a is boolean)
        run("a = 5 >= b", () -> {
            var a = as(AssignExpr.class, parse("a = 5 >= b"));
            var(a.targets().get(0), "a");
            var cmp = bin(a.value(), TokenType.GREATER_EQUAL, ">=");
            litVal(cmp.left(), new BigDecimal("5"));
            var(cmp.right(), "b");
        });
        run("a = -b", () -> {
            var a = as(AssignExpr.class, parse("a = -b"));
            var u = unary(a.value(), TokenType.MINUS, "-");
            var(u.expr(), "b");
        });
        run("a = !b", () -> {
            var a = as(AssignExpr.class, parse("a = !b"));
            var u = unary(a.value(), TokenType.NOT, "!");
            var(u.expr(), "b");
        });

        // compound assignments — need CompoundAssignExpr node (see header)
        run("a += 1", () -> {
            var c = as(CompoundAssignExpr.class, parse("a += 1"));
            var(c.target(), "a");
            op(c.operator(), TokenType.PLUS_EQUAL, "+=");
            litVal(c.value(), BigDecimal.ONE);
        });
        run("a -= b", () -> {
            var c = as(CompoundAssignExpr.class, parse("a -= b"));
            var(c.target(), "a");
            op(c.operator(), TokenType.MINUS_EQUAL, "-=");
            var(c.value(), "b");
        });
        run("a *= 2", () -> {
            var c = as(CompoundAssignExpr.class, parse("a *= 2"));
            var(c.target(), "a");
            op(c.operator(), TokenType.STAR_EQUAL, "*=");
            litVal(c.value(), new BigDecimal("2"));
        });
        run("a /= 2", () -> {
            var c = as(CompoundAssignExpr.class, parse("a /= 2"));
            var(c.target(), "a");
            op(c.operator(), TokenType.SLASH_EQUAL, "/=");
            litVal(c.value(), new BigDecimal("2"));
        });
        run("a ^= 2", () -> {
            var c = as(CompoundAssignExpr.class, parse("a ^= 2"));
            var(c.target(), "a");
            op(c.operator(), TokenType.CARET_EQUAL, "^=");
            litVal(c.value(), new BigDecimal("2"));
        });
        run("a %= 2", () -> {
            var c = as(CompoundAssignExpr.class, parse("a %= 2"));
            var(c.target(), "a");
            op(c.operator(), TokenType.MODULO_EQUAL, "%=");
            litVal(c.value(), new BigDecimal("2"));
        });
        expectError("a = ");         // assignment with no value
    }

    // ══════════════════════ 15. calls  f(...) ══════════════════════

    static void calls() {
        run("f()", () -> {
            var c = as(CallExpr.class, parse("f()"));
            var(c.callee(), "f");
            if (!c.arguments().isEmpty())
                throw new AssertionError("expected 0 args, got " + c.arguments().size() + "   [" + current + "]");
        });
        run("f(1)", () -> {
            var c = as(CallExpr.class, parse("f(1)"));
            var(c.callee(), "f");
            if (c.arguments().size() != 1)
                throw new AssertionError("expected 1 arg, got " + c.arguments().size() + "   [" + current + "]");
            litVal(c.arguments().get(0), BigDecimal.ONE);
        });
        run("f(a, b + 1)", () -> {
            var c = as(CallExpr.class, parse("f(a, b + 1)"));
            var(c.callee(), "f");
            if (c.arguments().size() != 2)
                throw new AssertionError("expected 2 args, got " + c.arguments().size() + "   [" + current + "]");
            var(c.arguments().get(0), "a");
            var add = bin(c.arguments().get(1), TokenType.PLUS, "+");
            var(add.left(), "b");
            litVal(add.right(), BigDecimal.ONE);
        });
        // nested call as argument
        run("f(g(x))", () -> {
            var outer = as(CallExpr.class, parse("f(g(x))"));
            var(outer.callee(), "f");
            var inner = as(CallExpr.class, outer.arguments().get(0));
            var(inner.callee(), "g");
            var(inner.arguments().get(0), "x");
        });
        // method call: a.b(x) -> Call(Member(a, b), [x])
        run("a.b(x)", () -> {
            var c = as(CallExpr.class, parse("a.b(x)"));
            var m = as(MemberAccessExpr.class, c.callee());
            var(m.object(), "a");
            var(m.member(), "b");
            var(c.arguments().get(0), "x");
        });
        // chained call: f()() -> Call(Call(f), [])
        run("f()()", () -> {
            var outer = as(CallExpr.class, parse("f()()"));
            var inner = as(CallExpr.class, outer.callee());
            var(inner.callee(), "f");
        });
        expectError("f(,1)");
        expectError("f(1,)");
        expectError("f(1");
    }

    // ══════════════════════ 16. index  a[...] ══════════════════════

    static void index() {
        run("a[0]", () -> {
            var ix = as(IndexExpr.class, parse("a[0]"));
            var(ix.object(), "a");
            litVal(ix.index(), BigDecimal.ZERO);
        });
        run("a[i + 1]", () -> {
            var ix = as(IndexExpr.class, parse("a[i + 1]"));
            var(ix.object(), "a");
            var add = bin(ix.index(), TokenType.PLUS, "+");
            var(add.left(), "i");
            litVal(add.right(), BigDecimal.ONE);
        });
        run("a[0][1]", () -> {
            var outer = as(IndexExpr.class, parse("a[0][1]"));
            var inner = as(IndexExpr.class, outer.object());
            var(inner.object(), "a");
            litVal(inner.index(), BigDecimal.ZERO);
            litVal(outer.index(), BigDecimal.ONE);
        });
        // postfix chains bind left: a[b][c].d -> Member(Index(Index(a,b),c), d)
        run("a[b][c].d", () -> {
            var m = as(MemberAccessExpr.class, parse("a[b][c].d"));
            var(m.member(), "d");
            var outer = as(IndexExpr.class, m.object());
            var inner = as(IndexExpr.class, outer.object());
            var(inner.object(), "a");
            var(inner.index(), "b");
            var(outer.index(), "c");
        });
        expectError("a[");
        expectError("a[]");
    }

    // ══════════════════════ 17. member access  a.b ══════════════════════

    static void memberAccess() {
        run("a.b", () -> {
            var m = as(MemberAccessExpr.class, parse("a.b"));
            var(m.object(), "a");
            var(m.member(), "b");
        });
        // LEFT-assoc: a.b.c  ->  (a.b).c
        run("a.b.c", () -> {
            var outer = as(MemberAccessExpr.class, parse("a.b.c"));
            var(outer.member(), "c");
            var inner = as(MemberAccessExpr.class, outer.object());
            var(inner.object(), "a");
            var(inner.member(), "b");
        });
    }

    // ══════════════════════ 18. cast  as ══════════════════════

    static void cast() {
        run("x as num", () -> {
            var c = as(CastExpr.class, parse("x as num"));
            var(c.value(), "x");
            var t = (PrimitiveType) c.targetType(); // TypeNode, not Expr — direct cast
            op(t.type(), TokenType.NUM, "num");
        });
        // as binds tighter than mult: x as num * 2 -> (x as num) * 2
        run("x as num * 2", () -> {
            var m = bin(parse("x as num * 2"), TokenType.STAR, "*");
            var c = as(CastExpr.class, m.left());
            var(c.value(), "x");
            litVal(m.right(), new BigDecimal("2"));
        });
        run("(x as num) + 1", () -> {
            var add = bin(parse("(x as num) + 1"), TokenType.PLUS, "+");
            var c = as(CastExpr.class, add.left());
            var(c.value(), "x");
            litVal(add.right(), BigDecimal.ONE);
        });
        // prefix unary binds tighter than as: -x as num -> (-x) as num
        run("-x as num", () -> {
            var c = as(CastExpr.class, parse("-x as num"));
            var u = unary(c.value(), TokenType.MINUS, "-");
            var(u.expr(), "x");
        });
    }

    // ══════════════════════ 19. typeof  (BLOCKED) ══════════════════════

    static void typeOf() {
        // BLOCKED: TokenType/Lexer have no TYPEOF token — `typeof` lexes as
        // an IDENTIFIER, so these parse as a bare variable + stray token.
        // They stay RED until a TYPEOF token is added. TypeOfExpr node exists.
        run("typeof x", () -> {
            var t = as(TypeOfExpr.class, parse("typeof x"));
            var(t.expression(), "x");
        });
        run("typeof (a + b)", () -> {
            var t = as(TypeOfExpr.class, parse("typeof (a + b)"));
            var add = bin(t.expression(), TokenType.PLUS, "+");
            var(add.left(), "a");
            var(add.right(), "b");
        });
    }

    // ══════════════════════ 20. array literal  [a, b, c] ══════════════════════

    static void arrayLiteral() {
        run("[1, 2, 3]", () -> {
            var a = as(ArrayLiteralExpr.class, parse("[1, 2, 3]"));
            if (a.elements().size() != 3)
                throw new AssertionError("expected 3 elems, got " + a.elements().size() + "   [" + current + "]");
            litVal(a.elements().get(0), BigDecimal.ONE);
            litVal(a.elements().get(1), new BigDecimal("2"));
            litVal(a.elements().get(2), new BigDecimal("3"));
        });
        run("[]", () -> {
            var a = as(ArrayLiteralExpr.class, parse("[]"));
            if (!a.elements().isEmpty())
                throw new AssertionError("expected empty array   [" + current + "]");
        });
        run("[[1, 2], [3]]", () -> {
            var a = as(ArrayLiteralExpr.class, parse("[[1, 2], [3]]"));
            if (a.elements().size() != 2)
                throw new AssertionError("expected 2 elems   [" + current + "]");
            var inner1 = as(ArrayLiteralExpr.class, a.elements().get(0));
            var inner2 = as(ArrayLiteralExpr.class, a.elements().get(1));
            if (inner1.elements().size() != 2 || inner2.elements().size() != 1)
                throw new AssertionError("bad nested arrays   [" + current + "]");
        });
        run("[a, b + 1]", () -> {
            var a = as(ArrayLiteralExpr.class, parse("[a, b + 1]"));
            var(a.elements().get(0), "a");
            var add = bin(a.elements().get(1), TokenType.PLUS, "+");
            var(add.left(), "b");
            litVal(add.right(), BigDecimal.ONE);
        });
        // postfix on a literal: [1, 2][0] -> Index(Array([1,2]), 0)
        run("[1, 2][0]", () -> {
            var ix = as(IndexExpr.class, parse("[1, 2][0]"));
            var a = as(ArrayLiteralExpr.class, ix.object());
            if (a.elements().size() != 2)
                throw new AssertionError("expected 2 elems   [" + current + "]");
            litVal(ix.index(), BigDecimal.ZERO);
        });
        expectError("[1, 2");
    }

    // ══════════════════════ 21. error cases ══════════════════════

    static void errors() {
        expectError("");        // empty source (only EOF)
        expectError("1 +");     // trailing operator
        expectError("1 + * 2"); // missing operand
        expectError(")");       // operator-like token with no operand
        expectError("(1");      // unclosed paren
        expectError("a + b +"); // trailing operator
    }
}
