package com.doruk.parser.interfaces;

import com.doruk.parser.nodes.stmt.*;

public sealed interface Stmt extends AstNode permits BlockStmt, BreakStmt, EnumDeclStmt, ExpressionStmt, ForStmt, FunctionDeclStmt, IfStmt, ImportStmt, ReturnStmt, VariableDeclStmt, VisibleStmt, WhileStmt
{
}
