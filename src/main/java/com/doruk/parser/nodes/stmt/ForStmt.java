package com.doruk.parser.nodes.stmt;

import com.doruk.dto.Token;
import com.doruk.parser.interfaces.Expr;
import com.doruk.parser.interfaces.Stmt;
import com.doruk.parser.nodes.components.VariableDecl;

import java.util.List;

public record ForStmt(
        Token keyword,
        Token modifier,          // var/final/null
        List<VariableDecl> variables,
        Expr iterable,
        BlockStmt body
) implements Stmt {
}
