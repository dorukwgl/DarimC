package com.doruk.parser.nodes.stmt;

import com.doruk.dto.Token;
import com.doruk.parser.interfaces.Stmt;

public record ImportStmt(
        Token keyword,
        Token file,
        Token asKeyword,
        Token alias
) implements Stmt {
}
