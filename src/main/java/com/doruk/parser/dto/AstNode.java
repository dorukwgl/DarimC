package com.doruk.parser.dto;

import com.doruk.parser.dto.expr.Expr;
import com.doruk.parser.dto.stmt.Stmt;

public sealed interface AstNode permits Stmt, Expr {
}
