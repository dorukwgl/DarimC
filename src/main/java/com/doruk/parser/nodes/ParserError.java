package com.doruk.parser.nodes;

import com.doruk.dto.Token;

public record ParserError(
        Token token,
        String message
) {
}