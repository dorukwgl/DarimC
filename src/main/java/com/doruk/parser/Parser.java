package com.doruk.parser;

import com.doruk.dto.Token;
import com.doruk.lexer.TokenType;
import com.doruk.parser.interfaces.Stmt;
import com.doruk.parser.nodes.ParserError;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Parser {
    private final List<Token> tokens;
    private final ParserState state;

    private final List<Stmt> program;
    private final List<ParserError> errors;

    private final StatementParser statementParser;
    private final ExpressionParser expressionParser;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.state = new ParserState(tokens);
        this.program = new ArrayList<>(20);
        this.errors = new ArrayList<>(10);

        this.expressionParser = new ExpressionParser(this.state);
        this.statementParser = new StatementParser(this.state, this.expressionParser);
    }

    private void error(Token errorToken, String msg) {
        errors.add(new ParserError(msg, errorToken));
    }

    // find the next statement boundary
    private void synchronize() {
        // consume the illegal token
        state.advance();

        // consume until statement boundary
        while (!state.isAtEnd()) {
            switch (state.peek().type()) {
                case SEMICOLON,
                     ENUM,
                     VAR,
                     FINAL,
                     IF,
                     ELSE,
                     FOR,
                     WHILE,
                     MATCH,
                     VOID,
                     IMPORT,
                     VISIBLE,
                     RIGHT_BRACE,
                     RETURN,
                     BREAK
                     -> {
                    return;
                }
                case NUM, STRING, BOOL -> {
                    var tkn = state.previous().type();
                    if (!(tkn == TokenType.AS || tkn == TokenType.COLON))
                        return; // its a start of function declaration
                }
            }
            state.advance();
        }
    }

    private Optional<Token> consume(TokenType expected, String msg) {
        if (state.match(expected))
            return Optional.of(state.peek());

        this.error(state.peek(), msg + " got " + state.peek().lexeme());
        return Optional.empty();
    }


}
