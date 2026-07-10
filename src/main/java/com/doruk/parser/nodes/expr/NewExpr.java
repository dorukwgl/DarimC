package com.doruk.parser.nodes.expr;

import com.doruk.dto.Token;
import com.doruk.parser.interfaces.Expr;

public record NewExpr(
        Token keyword,
        Expr expression
) implements Expr {
}
