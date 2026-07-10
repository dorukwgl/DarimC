package com.doruk.parser.nodes.expr;

import com.doruk.parser.interfaces.Expr;
import com.doruk.parser.interfaces.TypeNode;
import com.doruk.parser.nodes.components.Parameter;
import com.doruk.parser.nodes.stmt.BlockStmt;

import java.util.List;

public record FuncLiteralExpr(
        List<TypeNode> returnTypes,
        List<Parameter> parameters,
        BlockStmt body
) implements Expr {
}
