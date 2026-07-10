package com.doruk.parser.nodes.stmt;

import com.doruk.dto.Token;
import com.doruk.parser.interfaces.Stmt;

public record BreakStmt(
        Token tkn
) implements Stmt {
}
