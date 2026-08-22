package com.doruk.parser.nodes.expr;

import com.doruk.dto.Token;
import com.doruk.parser.interfaces.Expr;

public record UnaryExpr(
    Token operator,
    boolean isPrefix,
    Expr expr
) implements Expr {
}
