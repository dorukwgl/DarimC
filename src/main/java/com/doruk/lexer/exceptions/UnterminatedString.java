package com.doruk.lexer.exceptions;

import com.doruk.lexer.dto.Pos;

public class UnterminatedString extends RuntimeException {
    public UnterminatedString(Pos pos, String lexeme) {
        super("Unterminated String Literal, expected \" got EOF. on line:" +
                pos.line() +
                " column:" + pos.column() +
                " span: " + lexeme.length()
        );
    }
}
