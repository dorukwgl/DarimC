package com.doruk.parser.nodes;

import com.doruk.dto.Token;

public record ParserError(
        String message,
        Token unexpected
) {
    @Override
    public String toString() {
        var pos = unexpected.pos();
        return message +
                " at:- file: " + pos.file() +
                " line:" + pos.line() +
                " col:" + pos.column() +
                " len:" + unexpected.lexeme().length();
    }
}