package com.doruk.dto;

public record Pos(
        String file,
        int line,
        int column
) {
    /**
     * Returns the position as a string
     * @return
     */
    @Override
    public String toString() {
        return "Pos(" + file + "," + line + "," + column + ")";
    }
}
