package com.doruk.parser.interfaces;

import com.doruk.parser.nodes.expr.LiteralExpr;
import com.doruk.parser.nodes.expr.VariableExpr;

public sealed interface Expr extends AstNode permits VariableExpr, LiteralExpr {
}
