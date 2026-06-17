package com.doruk.lexer.exceptions;

import com.doruk.dto.Pos;

public class InvalidEscape extends LexerException {
    public InvalidEscape(char c, Pos pos) {
        super("Illegal escape sequence: " + c +
                " on " + pos.file() + ":" + pos.line() + ":" + pos.column() + " span: 1");
    }
}
