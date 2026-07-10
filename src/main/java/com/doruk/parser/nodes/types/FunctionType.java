package com.doruk.parser.nodes.types;

import com.doruk.dto.Token;
import com.doruk.parser.interfaces.TypeNode;

import java.util.List;

public record FunctionType(
        Token keyword,
        List<Token> parameterTypes,
        List<Token> returnTypes
) implements TypeNode {
}
