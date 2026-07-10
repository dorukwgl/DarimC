package com.doruk.parser.nodes.expr;

import com.doruk.parser.interfaces.Expr;

public record MemberAccessExpr(
        Expr object,
        Expr member
) implements Expr {
}
