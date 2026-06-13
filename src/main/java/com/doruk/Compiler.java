package com.doruk;

import com.doruk.argue.ArgParser;
import com.doruk.lexer.Lexer;
import com.doruk.lexer.dto.Token;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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

        // check flags, else move to parser
        if (args.containsFlag("dump-token")) {
            accumulation.forEach(tokens -> tokens.forEach(System.out::println));
            return;
        }
    }

}
