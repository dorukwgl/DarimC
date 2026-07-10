package com.doruk.parser.nodes.types;

import com.doruk.dto.Token;
import com.doruk.parser.interfaces.TypeNode;

public record PrimitiveType(
        Token type
) implements TypeNode {
}
