package com.doruk.parser;

public class StatementParser {
    private final ParserState state;
    private final ExpressionParser expressionParser;

    public StatementParser(ParserState state, ExpressionParser expressionParser) {
        this.state = state;
        this.expressionParser = expressionParser;
    }
}
