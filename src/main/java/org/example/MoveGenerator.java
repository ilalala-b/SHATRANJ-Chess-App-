package org.example;

import java.util.ArrayList;
import java.util.List;

public class MoveGenerator {

    public static List<Move> generateLegalMoves(Board board) {
        List<Move> pseudoLegal = generatePseudoLegalMoves(board);
        List<Move> legal = new ArrayList<>();

        Piece.Color mover = board.getSideToMove();
        for (Move move : pseudoLegal) {
            Board.MoveInfo info = board.makeMove(move);
            if (!board.isInCheck(mover)) {
                legal.add(move);
            }
            board.unmakeMove(info);
        }
        return legal;
    }

    public static List<Move> generatePseudoLegalMoves(Board board) {
        List<Move> moves = new ArrayList<>();
        Piece.Color me = board.getSideToMove();
        Piece.Color opponent = (me == Piece.Color.WHITE) ? Piece.Color.BLACK : Piece.Color.WHITE;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                if (p == null || p.getColor() != me) continue;

                switch (p.getType()) {
                    case PAWN:   generatePawnMoves(board, moves, r, c, me); break;
                    case KNIGHT: generateKnightMoves(board, moves, r, c, me); break;
                    case BISHOP: generateSlidingMoves(board, moves, r, c, me, new int[][]{{-1,-1},{-1,1},{1,-1},{1,1}}); break;
                    case ROOK:   generateSlidingMoves(board, moves, r, c, me, new int[][]{{-1,0},{1,0},{0,-1},{0,1}}); break;
                    case QUEEN:  generateSlidingMoves(board, moves, r, c, me, new int[][]{{-1,-1},{-1,1},{1,-1},{1,1},{-1,0},{1,0},{0,-1},{0,1}}); break;
                    case KING:   generateKingMoves(board, moves, r, c, me); break;
                }
            }
        }
        return moves;
    }

    private static void generatePawnMoves(Board board, List<Move> moves, int r, int c, Piece.Color me) {
        int direction = (me == Piece.Color.WHITE) ? -1 : 1;
        int startRow = (me == Piece.Color.WHITE) ? 6 : 1;
        int promotionRow = (me == Piece.Color.WHITE) ? 0 : 7;

        int oneForward = r + direction;
        if (oneForward >= 0 && oneForward <= 7 && board.getPiece(oneForward, c) == null) {
            if (oneForward == promotionRow) {
                addPromotionMoves(moves, r, c, oneForward, c);
            } else {
                moves.add(new Move(r, c, oneForward, c));
            }

            if (r == startRow) {
                int twoForward = r + 2 * direction;
                if (board.getPiece(twoForward, c) == null) {
                    moves.add(new Move(r, c, twoForward, c, Move.Flag.DOUBLE_PAWN_PUSH));
                }
            }
        }

        for (int dc : new int[]{-1, 1}) {
            int targetCol = c + dc;
            if (targetCol < 0 || targetCol > 7) continue;
            int targetRow = r + direction;
            if (targetRow < 0 || targetRow > 7) continue;

            Piece target = board.getPiece(targetRow, targetCol);
            if (target != null && target.getColor() != me) {
                if (targetRow == promotionRow) {
                    addPromotionMoves(moves, r, c, targetRow, targetCol);
                } else {
                    moves.add(new Move(r, c, targetRow, targetCol));
                }
            } else if (target == null
                    && targetRow == board.getEnPassantRow()
                    && targetCol == board.getEnPassantCol()) {
                moves.add(new Move(r, c, targetRow, targetCol, Move.Flag.EN_PASSANT));
            }
        }
    }

    private static void addPromotionMoves(List<Move> moves, int fromRow, int fromCol, int toRow, int toCol) {
        moves.add(new Move(fromRow, fromCol, toRow, toCol, Move.Flag.PROMOTION_QUEEN));
        moves.add(new Move(fromRow, fromCol, toRow, toCol, Move.Flag.PROMOTION_ROOK));
        moves.add(new Move(fromRow, fromCol, toRow, toCol, Move.Flag.PROMOTION_BISHOP));
        moves.add(new Move(fromRow, fromCol, toRow, toCol, Move.Flag.PROMOTION_KNIGHT));
    }

    private static void generateKnightMoves(Board board, List<Move> moves, int r, int c, Piece.Color me) {
        int[][] offsets = {
                {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
                {1, -2}, {1, 2}, {2, -1}, {2, 1}
        };
        for (int[] off : offsets) {
            int nr = r + off[0];
            int nc = c + off[1];
            if (nr < 0 || nr > 7 || nc < 0 || nc > 7) continue;
            Piece target = board.getPiece(nr, nc);
            if (target == null || target.getColor() != me) {
                moves.add(new Move(r, c, nr, nc));
            }
        }
    }

    private static void generateSlidingMoves(Board board, List<Move> moves, int r, int c, Piece.Color me, int[][] directions) {
        for (int[] dir : directions) {
            int nr = r + dir[0];
            int nc = c + dir[1];
            while (nr >= 0 && nr <= 7 && nc >= 0 && nc <= 7) {
                Piece target = board.getPiece(nr, nc);
                if (target == null) {
                    moves.add(new Move(r, c, nr, nc));
                } else {
                    if (target.getColor() != me) {
                        moves.add(new Move(r, c, nr, nc));
                    }
                    break;
                }
                nr += dir[0];
                nc += dir[1];
            }
        }
    }

    private static void generateKingMoves(Board board, List<Move> moves, int r, int c, Piece.Color me) {
        int[][] offsets = {
                {-1, -1}, {-1, 0}, {-1, 1}, {0, -1},
                {0, 1}, {1, -1}, {1, 0}, {1, 1}
        };
        for (int[] off : offsets) {
            int nr = r + off[0];
            int nc = c + off[1];
            if (nr < 0 || nr > 7 || nc < 0 || nc > 7) continue;
            Piece target = board.getPiece(nr, nc);
            if (target == null || target.getColor() != me) {
                moves.add(new Move(r, c, nr, nc));
            }
        }

        Piece.Color opponent = (me == Piece.Color.WHITE) ? Piece.Color.BLACK : Piece.Color.WHITE;

        if (me == Piece.Color.WHITE && r == 7 && c == 4) {
            if (canCastleKingside(board, 7, me, opponent)) {
                moves.add(new Move(7, 4, 7, 6, Move.Flag.CASTLE_KINGSIDE));
            }
            if (canCastleQueenside(board, 7, me, opponent)) {
                moves.add(new Move(7, 4, 7, 2, Move.Flag.CASTLE_QUEENSIDE));
            }
        } else if (me == Piece.Color.BLACK && r == 0 && c == 4) {
            if (canCastleKingside(board, 0, me, opponent)) {
                moves.add(new Move(0, 4, 0, 6, Move.Flag.CASTLE_KINGSIDE));
            }
            if (canCastleQueenside(board, 0, me, opponent)) {
                moves.add(new Move(0, 4, 0, 2, Move.Flag.CASTLE_QUEENSIDE));
            }
        }
    }

    private static boolean canCastleKingside(Board board, int row, Piece.Color me, Piece.Color opponent) {
        Piece king = board.getPiece(row, 4);
        Piece rook = board.getPiece(row, 7);
        if (king == null || king.getType() != Piece.Type.KING || king.getColor() != me) return false;
        if (rook == null || rook.getType() != Piece.Type.ROOK || rook.getColor() != me) return false;
        if (board.getPiece(row, 5) != null || board.getPiece(row, 6) != null) return false;
        if (board.isSquareAttacked(row, 4, opponent)) return false;
        if (board.isSquareAttacked(row, 5, opponent)) return false;
        if (board.isSquareAttacked(row, 6, opponent)) return false;
        return true;
    }

    private static boolean canCastleQueenside(Board board, int row, Piece.Color me, Piece.Color opponent) {
        Piece king = board.getPiece(row, 4);
        Piece rook = board.getPiece(row, 0);
        if (king == null || king.getType() != Piece.Type.KING || king.getColor() != me) return false;
        if (rook == null || rook.getType() != Piece.Type.ROOK || rook.getColor() != me) return false;
        if (board.getPiece(row, 1) != null
                || board.getPiece(row, 2) != null
                || board.getPiece(row, 3) != null) return false;
        if (board.isSquareAttacked(row, 4, opponent)) return false;
        if (board.isSquareAttacked(row, 3, opponent)) return false;
        if (board.isSquareAttacked(row, 2, opponent)) return false;
        return true;
    }

    public static boolean hasAnyLegalMove(Board board) {
        List<Move> pseudo = generatePseudoLegalMoves(board);
        Piece.Color mover = board.getSideToMove();
        for (Move move : pseudo) {
            Board.MoveInfo info = board.makeMove(move);
            boolean legal = !board.isInCheck(mover);
            board.unmakeMove(info);
            if (legal) return true;
        }
        return false;
    }
}