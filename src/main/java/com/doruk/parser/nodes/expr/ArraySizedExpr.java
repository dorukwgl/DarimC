package com.doruk.parser.nodes.expr;

import com.doruk.parser.interfaces.Expr;

public record ArraySizedExpr(
        Expr size
) implements Expr {
}
