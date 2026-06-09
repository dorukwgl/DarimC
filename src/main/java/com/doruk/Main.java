package com.doruk;

import com.doruk.argue.ArgParser;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    private static void printHelp(ArgParser parser) {
        System.out.println("Darimc - Compiler Options");
        System.out.println("Usage: darimc [options] [source files]");
        System.out.println("Usage: darimc [options] [source files] [output file]");
        System.out.println("Example: darimc --src hello.dm hi.dm file.dm -o program");
        System.out.println("Example: darimc hello.dm -o program");
        System.out.println("Example: darimc hello.dm");
        System.out.print("Options:");
        System.out.println(parser.getOptions());
    }
    public static void main(String[] args) {
        var parser = new ArgParser();
       parser.arg("h", "help", "Show help");
       parser.arg("v", "version", "Show Version");
       parser.shortValueArg("o", "Specify the Output file. Extension is not needed");
       parser.longMultiValueArg("src", "Specify the source file(s). One or multiple");

       parser.parse(args);

       if (parser.containsFlag("h") || parser.containsFlag("help"))
           printHelp(parser);
       if (parser.containsFlag("v") || parser.containsFlag("version"))
           System.out.println("Darimc - Version 0.1.0");
    }
}
