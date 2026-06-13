package com.doruk.lexer;

import com.doruk.lexer.dto.Pos;
import com.doruk.lexer.dto.Token;
import com.doruk.lexer.exceptions.InvalidEscape;
import com.doruk.lexer.exceptions.UnterminatedString;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer {
    private String source;
    private List<Token> tokens;
    private int line = 0;
    private int column = 0;
    private int cursor = 0;

    private Map<String, TokenType> tokenMap;

    public Lexer(String source) {
        this.source = source;

        this.tokenMap = new HashMap<>();
        // fill the token map
        Arrays.stream(TokenType.values())
                .forEach(tokenType -> tokenMap.put(tokenType.name().toLowerCase(), tokenType));

        // tokenize
        tokenize();

        // append eof token
        tokens.add(new Token(TokenType.EOF, "", null,
                new Pos(line + 1, column)));
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

    private void skipComment() {
        while (view() != '\n' && cursor < source.length())
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

    private void createToken(TokenType type) {
        var lexeme = String.valueOf(consume());
        tokens.add(new Token(type, lexeme, null,
                new Pos(line + 1, column + 1)));
    }

    private String scanIdentifier(char c) {
        return null;
    }

    private BigDecimal scanNumber(char c) {
        return null;
    }

    private String[] scanString(char c) {
        var lexeme = new StringBuilder();
        var literal = new StringBuilder();
        lexeme.append(c);

        var last = c;
        while (cursor < source.length()) {
            // check escape
            if (view() == '\\') {
                var e = getEscapedChar(viewNext());
                if (e == '\0')
                    throw new InvalidEscape(viewNext(), new Pos(line + 1, column + 1));

                lexeme.append((last = consume()));
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
            throw new UnterminatedString(new Pos(line + 1, column + 1), lexeme.toString());

        return new String[]{lexeme.toString(), literal.toString()};
    }

    // when the single char doesn't match is expected in switch
    private void tokenizeDefault(char c) {
        if (c == '"') {
            var str = scanString(c);
        } else if (isDigit(c)) {
            BigDecimal nm = scanNumber(c);
        } else if (c == '_' || isAlpha(c)) {
            var id = scanIdentifier(c);
            // check if identifier is keyword
        }
        // throw invalid token at position
    }

    private void tokenize() {
        while (cursor < source.length()) {
            char c = this.consume();

            switch (c) {
                case '+' -> {
                }
                case '-' -> {
                }
                case '*' -> {
                }
                case '/' -> {
                }
                case '%' -> {
                }
                case '^' -> {
                }
                case '<' -> {
                }
                case '>' -> {
                }
                case '=' -> {
                }
                case '!' -> {
                }
                case '&' -> {
                }
                case '|' -> {
                }
                case ',' -> {
                }
                case '.' -> {
                }
                case ':' -> {
                }
                case '#' -> {
                }
                case '(' -> {
                }
                case ')' -> {
                }
                case '{' -> {
                }
                case '}' -> {
                }
                case '[' -> {
                }
                case ']' -> {
                }
                case '\\' -> {
                }
                case '\n', '\t', ' ' -> consume();
                default -> {

                }
            }
        }
    }

    public List<Token> tokens() {
        return tokens;
    }
}
