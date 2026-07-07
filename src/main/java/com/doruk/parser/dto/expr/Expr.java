package com.doruk.parser.dto.expr;

import com.doruk.parser.dto.AstNode;

public sealed interface Expr extends AstNode permits VariableExpr, LiteralExpr {
}
