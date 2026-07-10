package com.doruk.parser.nodes.expr;

import com.doruk.dto.Token;
import com.doruk.parser.interfaces.Expr;

public record BetExpr(
        Token keyword,
        Expr pivot,
        Expr lower,
        Expr upper
) implements Expr {
}
