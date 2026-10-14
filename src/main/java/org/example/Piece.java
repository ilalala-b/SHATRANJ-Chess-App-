package org.example;

public class Piece {

    public enum Color {
        WHITE, BLACK
    }

    public enum Type {
        PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING
    }

    private final Color color;
    private final Type type;

    public Piece(Color color, Type type) {
        this.color = color;
        this.type = type;
    }

    public Color getColor() {
        return color;
    }

    public Type getType() {
        return type;
    }

    public boolean isColor(Color other) {
        return this.color == other;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Piece)) return false;
        Piece other = (Piece) obj;
        return color == other.color && type == other.type;
    }

    @Override
    public int hashCode() {
        return color.ordinal() * 31 + type.ordinal();
    }

    @Override
    public String toString() {
        return color + " " + type;
    }
}