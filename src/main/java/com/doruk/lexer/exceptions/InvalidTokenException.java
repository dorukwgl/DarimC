package com.doruk.lexer.exceptions;

import com.doruk.dto.Pos;

public class InvalidTokenException extends LexerException {
    public InvalidTokenException(String lexeme, Pos pos) {
        super("Illegal token:" + lexeme +
                " on " + pos.file() +
                ":" + pos.line() +
                ":" + pos.column() +
                " span: " + lexeme.length()
        );
    }
}
