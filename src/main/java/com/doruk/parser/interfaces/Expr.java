package com.doruk.parser.interfaces;

import com.doruk.parser.nodes.expr.*;

public sealed interface Expr extends AstNode permits
        ArrayLiteralExpr,
        ArraySizedExpr,
        AssignExpr,
        BetExpr,
        BinaryExpr,
        CallExpr,
        CastExpr,
        FuncLiteralExpr,
        IndexExpr,
        LiteralExpr,
        MatchExpr,
        MemberAccessExpr,
        NewExpr,
        TernaryExpr,
        TypeOfExpr,
        UnaryExpr,
        VariableExpr
{
}
