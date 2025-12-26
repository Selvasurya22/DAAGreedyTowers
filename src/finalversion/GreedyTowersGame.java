package game;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class GreedyTowersGame extends JFrame {

    // ================= CONFIG =================
    private static final int N = 5;

    // ================= GAME STATE =================
    private int[][] grid = new int[N][N];
    private boolean[][] rowUsed = new boolean[N][N + 1];
    private boolean[][] colUsed = new boolean[N][N + 1];

    private boolean humanTurn = true;
    private boolean gameOver = false;

    private int humanScore = 0;
    private int cpuScore = 0;

    // ================= GREEDY STRATEGY =================
    enum CPUStrategy {
        SCORE_GREEDY,
        BLOCKING_GREEDY
    }

    private CPUStrategy cpuStrategy = CPUStrategy.SCORE_GREEDY;

    // ================= UI =================
    private JButton[][] buttons = new JButton[N][N];
    private JLabel statusLabel = new JLabel();
    private JLabel scoreLabel = new JLabel();

    // ================= MOVE STRUCT =================
    static class Move {
        int r, c, v;
        int score;
        int oppMoves;

        Move(int r, int c, int v, int score, int oppMoves) {
            this.r = r;
            this.c = c;
            this.v = v;
            this.score = score;
            this.oppMoves = oppMoves;
        }
    }

    // ================= CONSTRUCTOR =================
    public GreedyTowersGame() {
        setTitle("Greedy Towers – Human vs CPU");
        setSize(700, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(createTopPanel(), BorderLayout.NORTH);
        add(createBoard(), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        updateLabels();
        setVisible(true);
    }

    // ================= UI BUILDERS =================
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1));

        JComboBox<String> strategyBox = new JComboBox<>(
                new String[]{"Score Greedy", "Blocking Greedy"}
        );
        strategyBox.addActionListener(e ->
                cpuStrategy = strategyBox.getSelectedIndex() == 0
                        ? CPUStrategy.SCORE_GREEDY
                        : CPUStrategy.BLOCKING_GREEDY
        );

        panel.add(strategyBox);
        panel.add(scoreLabel);
        return panel;
    }

    private JPanel createBoard() {
        JPanel panel = new JPanel(new GridLayout(N, N));

        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                JButton btn = new JButton("");
                btn.setFont(new Font("Arial", Font.BOLD, 20));
                final int rr = r, cc = c;
                btn.addActionListener(e -> handleHumanMove(rr, cc));
                buttons[r][c] = btn;
                panel.add(btn);
            }
        }
        return panel;
    }

    // ================= HUMAN MOVE =================
    private void handleHumanMove(int r, int c) {
        if (gameOver || !humanTurn || grid[r][c] != 0) return;

        for (int v = 1; v <= N; v++) {
            if (canPlace(r, c, v)) {
                placeValue(r, c, v, true);
                break;
            }
        }

        if (!gameOver) {
            humanTurn = false;
            updateHeatMap();
            SwingUtilities.invokeLater(this::doCPUMove);
        }
    }

    // ================= CPU MOVE =================
    private void doCPUMove() {
        Move move = (cpuStrategy == CPUStrategy.SCORE_GREEDY)
                ? getScoreGreedyMove()
                : getBlockingGreedyMove();

        if (move == null) {
            endGame();
            return;
        }

        placeValue(move.r, move.c, move.v, false);
        humanTurn = true;
        updateHeatMap();
    }

    // ================= GREEDY STRATEGY 1 =================
//    private Move getScoreGreedyMove() {
//        Move best = null;
//        int bestScore = -1;
//
//        forEachValidMove((r, c, v) -> {
//            int score = estimateScore(r, c);
//            if (score > bestScore) {
//                bestScore = score;
//                return new Move(r, c, v, score, 0);
//            }
//            return null;
//        });
//
//        return bestScore >= 0 ? bestMoveHolder : null;
//    }
    
    private Move getScoreGreedyMove() {
        Move best = null;
        int bestScore = -1;

        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                if (grid[r][c] != 0) continue;

                for (int v = 1; v <= N; v++) {
                    if (!canPlace(r, c, v)) continue;

                    int score = estimateScore(r, c);
                    if (score > bestScore) {
                        bestScore = score;
                        best = new Move(r, c, v, score, 0);
                    }
                }
            }
        }
        return best;
    }


    // ================= GREEDY STRATEGY 2 =================
//    private Move getBlockingGreedyMove() {
//        Move best = null;
//        int minOpp = Integer.MAX_VALUE;
//
//        forEachValidMove((r, c, v) -> {
//            simulate(r, c, v);
//            int oppMoves = countAllValidMoves();
//            rollback(r, c, v);
//
//            if (oppMoves < minOpp) {
//                minOpp = oppMoves;
//                return new Move(r, c, v, 0, oppMoves);
//            }
//            return null;
//        });
//
//        return bestMoveHolder;
//    }
    private Move getBlockingGreedyMove() {
        Move best = null;
        int minOpp = Integer.MAX_VALUE;

        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                if (grid[r][c] != 0) continue;

                for (int v = 1; v <= N; v++) {
                    if (!canPlace(r, c, v)) continue;

                    // simulate
                    simulate(r, c, v);
                    int oppMoves = countAllValidMoves();
                    rollback(r, c, v);

                    if (oppMoves < minOpp) {
                        minOpp = oppMoves;
                        best = new Move(r, c, v, 0, oppMoves);
                    }
                }
            }
        }
        return best;
    }

    private Move bestMoveHolder = null;

    private void forEachValidMove(MoveEvaluator eval) {
        bestMoveHolder = null;
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                if (grid[r][c] != 0) continue;
                for (int v = 1; v <= N; v++) {
                    if (!canPlace(r, c, v)) continue;
                    Move m = eval.evaluate(r, c, v);
                    if (m != null) bestMoveHolder = m;
                }
            }
        }
    }

    interface MoveEvaluator {
        Move evaluate(int r, int c, int v);
    }

    // ================= GAME LOGIC =================
    private boolean canPlace(int r, int c, int v) {
        return !rowUsed[r][v] && !colUsed[c][v];
    }

    private void placeValue(int r, int c, int v, boolean human) {
        grid[r][c] = v;
        rowUsed[r][v] = true;
        colUsed[c][v] = true;

        buttons[r][c].setText(String.valueOf(v));
        buttons[r][c].setEnabled(false);

        if (human) humanScore += estimateScore(r, c);
        else cpuScore += estimateScore(r, c);

        updateLabels();

        if (countAllValidMoves() == 0) endGame();
    }

    private int estimateScore(int r, int c) {
        return 10; // simple, deterministic scoring
    }

    private int countAllValidMoves() {
        int count = 0;
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                if (grid[r][c] == 0)
                    for (int v = 1; v <= N; v++)
                        if (canPlace(r, c, v)) count++;
        return count;
    }

    private void simulate(int r, int c, int v) {
        grid[r][c] = v;
        rowUsed[r][v] = colUsed[c][v] = true;
    }

    private void rollback(int r, int c, int v) {
        grid[r][c] = 0;
        rowUsed[r][v] = colUsed[c][v] = false;
    }

    // ================= HEAT MAP =================
    private void updateHeatMap() {
        if (gameOver || humanTurn) return;

        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                JButton btn = buttons[r][c];
                if (grid[r][c] != 0) continue;

                int heat = evaluateHeat(r, c);
                btn.setBackground(heatColor(heat));
            }
        }
    }

    private int evaluateHeat(int r, int c) {
        int best = Integer.MIN_VALUE;

        for (int v = 1; v <= N; v++) {
            if (!canPlace(r, c, v)) continue;
            simulate(r, c, v);
            int val = (cpuStrategy == CPUStrategy.SCORE_GREEDY)
                    ? estimateScore(r, c)
                    : -countAllValidMoves();
            rollback(r, c, v);
            best = Math.max(best, val);
        }
        return best;
    }

    private Color heatColor(int value) {
        if (value == Integer.MIN_VALUE) return Color.WHITE;
        int intensity = Math.min(255, 100 + value * 10);
        return new Color(255, 255 - intensity, 255 - intensity);
    }

    // ================= GAME END =================
    private void endGame() {
        gameOver = true;
        String winner = (humanScore > cpuScore) ? "Human Wins!"
                : (cpuScore > humanScore) ? "CPU Wins!" : "Draw!";
        statusLabel.setText("Game Over: " + winner);
    }

    private void updateLabels() {
        scoreLabel.setText(
                "Human: " + humanScore + " | CPU: " + cpuScore
        );
        statusLabel.setText(humanTurn ? "Human Turn" : "CPU Thinking...");
    }

    // ================= MAIN =================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(GreedyTowersGame::new);
    }
}
