package com.doruk.parser.nodes.stmt;

import com.doruk.parser.interfaces.Stmt;

import java.util.List;

public record BlockStmt(
        List<Stmt> statements
) implements Stmt {
}