package com.doruk.parser.nodes;

import com.doruk.parser.interfaces.Stmt;

import java.util.List;

public record Program(
        List<Stmt> statements
) {
}
