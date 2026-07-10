package com.doruk.parser.nodes.stmt;

import com.doruk.dto.Token;
import com.doruk.parser.interfaces.Expr;
import com.doruk.parser.interfaces.Stmt;
import com.doruk.parser.nodes.components.CaseBlock;

import java.util.List;

public record SwitchStmt(
        Token keyword,
        Expr expr,
        List<CaseBlock> cases,
        BlockStmt defaultCase
) implements Stmt {
}
