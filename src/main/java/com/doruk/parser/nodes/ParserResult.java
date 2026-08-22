package com.doruk.parser.nodes;

import java.util.List;

public record ParserResult(
        Program program,
        List<ParserError> errors
) {
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
