package com.doruk.parser.nodes.expr;

import com.doruk.parser.interfaces.Expr;

import java.util.List;

public record AssignExpr(
        List<VariableExpr> targets,
        Expr value
) implements Expr {
}
