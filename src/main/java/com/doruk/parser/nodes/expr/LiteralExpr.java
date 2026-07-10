package com.doruk.parser.nodes.expr;

import com.doruk.dto.Token;
import com.doruk.parser.interfaces.Expr;

public record LiteralExpr(
    Token token
) implements Expr {
}
