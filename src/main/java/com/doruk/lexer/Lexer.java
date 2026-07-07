package com.doruk.lexer;

import com.doruk.dto.Pos;
import com.doruk.dto.Token;
import com.doruk.lexer.exceptions.InvalidEscape;
import com.doruk.lexer.exceptions.InvalidTokenException;
import com.doruk.lexer.exceptions.UnterminatedString;

import java.math.BigDecimal;
import java.util.*;

public class Lexer {
    private final String fileName;
    private final String source;
    private final List<Token> tokens;
    private int line = 0;
    private int column = 0;
    private int cursor = 0;

    private final Map<String, TokenType> tokenMap;
    private final Set<Character> validNumTrailingTokens =
            new HashSet<>(Set.of(
                    ' ', '\t', '\n', ')', ']', '}', '+', '-', '*', '/', '%', '=', '!', '<', '>', '&', '|', '#', ',', ':', '^'
            ));

    public Lexer(String fileName, String source) {
        this.source = source;
        this.fileName = fileName;

        this.tokenMap = new HashMap<>();
        this.tokens = new ArrayList<>();
        // fill the token map
        Arrays.stream(TokenType.values())
                .forEach(tokenType -> tokenMap.put(tokenType.name().toLowerCase(), tokenType));

        // tokenize
        tokenize();
        // append eof token
        tokens.add(new Token(TokenType.EOF, "", null,
                new Pos(fileName, line + 1, column)));
    }

    private char view() {
        return source.charAt(cursor);
    }

    private char viewNext() {
        return source.charAt(cursor + 1);
    }

    private char consume() {
        var c = source.charAt(cursor++);
        column++;

        if (c == '\n') {
            line++;
            column = 0;
        }
        return c;
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private boolean isValidIdentifier(char c) {
        return isAlpha(c) || isDigit(c) || c == '_';
    }

    private boolean isDigit(char c) {
        return (c >= '0' && c <= '9');
    }

    private boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n';
    }

    private void skipComment() {
        while (cursor < source.length() && view() != '\n')
            consume();
    }

    private char getEscapedChar(char escaped) {
        return switch (escaped) {
            case 'n' -> '\n';
            case 't' -> '\t';
            case '"' -> '\"';
            case '\\' -> '\\';
            default -> '\0';
        };
    }

    private Pos calculatePosition(String lexeme) {
        return new Pos(fileName, line + 1, column - (lexeme.length() - 1));
    }

    private void addToken(TokenType type, String lexeme, Object literal) {
        tokens.add(
                new Token(type, lexeme, literal, calculatePosition(lexeme))
        );
    }

    private String scanIdentifier(char c) {
        var id = new StringBuilder();
        id.append(c);

        while (cursor < source.length()) {
            char i = view();
            if (isWhitespace(c) || !isValidIdentifier(i))
                break;

            id.append(consume());
        }
        return id.toString();
    }

    private void scanNumber(char c) {
        var builder = new StringBuilder();
        builder.append(c);

        while (cursor < source.length()) {
            char n = view();
            if (validNumTrailingTokens.contains(n) || (n == '.' && viewNext() == '.')) // if c = ., and next = ., then its range
                break;

            builder.append(consume());

            if (!(isDigit(n) || n == '.'))
                throw new InvalidTokenException(builder.toString(), calculatePosition(builder.toString()));
        }

        var lexeme = builder.toString();
        try {
            var num = new BigDecimal(lexeme);
            addToken(TokenType.NUMBER_LITERAL, lexeme, num);
        } catch (NumberFormatException e) {
            throw new InvalidTokenException(builder.toString(), calculatePosition(builder.toString()));
        }
    }

    private void scanString(char c) {
        var lexeme = new StringBuilder();
        var literal = new StringBuilder();
        lexeme.append(c);

        var last = c;
        while (cursor < source.length()) {
            // check escape
            if (view() == '\\') {
                // check if eof
                if ((cursor + 1) >= source.length())
                    throw new InvalidEscape('\0', calculatePosition("\0"));

                var e = getEscapedChar(viewNext());
                if (e == '\0')
                    throw new InvalidEscape(viewNext(), calculatePosition(String.valueOf(viewNext())));

                lexeme.append(consume());
                lexeme.append((last = consume()));
                literal.append(e);
                continue;
            }

            last = consume();
            lexeme.append(last);

            if (last == '"')
                break;
            literal.append(last);
        }

        if (last != '"')
            throw new UnterminatedString(calculatePosition(lexeme.toString()), lexeme.toString());

        addToken(TokenType.STRING_LITERAL, lexeme.toString(), literal.toString());
    }

    private void scanOperator(char c) {
        // since c is already consumed, view should point to next char
        var next = cursor >= source.length() ? '\0' : view();
        switch (c) {
            case '+' -> {
                if (next == '+')
                    addToken(TokenType.PLUS_PLUS, "++", null);
                else if (next == '=')
                    addToken(TokenType.PLUS_EQUAL, "+=", null);
                else {
                    addToken(TokenType.PLUS, "+", null);
                    return; // don't consume
                }
                consume();
            }
            case '-' -> {
                if (next == '-')
                    addToken(TokenType.MINUS_MINUS, "--", null);
                else if (next == '=')
                    addToken(TokenType.MINUS_EQUAL, "-=", null);
                else {
                    addToken(TokenType.MINUS, "-", null);
                    return; // don't consume
                }
                consume();
            }
            case '*' -> {
                if (next == '=') {
                    addToken(TokenType.STAR_EQUAL, "*=", null);
                    consume();
                }
                else addToken(TokenType.STAR, "*", null);
            }
            case '/' -> {
                if (next == '/')
                    addToken(TokenType.SLASH_SLASH, "//", null);
                else if (next == '=')
                    addToken(TokenType.SLASH_EQUAL, "/=", null);
                else {
                    addToken(TokenType.SLASH, "/", null);
                    return; // don't consume
                }
                consume();
            }
            case '%' -> {
                if (next == '=') {
                    addToken(TokenType.MODULO_EQUAL, "%=", null);
                    consume();
                }
                else addToken(TokenType.MODULO, "%", null);
            }
            case '^' -> {
                if (next == '=') {
                    addToken(TokenType.CARET_EQUAL, "^=", null);
                    consume();
                }
                else addToken(TokenType.CARET, "^", null);
            }
            case '<' -> {
                if (next == '=') {
                    addToken(TokenType.LESS_EQUAL, "<=", null);
                    consume();
                }
                else addToken(TokenType.LESS, "<", null);
            }
            case '>' -> {
                if (next == '=') {
                    addToken(TokenType.GREATER_EQUAL, ">=", null);
                    consume();
                }
                else addToken(TokenType.GREATER, ">", null);
            }
            case '=' -> {
                if (next == '=') {
                    addToken(TokenType.EQUAL_EQUAL, "==", null);
                    consume();
                }
                else addToken(TokenType.EQUAL, "=", null);
            }
            case '&' -> {
                if (next == '&') {
                    addToken(TokenType.AND, "&&", null);
                    consume();
                }
                else throw new InvalidTokenException("&", new Pos(fileName, line + 1, column));
            }
            case '|' -> {
                if (next == '|') {
                    addToken(TokenType.OR, "||", null);
                    consume();
                }
                else  throw new InvalidTokenException("|", new Pos(fileName, line + 1, column));
            }
            case '!' -> {
                if (next == '=') {
                    addToken(TokenType.NOT_EQUAL, "!=", null);
                    consume();
                }
                else addToken(TokenType.NOT, "!", null);
            }
            case '.' -> {
                if (next == '.') {
                    addToken(TokenType.RANGE, "..", null);
                    consume();
                }
                else addToken(TokenType.DOT, ".", null);
            }
        }
    }

    // when the single char doesn't match is expected in switch
    private void tokenizeDefault(char c) {
        if (c == '"')
            scanString(c);
        else if (isDigit(c))
            scanNumber(c);
        else if (c == '_' || isAlpha(c)) {
            var id = scanIdentifier(c);
            // check if identifier is keyword
            var tokenType = tokenMap.getOrDefault(id, TokenType.IDENTIFIER);
            addToken(tokenType, id, null);
        }
        // throw invalid token at position
        else throw new InvalidTokenException(String.valueOf(c), new Pos(fileName, line + 1, column));
    }

    private void tokenize() {
        while (cursor < source.length()) {
            char c = this.consume();

            switch (c) {
                case '+', '<', '>', '=', '&', '|', '!', '%', '^', '-', '*', '/', '.' -> scanOperator(c);
                case ',' -> addToken(TokenType.COMMA, ",", null);
                case ':' -> addToken(TokenType.COLON, ":", null);
                case '#' -> skipComment();
                case '(' -> addToken(TokenType.LEFT_PAREN, "(", null);
                case ')' -> addToken(TokenType.RIGHT_PAREN, ")", null);
                case '{' -> addToken(TokenType.LEFT_BRACE, "{", null);
                case '}' -> addToken(TokenType.RIGHT_BRACE, "}", null);
                case '[' -> addToken(TokenType.LEFT_BRACKET, "[", null);
                case ']' -> addToken(TokenType.RIGHT_BRACKET, "]", null);
                case ';' -> addToken(TokenType.SEMICOLON, ";", null);
                case '\n', '\t', ' ' -> {}
                default -> tokenizeDefault(c);
            }
        }
    }

    public List<Token> tokens() {
        return tokens;
    }
}
