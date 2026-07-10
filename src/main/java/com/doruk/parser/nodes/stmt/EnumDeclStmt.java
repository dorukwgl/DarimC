package com.doruk.parser.nodes.stmt;

import com.doruk.dto.Token;
import com.doruk.parser.interfaces.Stmt;

import java.util.List;

public record EnumDeclStmt(
        Token keyword,
        Token name,
        List<EnumMember> members
) implements Stmt {
}
