package com.doruk.parser.nodes.components;

import com.doruk.dto.Token;
import com.doruk.parser.interfaces.Expr;
import com.doruk.parser.interfaces.TypeNode;

public record Parameter(
        Token name,
        TypeNode type,
        Expr defaultValue
) {
}
