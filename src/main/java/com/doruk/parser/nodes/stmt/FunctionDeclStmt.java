package com.doruk.parser.nodes.stmt;

import com.doruk.dto.Token;
import com.doruk.parser.interfaces.Stmt;
import com.doruk.parser.interfaces.TypeNode;
import com.doruk.parser.nodes.components.Parameter;

import java.util.List;

public record FunctionDeclStmt(
        List<TypeNode> returnTypes, // if void, then only one
        Token identifier,
        List<Parameter> parameters,
        BlockStmt body
) implements Stmt {
}
