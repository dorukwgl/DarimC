package com.doruk.parser.interfaces;

import com.doruk.parser.nodes.stmt.*;

public sealed interface Stmt extends AstNode permits
        ExpressionStmt,
        BlockStmt,
        BreakStmt,
        EnumDeclStmt,
        ForStmt,
        FunctionDeclStmt,
        IfStmt,
        ImportStmt,
        ReturnStmt,
        VariableDeclStmt,
        WhileStmt
{
}
