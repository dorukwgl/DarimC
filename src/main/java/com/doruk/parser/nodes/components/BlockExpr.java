package com.doruk.parser.nodes.components;

import com.doruk.parser.interfaces.Expr;
import com.doruk.parser.interfaces.Stmt;

import java.util.List;

public record BlockExpr(
        List<Stmt> statements,
        Expr result
) {
}
