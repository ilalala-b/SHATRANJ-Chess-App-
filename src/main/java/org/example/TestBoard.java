package org.example;

public class TestBoard {
    public static void main(String[] args) {
        Board board = new Board();
        Engine engine = new Engine();

        System.out.println("=== Test 1: Starting position, depth 3 ===");
        long start = System.currentTimeMillis();
        Move best = engine.findBestMove(board, 3);
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("Best move: " + best);
        System.out.println("Time: " + elapsed + "ms, nodes: " + engine.getNodesSearched());

        System.out.println("\n=== Test 2: Free black queen on e3, engine should capture ===");
        board.loadFen("rnb1kbnr/pppppppp/8/8/8/4q3/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        board.print();
        Move takeQueen = engine.findBestMove(board, 3);
        System.out.println("Engine best move: " + takeQueen);
        System.out.println("Expected: d2e3 or f2e3 (pawn captures queen)");

        System.out.println("\n=== Test 3: Mate in 1 back rank ===");
        board.loadFen("6k1/5ppp/8/8/8/8/5PPP/R5K1 w - - 0 1");
        Move mate = engine.findBestMove(board, 3);
        System.out.println("Engine best move: " + mate);
        System.out.println("Expected: a1a8");

        System.out.println("\n=== Test 4: Mate in 2 ===");
        board.loadFen("6k1/5ppp/8/8/8/8/5PPP/1R4K1 w - - 0 1");
        Move mate2 = engine.findBestMove(board, 4);
        System.out.println("Engine best move: " + mate2);
        System.out.println("Expected: b1b8");
    }
}