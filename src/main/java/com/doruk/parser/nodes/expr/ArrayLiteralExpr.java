package com.doruk.parser.nodes.expr;

import com.doruk.parser.interfaces.Expr;

import java.util.List;

public record ArrayLiteralExpr(
        List<Expr> elements
) implements Expr {
}
