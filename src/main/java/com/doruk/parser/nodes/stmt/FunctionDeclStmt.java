package com.doruk.parser.nodes.stmt;

import com.doruk.dto.Token;
import com.doruk.parser.interfaces.Stmt;
import com.doruk.parser.interfaces.TypeNode;
import com.doruk.parser.nodes.components.Parameter;

import java.util.List;

public record FunctionDeclStmt(
        Token qualifier, // visible or not
        List<TypeNode> returnTypes, // if void, then only one
        Token name,
        List<Parameter> parameters,
        BlockStmt body
) implements Stmt {
}
