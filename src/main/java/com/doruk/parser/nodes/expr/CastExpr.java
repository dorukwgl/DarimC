package com.doruk.parser.nodes.expr;

import com.doruk.dto.Token;
import com.doruk.parser.interfaces.Expr;
import com.doruk.parser.interfaces.TypeNode;

public record CastExpr(
        Token keyword,
        Expr value,
        TypeNode targetType
) implements Expr {
}
