package com.doruk.lexer.exceptions;

import com.doruk.lexer.dto.Pos;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String lexeme, Pos pos) {
        super("Illegal token:" + lexeme +
                " on line:" + pos.line() +
                " column:" + pos.column() +
                " span: " + lexeme.length()
        );
    }
}
