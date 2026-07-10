package com.doruk.parser.nodes;

import java.util.List;

public record ParserResult(
        Program program,
        List<ParserError> errors
) {
}
