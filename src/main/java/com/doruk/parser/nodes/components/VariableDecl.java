package com.doruk.parser.nodes.components;

import com.doruk.dto.Token;
import com.doruk.parser.interfaces.TypeNode;

public record VariableDecl(
    Token name,
    TypeNode type
){
}
