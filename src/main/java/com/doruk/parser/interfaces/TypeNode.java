package com.doruk.parser.interfaces;

import com.doruk.parser.nodes.types.FunctionType;
import com.doruk.parser.nodes.types.PrimitiveType;

sealed public interface TypeNode permits PrimitiveType, FunctionType {
}
