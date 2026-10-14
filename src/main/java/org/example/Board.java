package org.example;

public class Board {

    private final Piece[][] squares;

    private Piece.Color sideToMove;
    private int enPassantRow;
    private int enPassantCol;
    private int halfmoveClock;
    private int fullmoveNumber;

    public Board() {
        this.squares = new Piece[8][8];
        this.enPassantRow = -1;
        this.enPassantCol = -1;
        this.halfmoveClock = 0;
        this.fullmoveNumber = 1;
        setStartingPosition();
    }

    public void setStartingPosition() {
        loadFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }

    public void loadFen(String fen) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                squares[r][c] = null;
            }
        }

        String[] parts = fen.split(" ");
        String placement = parts[0];
        int row = 0;
        int col = 0;

        for (int i = 0; i < placement.length(); i++) {
            char ch = placement.charAt(i);
            if (ch == '/') {
                row++;
                col = 0;
            } else if (Character.isDigit(ch)) {
                col += Character.getNumericValue(ch);
            } else {
                squares[row][col] = pieceFromChar(ch);
                col++;
            }
        }

        this.sideToMove = parts[1].equals("w") ? Piece.Color.WHITE : Piece.Color.BLACK;

        if (parts.length > 3 && !parts[3].equals("-")) {
            this.enPassantCol = parts[3].charAt(0) - 'a';
            this.enPassantRow = 8 - Character.getNumericValue(parts[3].charAt(1));
        } else {
            this.enPassantRow = -1;
            this.enPassantCol = -1;
        }

        if (parts.length > 4) {
            this.halfmoveClock = Integer.parseInt(parts[4]);
        }
        if (parts.length > 5) {
            this.fullmoveNumber = Integer.parseInt(parts[5]);
        }
    }

    private Piece pieceFromChar(char ch) {
        Piece.Color color = Character.isUpperCase(ch) ? Piece.Color.WHITE : Piece.Color.BLACK;
        char lower = Character.toLowerCase(ch);
        Piece.Type type;
        switch (lower) {
            case 'p': type = Piece.Type.PAWN; break;
            case 'n': type = Piece.Type.KNIGHT; break;
            case 'b': type = Piece.Type.BISHOP; break;
            case 'r': type = Piece.Type.ROOK; break;
            case 'q': type = Piece.Type.QUEEN; break;
            case 'k': type = Piece.Type.KING; break;
            default: return null;
        }
        return new Piece(color, type);
    }

    public Piece getPiece(int row, int col) {
        if (row < 0 || row > 7 || col < 0 || col > 7) return null;
        return squares[row][col];
    }

    public Piece.Color getSideToMove() {
        return sideToMove;
    }

    public void setSideToMove(Piece.Color color) {
        this.sideToMove = color;
    }

    public int getEnPassantRow() {
        return enPassantRow;
    }

    public int getEnPassantCol() {
        return enPassantCol;
    }

    public void setEnPassant(int row, int col) {
        this.enPassantRow = row;
        this.enPassantCol = col;
    }

    public void print() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = squares[r][c];
                if (p == null) {
                    System.out.print(". ");
                } else {
                    char ch = pieceToChar(p);
                    System.out.print(ch + " ");
                }
            }
            System.out.println();
        }
        System.out.println("Side to move: " + sideToMove);
        System.out.println("En passant: " + (enPassantRow == -1 ? "-" : (char)('a' + enPassantCol) + "" + (8 - enPassantRow)));
    }

    private char pieceToChar(Piece p) {
        char ch;
        switch (p.getType()) {
            case PAWN:   ch = 'p'; break;
            case KNIGHT: ch = 'n'; break;
            case BISHOP: ch = 'b'; break;
            case ROOK:   ch = 'r'; break;
            case QUEEN:  ch = 'q'; break;
            case KING:   ch = 'k'; break;
            default:     ch = '?';
        }
        return p.getColor() == Piece.Color.WHITE ? Character.toUpperCase(ch) : ch;
    }

    public static class MoveInfo {
        public Move move;
        public Piece movedPiece;
        public Piece capturedPiece;
        public int prevEnPassantRow;
        public int prevEnPassantCol;
        public Piece.Color prevSideToMove;
        public int prevHalfmoveClock;
        public Piece pawnPromotedTo;
        public Piece capturedEnPassantPawn;
    }

    public MoveInfo makeMove(Move move) {
        MoveInfo info = new MoveInfo();
        info.move = move;
        info.movedPiece = squares[move.getFromRow()][move.getFromCol()];
        info.capturedPiece = squares[move.getToRow()][move.getToCol()];
        info.prevEnPassantRow = enPassantRow;
        info.prevEnPassantCol = enPassantCol;
        info.prevSideToMove = sideToMove;
        info.prevHalfmoveClock = halfmoveClock;

        squares[move.getToRow()][move.getToCol()] = info.movedPiece;
        squares[move.getFromRow()][move.getFromCol()] = null;

        if (move.getFlag() == Move.Flag.CASTLE_KINGSIDE) {
            Piece rook = squares[move.getFromRow()][7];
            squares[move.getFromRow()][5] = rook;
            squares[move.getFromRow()][7] = null;
        } else if (move.getFlag() == Move.Flag.CASTLE_QUEENSIDE) {
            Piece rook = squares[move.getFromRow()][0];
            squares[move.getFromRow()][3] = rook;
            squares[move.getFromRow()][0] = null;
        } else if (move.getFlag() == Move.Flag.EN_PASSANT) {
            int capturedPawnRow = move.getFromRow();
            int capturedPawnCol = move.getToCol();
            info.capturedEnPassantPawn = squares[capturedPawnRow][capturedPawnCol];
            squares[capturedPawnRow][capturedPawnCol] = null;
        }

        if (move.isPromotion()) {
            Piece.Type promotedType;
            switch (move.getFlag()) {
                case PROMOTION_QUEEN:  promotedType = Piece.Type.QUEEN; break;
                case PROMOTION_ROOK:   promotedType = Piece.Type.ROOK; break;
                case PROMOTION_BISHOP: promotedType = Piece.Type.BISHOP; break;
                case PROMOTION_KNIGHT: promotedType = Piece.Type.KNIGHT; break;
                default:               promotedType = Piece.Type.QUEEN;
            }
            info.pawnPromotedTo = new Piece(info.movedPiece.getColor(), promotedType);
            squares[move.getToRow()][move.getToCol()] = info.pawnPromotedTo;
        }

        if ((info.movedPiece != null && info.movedPiece.getType() == Piece.Type.PAWN)
                || info.capturedPiece != null
                || info.capturedEnPassantPawn != null) {
            halfmoveClock = 0;
        } else {
            halfmoveClock++;
        }

        enPassantRow = -1;
        enPassantCol = -1;

        if (move.getFlag() == Move.Flag.DOUBLE_PAWN_PUSH) {
            enPassantRow = (move.getFromRow() + move.getToRow()) / 2;
            enPassantCol = move.getFromCol();
        }

        sideToMove = (sideToMove == Piece.Color.WHITE) ? Piece.Color.BLACK : Piece.Color.WHITE;
        if (sideToMove == Piece.Color.WHITE) {
            fullmoveNumber++;
        }

        return info;
    }

    public void unmakeMove(MoveInfo info) {
        Move move = info.move;

        squares[move.getFromRow()][move.getFromCol()] = info.movedPiece;
        squares[move.getToRow()][move.getToCol()] = info.capturedPiece;

        if (move.getFlag() == Move.Flag.CASTLE_KINGSIDE) {
            Piece rook = squares[move.getFromRow()][5];
            squares[move.getFromRow()][7] = rook;
            squares[move.getFromRow()][5] = null;
        } else if (move.getFlag() == Move.Flag.CASTLE_QUEENSIDE) {
            Piece rook = squares[move.getFromRow()][3];
            squares[move.getFromRow()][0] = rook;
            squares[move.getFromRow()][3] = null;
        } else if (move.getFlag() == Move.Flag.EN_PASSANT) {
            squares[move.getFromRow()][move.getToCol()] = info.capturedEnPassantPawn;
        }

        enPassantRow = info.prevEnPassantRow;
        enPassantCol = info.prevEnPassantCol;
        sideToMove = info.prevSideToMove;
        halfmoveClock = info.prevHalfmoveClock;
        if (sideToMove == Piece.Color.BLACK) {
            fullmoveNumber--;
        }
    }

    public boolean isSquareAttacked(int row, int col, Piece.Color byColor) {
        int pawnRow = (byColor == Piece.Color.WHITE) ? row + 1 : row - 1;
        for (int dc : new int[]{-1, 1}) {
            Piece p = getPiece(pawnRow, col + dc);
            if (p != null && p.getColor() == byColor && p.getType() == Piece.Type.PAWN) {
                return true;
            }
        }

        int[][] knightOffsets = {
                {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
                {1, -2}, {1, 2}, {2, -1}, {2, 1}
        };
        for (int[] off : knightOffsets) {
            Piece p = getPiece(row + off[0], col + off[1]);
            if (p != null && p.getColor() == byColor && p.getType() == Piece.Type.KNIGHT) {
                return true;
            }
        }

        int[][] kingOffsets = {
                {-1, -1}, {-1, 0}, {-1, 1}, {0, -1},
                {0, 1}, {1, -1}, {1, 0}, {1, 1}
        };
        for (int[] off : kingOffsets) {
            Piece p = getPiece(row + off[0], col + off[1]);
            if (p != null && p.getColor() == byColor && p.getType() == Piece.Type.KING) {
                return true;
            }
        }

        int[][] diagonalDirs = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
        for (int[] dir : diagonalDirs) {
            int r = row + dir[0];
            int c = col + dir[1];
            while (r >= 0 && r <= 7 && c >= 0 && c <= 7) {
                Piece p = squares[r][c];
                if (p != null) {
                    if (p.getColor() == byColor
                            && (p.getType() == Piece.Type.BISHOP || p.getType() == Piece.Type.QUEEN)) {
                        return true;
                    }
                    break;
                }
                r += dir[0];
                c += dir[1];
            }
        }

        int[][] straightDirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : straightDirs) {
            int r = row + dir[0];
            int c = col + dir[1];
            while (r >= 0 && r <= 7 && c >= 0 && c <= 7) {
                Piece p = squares[r][c];
                if (p != null) {
                    if (p.getColor() == byColor
                            && (p.getType() == Piece.Type.ROOK || p.getType() == Piece.Type.QUEEN)) {
                        return true;
                    }
                    break;
                }
                r += dir[0];
                c += dir[1];
            }
        }

        return false;
    }

    public int[] findKing(Piece.Color color) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = squares[r][c];
                if (p != null && p.getColor() == color && p.getType() == Piece.Type.KING) {
                    return new int[]{r, c};
                }
            }
        }
        return null;
    }

    public boolean isInCheck(Piece.Color color) {
        int[] kingPos = findKing(color);
        if (kingPos == null) return false;
        Piece.Color opponent = (color == Piece.Color.WHITE) ? Piece.Color.BLACK : Piece.Color.WHITE;
        return isSquareAttacked(kingPos[0], kingPos[1], opponent);
    }
}