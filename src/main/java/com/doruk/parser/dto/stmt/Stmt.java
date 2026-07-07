package com.doruk.parser.dto.stmt;

import com.doruk.parser.dto.AstNode;

public sealed interface Stmt extends AstNode permits ExpressionStmt {
}
