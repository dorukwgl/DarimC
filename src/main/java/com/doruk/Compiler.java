package com.doruk;

import com.doruk.argue.ArgParser;
import com.doruk.lexer.Lexer;
import com.doruk.lexer.dto.Token;
import com.doruk.lexer.exceptions.LexerException;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Compiler {
    private final ArgParser args;
    private final List<List<Token>> accumulation;

    public  Compiler(ArgParser argParser) {
        this.args = argParser;
        this.accumulation = new ArrayList<>();

        // check all files are source files
        var invalid  = args.getDefaultArgs().stream()
                .filter(f -> !f.endsWith(".dm"))
                .map(f -> {
                    System.out.println("Not a source file: " + f);
                    return true;
                })
                .findFirst()
                .orElse(false);
        if (invalid)
            return;

        try {
            // start lexing files
            startLexing();

            // check flags, else move to parser
            if (args.containsFlag("dump-token")) {
                dumpTokens();
                return;
            }
        } catch (LexerException e) {
            System.out.println(e.getMessage());
            System.exit(-1);
        }

    }

    private void dumpTokens() {
        var out = args.getValue("o");
        try {
            new BufferedWriter(new FileWriter(out != null ? out : "tokens_dump.txt"))
                    .write(
                            accumulation.stream()
                                    .map(f -> f.stream()
                                            .map(Token::toString)
                                            .reduce("", (a, b) -> a + "\n" + b))
                                    .reduce("", (a, b) -> a + "\n\n\n" + b)
                    );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void startLexing() {
        // iterate over given files
        for (String file : args.getDefaultArgs()) {
            String source = null;
            try {
                source = Files.readString(Paths.get(file));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (source.isBlank())
                return;
            accumulation.add(new Lexer(source).tokens());
        }
    }
}
