package com.doruk.lexer.exceptions;

import com.doruk.dto.Pos;

public class UnterminatedString extends LexerException {
    public UnterminatedString(Pos pos, String lexeme) {
        super("Unterminated String Literal, expected \" got EOF. on " +
                pos.file() +
                ":" +
                pos.line() +
                ":" + pos.column() +
                " span: " + lexeme.length()
        );
    }
}
