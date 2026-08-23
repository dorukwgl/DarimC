package com.doruk.parser;

import com.doruk.lexer.TokenType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class PrecedenceTable {
    private final static Map<String, Integer> table = new HashMap<>();
    private final static Map<TokenType, Integer> infix = new EnumMap<>(TokenType.class);

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

        table.putAll(Map.of(
                "bet", 5,

                "==", 6,
                "!=", 6,

                "<", 7,
                ">", 7,
                "<=", 7,
                ">=", 7
        ));
    }
}
