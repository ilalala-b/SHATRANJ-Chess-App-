package org.example;

public class Move {

    public enum Flag {
        NORMAL,
        CASTLE_KINGSIDE,
        CASTLE_QUEENSIDE,
        EN_PASSANT,
        DOUBLE_PAWN_PUSH,
        PROMOTION_QUEEN,
        PROMOTION_ROOK,
        PROMOTION_BISHOP,
        PROMOTION_KNIGHT
    }

    private final int fromRow;
    private final int fromCol;
    private final int toRow;
    private final int toCol;
    private final Flag flag;

    public Move(int fromRow, int fromCol, int toRow, int toCol) {
        this(fromRow, fromCol, toRow, toCol, Flag.NORMAL);
    }

    public Move(int fromRow, int fromCol, int toRow, int toCol, Flag flag) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.flag = flag;
    }

    public int getFromRow() {
        return fromRow;
    }

    public int getFromCol() {
        return fromCol;
    }

    public int getToRow() {
        return toRow;
    }

    public int getToCol() {
        return toCol;
    }

    public Flag getFlag() {
        return flag;
    }

    public boolean isPromotion() {
        return flag == Flag.PROMOTION_QUEEN
                || flag == Flag.PROMOTION_ROOK
                || flag == Flag.PROMOTION_BISHOP
                || flag == Flag.PROMOTION_KNIGHT;
    }

    public boolean isCastle() {
        return flag == Flag.CASTLE_KINGSIDE || flag == Flag.CASTLE_QUEENSIDE;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Move)) return false;
        Move other = (Move) obj;
        return fromRow == other.fromRow
                && fromCol == other.fromCol
                && toRow == other.toRow
                && toCol == other.toCol
                && flag == other.flag;
    }

    @Override
    public int hashCode() {
        int result = fromRow;
        result = 31 * result + fromCol;
        result = 31 * result + toRow;
        result = 31 * result + toCol;
        result = 31 * result + flag.ordinal();
        return result;
    }

    @Override
    public String toString() {
        String notation = "" + (char) ('a' + fromCol) + (8 - fromRow)
                + (char) ('a' + toCol) + (8 - toRow);
        if (flag != Flag.NORMAL) {
            notation += " (" + flag + ")";
        }
        return notation;
    }
}