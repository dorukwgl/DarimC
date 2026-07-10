package com.doruk.parser.nodes.stmt;

import com.doruk.dto.Token;
import com.doruk.parser.interfaces.Expr;
import com.doruk.parser.interfaces.Stmt;

public record IfStmt(
        Token keyword,
        Expr condition,
        BlockStmt thenBlock,
        Stmt elseBlock // either BlockStmt or IfStmt or null of no else block given
) implements Stmt {
}
