package com.doruk.lexer.dto;

public record Pos(
        int line,
        int column
) {
    /**
     * Returns the position as a string
     * @return
     */
    @Override
    public String toString() {
        return "Pos(" + line + "," + column + ")";
    }
}
