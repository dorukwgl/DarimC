package com.doruk.parser.nodes.expr;

import com.doruk.dto.Token;
import com.doruk.parser.interfaces.Expr;
import com.doruk.parser.nodes.components.BlockExpr;
import com.doruk.parser.nodes.components.MatchCase;

import java.util.List;

public record MatchExpr(
        Token keyword,
        Expr value,
        List<MatchCase> cases,
        BlockExpr defaultBlock
) implements Expr {
}
