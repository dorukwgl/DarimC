package com.doruk.parser.nodes.components;

import com.doruk.dto.Token;
import com.doruk.parser.interfaces.Expr;
import com.doruk.parser.nodes.stmt.BlockStmt;

public record CaseBlock(
        Token keyword,
        Expr value,
        BlockStmt statement
) {
}
