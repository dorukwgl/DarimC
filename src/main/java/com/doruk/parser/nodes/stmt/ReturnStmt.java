package com.doruk.parser.nodes.stmt;

import com.doruk.dto.Token;
import com.doruk.parser.interfaces.Expr;
import com.doruk.parser.interfaces.Stmt;

public record ReturnStmt(
        Token keyword,
        Expr value
) implements Stmt {
}
