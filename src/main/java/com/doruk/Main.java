package com.doruk;

import com.doruk.argue.ArgParser;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    private static void printHelp(ArgParser parser) {
        System.out.println("Darimc - Compiler Options");
        System.out.println("Usage: darimc [options] [source files]");
        System.out.println("Usage: darimc [options] [source files] [output file]");
        System.out.println("Example: darimc hello.dm hi.dm file.dm -o program");
        System.out.println("Example: darimc hello.dm -o program");
        System.out.println("Example: darimc hello.dm");
        System.out.print("Options:");
        System.out.println(parser.getOptions());
    }

    static void main(String[] args) {
        // register global error handler
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.out.println("Uncaught exception: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        });

        var parser = createCliOptionParser();
        parser.parse(args);

        if (parser.containsFlag("h") || parser.containsFlag("help")) {
            printHelp(parser);
            return;
        }
        if (parser.containsFlag("v") || parser.containsFlag("version")) {
            System.out.println("Darimc - Version 0.1.0");
            return;
        }

        new Compiler(parser);
    }

    private static ArgParser createCliOptionParser() {
        var parser = new ArgParser();
        parser.arg("h", "help", "Show help");
        parser.arg("v", "version", "Show Version");
        parser.longArg("lint", "check source files for errors");
        parser.shortValueArg("o", "Specify the Output file. Extension is not needed");
        parser.longValueArg("dump-bytecode", "Emit human readable text bytecode");
        parser.longArg("dump-token", "Prints token stream and exit");
        parser.longValueArg("dump-ast", "Prints AST and exit");
        return parser;
    }
}
