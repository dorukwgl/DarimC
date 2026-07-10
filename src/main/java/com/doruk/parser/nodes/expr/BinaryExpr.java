package com.doruk.parser.nodes.expr;

import com.doruk.dto.Token;
import com.doruk.parser.interfaces.Expr;

public record BinaryExpr(
        Expr left,
        Token operator,
        Expr right
) implements Expr {
}
