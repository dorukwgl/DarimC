package com.doruk.parser.nodes.stmt;

import com.doruk.dto.Token;
import com.doruk.parser.interfaces.Stmt;

public record VisibleStmt(
        Token keyword,
        Token identifier
)
implements Stmt {
}
