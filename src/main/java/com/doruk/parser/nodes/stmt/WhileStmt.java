package com.doruk.parser.nodes.stmt;

import com.doruk.dto.Token;
import com.doruk.parser.interfaces.Expr;
import com.doruk.parser.interfaces.Stmt;

public record WhileStmt(
        Token keyword,
        Expr condition,
        BlockStmt body
) implements Stmt {
}
