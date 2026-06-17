package com.doruk.dto;

import com.doruk.lexer.TokenType;

public record Token(
        TokenType type,
        String lexeme,
        Object literal,
        Pos pos
) {
    @Override
    public String toString() {
        return "Token(" +
                "type=" + type +
                ", lexeme='" + lexeme + '\'' +
                ", literal=" + literal +
                ", pos=" + pos +
                ")";
    }
}
