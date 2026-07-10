package com.doruk.parser.nodes.stmt;

import com.doruk.parser.interfaces.Expr;
import com.doruk.parser.interfaces.Stmt;

public record ExpressionStmt(
        Expr expr
) implements Stmt {
}
