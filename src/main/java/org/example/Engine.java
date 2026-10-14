package org.example;

import java.util.List;

public class Engine {

    private static final int MATE_SCORE = 100000;
    private static final int DEFAULT_DEPTH = 3;

    private int nodesSearched;

    public Move findBestMove(Board board) {
        return findBestMove(board, DEFAULT_DEPTH);
    }

    public Move findBestMove(Board board, int depth) {
        nodesSearched = 0;
        List<Move> moves = MoveGenerator.generateLegalMoves(board);

        if (moves.isEmpty()) return null;
        if (moves.size() == 1) return moves.get(0);

        orderMoves(board, moves);

        Move bestMove = null;
        int bestScore = -MATE_SCORE * 2;
        int alpha = -MATE_SCORE * 2;
        int beta = MATE_SCORE * 2;

        for (Move move : moves) {
            Board.MoveInfo info = board.makeMove(move);
            int score = -negamax(board, depth - 1, -beta, -alpha);
            board.unmakeMove(info);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
            if (score > alpha) {
                alpha = score;
            }
        }

        return bestMove;
    }

    private int negamax(Board board, int depth, int alpha, int beta) {
        nodesSearched++;

        List<Move> moves = MoveGenerator.generateLegalMoves(board);

        if (moves.isEmpty()) {
            if (board.isInCheck(board.getSideToMove())) {
                return -MATE_SCORE + (10 - depth);
            } else {
                return 0;
            }
        }

        if (depth == 0) {
            int eval = Evaluator.evaluate(board);
            return (board.getSideToMove() == Piece.Color.WHITE) ? eval : -eval;
        }

        orderMoves(board, moves);

        int bestScore = -MATE_SCORE * 2;
        for (Move move : moves) {
            Board.MoveInfo info = board.makeMove(move);
            int score = -negamax(board, depth - 1, -beta, -alpha);
            board.unmakeMove(info);

            if (score > bestScore) {
                bestScore = score;
            }
            if (score > alpha) {
                alpha = score;
            }
            if (alpha >= beta) {
                break;
            }
        }

        return bestScore;
    }

    private void orderMoves(Board board, List<Move> moves) {
        moves.sort((a, b) -> scoreMove(board, b) - scoreMove(board, a));
    }

    private int scoreMove(Board board, Move move) {
        int score = 0;

        Piece victim = board.getPiece(move.getToRow(), move.getToCol());
        Piece attacker = board.getPiece(move.getFromRow(), move.getFromCol());

        if (victim != null && attacker != null) {
            score += 10 * pieceValue(victim.getType()) - pieceValue(attacker.getType());
        }

        if (move.isPromotion()) {
            score += 800;
        }

        if (move.isCastle()) {
            score += 50;
        }

        return score;
    }

    private int pieceValue(Piece.Type type) {
        switch (type) {
            case PAWN:   return 100;
            case KNIGHT: return 320;
            case BISHOP: return 330;
            case ROOK:   return 500;
            case QUEEN:  return 900;
            case KING:   return 20000;
            default:     return 0;
        }
    }

    public int getNodesSearched() {
        return nodesSearched;
    }
}