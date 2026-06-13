package com.doruk.lexer.exceptions;

import com.doruk.lexer.dto.Pos;

public class InvalidEscape extends LexerException {
    public InvalidEscape(char c, Pos pos) {
        super("Illegal escape sequence: " + c + " on line:" + pos.line() + " column:" + pos.column() + " span: 1");
    }
}
