package com.doruk.parser;

import com.doruk.lexer.TokenType;

import java.util.EnumMap;
import java.util.Map;

public final class PrecedenceTable {
    private final static Map<TokenType, Integer> infix = new EnumMap<>(TokenType.class);
    private final static Map<TokenType, Integer> prefix = new EnumMap<>(TokenType.class);
    private final static Map<TokenType, Integer> postfix = new EnumMap<>(TokenType.class);

    public PrecedenceTable() {
        infix.put(TokenType.EQUAL, 1);
        infix.put(TokenType.PLUS_EQUAL, 1);
        infix.put(TokenType.MINUS_EQUAL, 1);
        infix.put(TokenType.SLASH_EQUAL, 1);
        infix.put(TokenType.STAR_EQUAL, 1);
        infix.put(TokenType.MODULO_EQUAL, 1);
        infix.put(TokenType.CARET_EQUAL, 1);

        infix.put(TokenType.TERNARY, 2);
        infix.put(TokenType.OR, 3);
        infix.put(TokenType.AND, 4);

        infix.put(TokenType.BET, 5);

        infix.put(TokenType.EQUAL_EQUAL, 6);
        infix.put(TokenType.NOT_EQUAL, 6);

        infix.put(TokenType.LESS, 7);
        infix.put(TokenType.GREATER, 7);
        infix.put(TokenType.LESS_EQUAL, 7);
        infix.put(TokenType.GREATER_EQUAL, 7);

        infix.put(TokenType.RANGE, 8);

        infix.put(TokenType.PLUS, 9);
        infix.put(TokenType.MINUS, 9);

        infix.put(TokenType.STAR, 10);
        infix.put(TokenType.SLASH, 10);
        infix.put(TokenType.MODULO, 10);
        infix.put(TokenType.SLASH_SLASH, 10);

        infix.put(TokenType.CARET, 11);
        infix.put(TokenType.AS, 12);

        // prefix precedences
        prefix.put(TokenType.NOT, 13);
        prefix.put(TokenType.MINUS, 13);
        prefix.put(TokenType.PLUS, 13);
        prefix.put(TokenType.PLUS_PLUS, 13);
        prefix.put(TokenType.MINUS_MINUS, 13);

        // postfix precedences
        postfix.put(TokenType.PLUS_PLUS, 14);
        postfix.put(TokenType.MINUS_MINUS, 14);
        postfix.put(TokenType.DOT, 14);
        postfix.put(TokenType.LEFT_PAREN, 14);
        postfix.put(TokenType.LEFT_BRACKET, 14);
    }
}
