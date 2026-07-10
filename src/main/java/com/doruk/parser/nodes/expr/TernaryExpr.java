package com.doruk.parser.nodes.expr;

import com.doruk.parser.interfaces.Expr;

public record TernaryExpr(
        Expr condition,
        Expr trueValue,
        Expr falseValue
) implements Expr {
}
