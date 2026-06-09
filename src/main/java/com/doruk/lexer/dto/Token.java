package com.doruk.lexer.dto;

import com.doruk.lexer.TokenType;

public record Token(
        TokenType type,
        String lexeme,
        Object literal,
        Pos pos
) {
}
