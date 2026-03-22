package chess;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ChessGUI extends JFrame {
    private Game game;
    private final JButton[][] squares;
    private final JLabel statusLabel;
    private final JLabel selectionLabel;
    private Integer selectedRow;
    private Integer selectedCol;
    private final Map<String, ImageIcon> pieceIcons;

    public ChessGUI() {
        this.game = new Game();
        this.squares = new JButton[8][8];
        this.statusLabel = new JLabel("WHITE to move");
        this.selectionLabel = new JLabel("Select a piece.");
        this.pieceIcons = new HashMap<>();

        setTitle("Java Chess");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        selectionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        topPanel.add(statusLabel);
        topPanel.add(selectionLabel);
        add(topPanel, BorderLayout.NORTH);

        JPanel boardPanel = new JPanel(new GridLayout(8, 8));
        boardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (int row = 7; row >= 0; row--) {
            for (int col = 0; col < 8; col++) {
                JButton button = new JButton();
                button.setFocusPainted(false);
                button.setContentAreaFilled(true);
                button.setHorizontalAlignment(SwingConstants.CENTER);
                button.setVerticalAlignment(SwingConstants.CENTER);
                button.setOpaque(true);
                button.setBorder(new LineBorder(Color.DARK_GRAY));

                final int currentRow = row;
                final int currentCol = col;
                button.addActionListener(e -> handleSquareClick(currentRow, currentCol));

                squares[row][col] = button;
                boardPanel.add(button);
            }
        }

        add(boardPanel, BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton resignButton = new JButton("Resign");
        JButton drawButton = new JButton("Offer Draw");
        JButton newGameButton = new JButton("New Game");

        resignButton.addActionListener(e -> resignCurrentPlayer());
        drawButton.addActionListener(e -> offerDraw());
        newGameButton.addActionListener(e -> startNewGame());

        controls.add(resignButton);
        controls.add(drawButton);
        controls.add(newGameButton);
        add(controls, BorderLayout.SOUTH);

        refreshBoard();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void handleSquareClick(int row, int col) {
        if (game.isGameOver()) {
            JOptionPane.showMessageDialog(this, "The game is over. Start a new game to play again.");
            return;
        }

        Board board = game.getBoard();
        Piece clickedPiece = board.getPiece(row, col);

        if (selectedRow == null || selectedCol == null) {
            if (clickedPiece == null) {
                selectionLabel.setText("Select one of your pieces.");
                return;
            }

            if (clickedPiece.getColor() != game.getCurrentTurn()) {
                selectionLabel.setText("It is " + game.getCurrentTurn() + "'s turn.");
                return;
            }

            selectedRow = row;
            selectedCol = col;
            selectionLabel.setText("Selected " + squareName(row, col) + ". Choose destination.");
            refreshBoard();
            return;
        }

        if (selectedRow == row && selectedCol == col) {
            clearSelection();
            selectionLabel.setText("Selection cleared.");
            refreshBoard();
            return;
        }

        Piece movingPiece = board.getPiece(selectedRow, selectedCol);
        if (movingPiece == null) {
            clearSelection();
            selectionLabel.setText("Selection lost. Select a piece again.");
            refreshBoard();
            return;
        }

        if (!game.isLegalMove(selectedRow, selectedCol, row, col)) {
            selectionLabel.setText("Illegal move. Try again.");
            refreshBoard();
            return;
        }

        PieceType promotionType = null;
        if (movingPiece.getType() == PieceType.PAWN && reachesPromotionRow(movingPiece, row)) {
            promotionType = askPromotionChoice();
            if (promotionType == null) {
                selectionLabel.setText("Promotion cancelled.");
                return;
            }
        }

        PieceColor turnBeforeMove = game.getCurrentTurn();
        game.makeMove(selectedRow, selectedCol, row, col, promotionType);
        PieceColor turnAfterMove = game.getCurrentTurn();

        boolean moveSucceeded = turnBeforeMove != turnAfterMove || game.isGameOver();
        if (!moveSucceeded) {
            selectionLabel.setText("Illegal move. Try again.");
        } else {
            clearSelection();
            selectionLabel.setText(game.isGameOver() ? "Game over." : "Move played.");
        }

        refreshBoard();

        if (game.isGameOver()) {
            showGameOverMessage();
        }
    }

    private void refreshBoard() {
        Board board = game.getBoard();

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                JButton button = squares[row][col];
                Piece piece = board.getPiece(row, col);

                button.setText("");
                button.setIcon(piece == null ? null : getPieceIcon(piece));
                button.setDisabledIcon(button.getIcon());

                Color baseColor = ((row + col) % 2 == 0)
                        ? new Color(240, 217, 181)
                        : new Color(181, 136, 99);

                if (selectedRow != null && selectedCol != null && selectedRow == row && selectedCol == col) {
                    baseColor = new Color(246, 246, 105);
                }

                button.setBackground(baseColor);
            }
        }

        String status = game.isGameOver() ? "Game over" : game.getCurrentTurn() + " to move";
        statusLabel.setText(status);
    }

    private PieceType askPromotionChoice() {
        Object[] choices = {"Queen", "Rook", "Bishop", "Knight"};
        Object choice = JOptionPane.showInputDialog(
                this,
                "Promote pawn to:",
                "Pawn Promotion",
                JOptionPane.PLAIN_MESSAGE,
                null,
                choices,
                choices[0]
        );

        if (choice == null) {
            return null;
        }

        switch (choice.toString()) {
            case "Rook":
                return PieceType.ROOK;
            case "Bishop":
                return PieceType.BISHOP;
            case "Knight":
                return PieceType.KNIGHT;
            default:
                return PieceType.QUEEN;
        }
    }

    private boolean reachesPromotionRow(Piece piece, int toRow) {
        return piece.getColor() == PieceColor.WHITE ? toRow == 7 : toRow == 0;
    }

    private void resignCurrentPlayer() {
        if (game.isGameOver()) {
            JOptionPane.showMessageDialog(this, "The game is already over.");
            return;
        }

        int result = JOptionPane.showConfirmDialog(
                this,
                game.getCurrentTurn() + " resigns?",
                "Resign",
                JOptionPane.YES_NO_OPTION
        );

        if (result == JOptionPane.YES_OPTION) {
            game.resignCurrentPlayer();
            clearSelection();
            refreshBoard();
            showGameOverMessage();
        }
    }

    private void offerDraw() {
        if (game.isGameOver()) {
            JOptionPane.showMessageDialog(this, "The game is already over.");
            return;
        }

        PieceColor offeringPlayer = game.getCurrentTurn();
        PieceColor otherPlayer = offeringPlayer == PieceColor.WHITE ? PieceColor.BLACK : PieceColor.WHITE;

        int result = JOptionPane.showConfirmDialog(
                this,
                offeringPlayer + " offers a draw. Does " + otherPlayer + " accept?",
                "Draw Offer",
                JOptionPane.YES_NO_OPTION
        );

        if (result == JOptionPane.YES_OPTION) {
            game.acceptDraw();
            clearSelection();
            refreshBoard();
            showGameOverMessage();
        } else {
            selectionLabel.setText("Draw offer declined.");
        }
    }

    private void startNewGame() {
        this.game = new Game();
        clearSelection();
        selectionLabel.setText("Select a piece.");
        refreshBoard();
    }

    private void showGameOverMessage() {
        String message = "Game over.";
        if (!game.isGameOver()) {
            return;
        }

        PieceColor nextTurn = game.getCurrentTurn();
        if (nextTurn == PieceColor.WHITE || nextTurn == PieceColor.BLACK) {
            message = "Game over.";
        }

        JOptionPane.showMessageDialog(this, message);
    }


    private ImageIcon getPieceIcon(Piece piece) {
        String key = getPieceImageName(piece);
        if (pieceIcons.containsKey(key)) {
            return pieceIcons.get(key);
        }

        ImageIcon icon = loadPieceIcon(key);
        pieceIcons.put(key, icon);
        return icon;
    }

    private String getPieceImageName(Piece piece) {
        String color = piece.getColor() == PieceColor.WHITE ? "white" : "black";
        String type;

        switch (piece.getType()) {
            case KING:
                type = "king";
                break;
            case QUEEN:
                type = "queen";
                break;
            case ROOK:
                type = "rook";
                break;
            case BISHOP:
                type = "bishop";
                break;
            case KNIGHT:
                type = "knight";
                break;
            default:
                type = "pawn";
                break;
        }

        return color + "_" + type;
    }

    private ImageIcon loadPieceIcon(String name) {
        String path = "pieces/" + name + ".png";
        ImageIcon baseIcon = new ImageIcon(path);

        if (baseIcon.getIconWidth() <= 0) {
            return createFallbackIcon();
        }

        Image scaled = baseIcon.getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private ImageIcon createFallbackIcon() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        return new ImageIcon(image);
    }

    private void clearSelection() {
        selectedRow = null;
        selectedCol = null;
    }

    private String squareName(int row, int col) {
        char file = (char) ('A' + col);
        char rank = (char) ('1' + row);
        return "" + file + rank;
    }
}
