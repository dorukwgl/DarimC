package com.doruk.parser.nodes.stmt;

import com.doruk.dto.Token;
import com.doruk.parser.interfaces.Expr;
import com.doruk.parser.interfaces.Stmt;
import com.doruk.parser.nodes.components.VariableDecl;

import java.util.List;

public record VariableDeclStmt(
        Token qualifier, // visible or not/ can be null
        Token modifier, // var / final
        List<VariableDecl> declarations,
        Expr initializer
) implements Stmt {
}
