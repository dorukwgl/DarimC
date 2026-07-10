package com.doruk.parser.nodes.expr;

import com.doruk.parser.interfaces.Expr;

import java.util.List;

public record CallExpr(
    Expr callee,
    List<Expr> arguments
) implements Expr {
}
