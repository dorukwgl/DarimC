package com.doruk.parser.nodes.expr;

import com.doruk.parser.interfaces.Expr;

public record IndexExpr(
        Expr object,
        Expr index
) implements Expr {
}
