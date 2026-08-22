package com.doruk.parser;

import com.doruk.dto.Token;
import com.doruk.lexer.TokenType;

import java.util.Arrays;
import java.util.List;

public class ParserState {
    private final List<Token> tokens;
    private int pos = 0;

    public ParserState(List<Token> tokens) {
        this.tokens = tokens;
    }

    public boolean isAtEnd() {
        return tokens.get(pos).type() == TokenType.EOF;
    }

    public Token peek() {
        return tokens.get(pos);
    }

    public Token previous() {
        return tokens.get(pos - 1);
    }

    public Token advance() {
        if (!isAtEnd()) pos++;
        return tokens.get(pos - 1);
    }

    public boolean match(TokenType... types) {
        var matches = Arrays.stream(types).anyMatch(t -> t == peek().type());
        if (matches) advance();
        return matches;
    }
}
