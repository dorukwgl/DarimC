package com.doruk.parser.nodes.components;

import com.doruk.parser.interfaces.Expr;

public record MatchCase(
        Expr pattern,
        BlockExpr block
) {
}
