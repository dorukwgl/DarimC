package com.doruk.parser.nodes.stmt;

import com.doruk.dto.Token;
import com.doruk.parser.interfaces.Expr;

public record EnumMember(
        Token name,
        Expr value
) {
}
