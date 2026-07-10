package com.doruk.parser.nodes.expr;

import com.doruk.parser.interfaces.Expr;

public record TypeOfExpr(
        Expr expression
) implements Expr {
}
