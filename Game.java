package chess;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Game {
    private Board board;
    private PieceColor currentTurn;

    private int lastFromRow = -1;
    private int lastFromCol = -1;
    private int lastToRow = -1;
    private int lastToCol = -1;
    private Piece lastMovedPiece = null;
    private int halfMoveClock = 0;
    private Map<String, Integer> positionCounts = new HashMap<>();

    boolean gameOver;

    public Game() {
        board = new Board();
        currentTurn = PieceColor.WHITE;
        this.gameOver = false;
        recordCurrentPosition();
    }

    public void start() {
        Scanner scanner = new Scanner(System.in);

        board.showBoard();

        while (!gameOver) {
            System.out.println("Turn: " + currentTurn);
            System.out.print("Enter move (e.g. E2 E4), or type resign/draw: ");

            if (!scanner.hasNext()) {
                break;
            }
            String firstInput = scanner.next();

            if (firstInput.equalsIgnoreCase("resign")) {
                resignCurrentPlayer();
                break;
            }

            if (firstInput.equalsIgnoreCase("draw")) {
                System.out.print(currentTurn + " offers a draw. Does the other player accept? (yes/no): ");

                if (!scanner.hasNext()) {
                    break;
                }
                String response = scanner.next();

                if (response.equalsIgnoreCase("yes")) {
                    acceptDraw();
                    break;
                } else {
                    System.out.println("Draw offer declined.");
                    continue;
                }
            }

            if (!scanner.hasNext()) {
                break;
            }
            String secondInput = scanner.next();

            int[] fromPos = parseSquare(firstInput.toUpperCase());
            int[] toPos = parseSquare(secondInput.toUpperCase());

            if (fromPos == null || toPos == null) {
                System.out.println("Invalid input. Use format like E2 E4, or type resign/draw");
                continue;
            }

            PieceType promotionChoice = null;
            Piece movingPiece = board.getPiece(fromPos[0], fromPos[1]);
            if (movingPiece != null && movingPiece.getType() == PieceType.PAWN && reachesPromotionRow(movingPiece, toPos[0])) {
                promotionChoice = askPromotionChoice(scanner);
            }

            makeMove(fromPos[0], fromPos[1], toPos[0], toPos[1], promotionChoice);
        }

        scanner.close();
    }

    public void switchTurn() {
        if (currentTurn == PieceColor.WHITE) {
            currentTurn = PieceColor.BLACK;
        } else {
            currentTurn = PieceColor.WHITE;
        }
    }

    public void makeMove(int fromRow, int fromCol, int toRow, int toCol) {
        makeMove(fromRow, fromCol, toRow, toCol, PieceType.QUEEN);
    }

    public void makeMove(int fromRow, int fromCol, int toRow, int toCol, PieceType promotionType) {

        if (gameOver) {
            System.out.println("Game is over...");
            return;
        }

        if (!board.isInsideBoard(fromRow, fromCol) || !board.isInsideBoard(toRow, toCol)) {
            System.out.println("Out of bounds...");
            return;
        }

        if (board.isEmpty(fromRow, fromCol)) {
            System.out.println("No piece there...");
            return;
        }

        Piece piece = board.getPiece(fromRow, fromCol);

        if (currentTurn != piece.getColor()) {
            System.out.println("Not your turn...");
            return;
        }

        if (!isValidMove(piece, fromRow, fromCol, toRow, toCol)) {
            System.out.println("Invalid move...");
            return;
        }

        if (leavesKingInCheck(piece, fromRow, fromCol, toRow, toCol)) {
            System.out.println("You cannot leave your king in check...");
            return;
        }

        Piece targetPiece = board.getPiece(toRow, toCol);
        boolean isCapture = targetPiece != null;
        boolean isEnPassantMove = isValidEnPassant(piece, fromRow, fromCol, toRow, toCol);

        if (isCastlingMove(piece, fromRow, fromCol, toRow, toCol)) {
            performCastling(fromRow, fromCol, toRow, toCol);

        } else if (isEnPassantMove) {
            isCapture = true;
            performEnPassant(fromRow, fromCol, toRow, toCol);

            if (shouldPromote(piece, toRow)) {
                promotePawn(toRow, toCol, promotionType);
            }

        } else {
            board.movePiece(fromRow, fromCol, toRow, toCol);
            piece.setHasMoved(true);

            if (shouldPromote(piece, toRow)) {
                promotePawn(toRow, toCol, promotionType);
            }
        }

        updateHalfMoveClock(piece, isCapture);
        recordLastMove(piece, fromRow, fromCol, toRow, toCol);

        switchTurn();
        recordCurrentPosition();
        board.showBoard();

        if (isFiftyMoveDraw()) {
            System.out.println("DRAW! 50-move rule.");
            gameOver = true;
            return;
        }

        if (isThreefoldRepetition()) {
            System.out.println("DRAW! Threefold repetition.");
            gameOver = true;
            return;
        }

        if (isInsufficientMaterial()) {
            System.out.println("Draw by insufficient material!");
            gameOver = true;
            return;
            }

        if (isCheckmate(currentTurn)) {
            System.out.println("CHECKMATE! " + currentTurn + " loses.");
            gameOver = true;
            return;
        }

        if (isStalemate(currentTurn)) {
            System.out.println("STALEMATE! Draw.");
            gameOver = true;
            return;
        }

        if (isInCheck(currentTurn)) {
            System.out.println(currentTurn + " is in check!");
        }
    }

    public Board getBoard() {
        return board;
    }

    public PieceColor getCurrentTurn() {
        return currentTurn;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void resignCurrentPlayer() {
        if (gameOver) {
            System.out.println("Game is over...");
            return;
        }

        PieceColor winner = (currentTurn == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;
        System.out.println(currentTurn + " resigns.");
        System.out.println(winner + " wins!");
        gameOver = true;
    }

    public void acceptDraw() {
        if (gameOver) {
            System.out.println("Game is over...");
            return;
        }

        System.out.println("Game drawn by agreement.");
        gameOver = true;
    }

    private boolean isValidKnightMove(Piece piece, int fromRow, int fromCol, int toRow, int toCol) {
        Piece target = board.getPiece(toRow, toCol);

        if (fromRow == toRow && fromCol == toCol)
            return false;

        if (target != null && target.getColor() == piece.getColor())
            return false;

        int rowDiff = Math.abs(toRow - fromRow);
        int colDiff = Math.abs(toCol - fromCol);

        return (rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2);
    }

    private boolean isValidRookMove(Piece piece, int fromRow, int fromCol, int toRow, int toCol) {
        Piece target = board.getPiece(toRow, toCol);

        if (fromRow == toRow && fromCol == toCol)
            return false;

        if (target != null && target.getColor() == piece.getColor())
            return false;

        if (fromRow != toRow && fromCol != toCol)
            return false;

        return isPathClear(fromRow, fromCol, toRow, toCol);
    }

    private boolean isValidBishopMove(Piece piece, int fromRow, int fromCol, int toRow, int toCol) {
        Piece target = board.getPiece(toRow, toCol);

        if (fromRow == toRow && fromCol == toCol)
            return false;

        if (target != null && target.getColor() == piece.getColor())
            return false;

        if (Math.abs(toRow - fromRow) != Math.abs(toCol - fromCol))
            return false;

        return isPathClear(fromRow, fromCol, toRow, toCol);
    }

    private boolean isValidQueenMove(Piece piece, int fromRow, int fromCol, int toRow, int toCol) {
        return isValidRookMove(piece, fromRow, fromCol, toRow, toCol) ||
                isValidBishopMove(piece, fromRow, fromCol, toRow, toCol);
    }

    private boolean isValidKingMove(Piece piece, int fromRow, int fromCol, int toRow, int toCol) {
        if (fromRow == toRow && fromCol == toCol)
            return false;

        Piece target = board.getPiece(toRow, toCol);

        if (target != null && target.getColor() == piece.getColor())
            return false;

        int rowDiff = Math.abs(toRow - fromRow);
        int colDiff = Math.abs(toCol - fromCol);

        if (rowDiff <= 1 && colDiff <= 1) {
            if (target != null && target.getType() == PieceType.KING) {
                return false;
            }

            return !isSquareUnderAttack(toRow, toCol, piece.getColor());
        }

        return isValidCastling(piece, fromRow, fromCol, toRow, toCol);
    }

    private boolean isValidPawnMove(Piece piece, int fromRow, int fromCol, int toRow, int toCol) {

        if (fromRow == toRow && fromCol == toCol)
            return false;

        Piece target = board.getPiece(toRow, toCol);

        if (target != null && target.getColor() == piece.getColor())
            return false;

        int direction;
        int startRow;

        if (piece.getColor() == PieceColor.WHITE) {
            direction = 1;
            startRow = 1;
        } else {
            direction = -1;
            startRow = 6;
        }

        if (toCol == fromCol && toRow == fromRow + direction && target == null) {
            return true;
        }

        if (toCol == fromCol &&
                fromRow == startRow &&
                toRow == fromRow + 2 * direction &&
                target == null &&
                board.isEmpty(fromRow + direction, fromCol)) {
            return true;
        }

        if (Math.abs(toCol - fromCol) == 1 &&
                toRow == fromRow + direction &&
                target != null &&
                target.getColor() != piece.getColor()) {
            return true;
        }

        return isValidEnPassant(piece, fromRow, fromCol, toRow, toCol);
    }

    private boolean isValidMove(Piece piece, int fromRow, int fromCol, int toRow, int toCol) {
        switch (piece.getType()) {
            case KNIGHT:
                return isValidKnightMove(piece, fromRow, fromCol, toRow, toCol);
            case ROOK:
                return isValidRookMove(piece, fromRow, fromCol, toRow, toCol);
            case BISHOP:
                return isValidBishopMove(piece, fromRow, fromCol, toRow, toCol);
            case QUEEN:
                return isValidQueenMove(piece, fromRow, fromCol, toRow, toCol);
            case KING:
                return isValidKingMove(piece, fromRow, fromCol, toRow, toCol);
            case PAWN:
                return isValidPawnMove(piece, fromRow, fromCol, toRow, toCol);
            default:
                return false;
        }
    }

    private boolean isPathClear(int fromRow, int fromCol, int toRow, int toCol) {
        int rowStep = Integer.compare(toRow, fromRow);
        int colStep = Integer.compare(toCol, fromCol);

        int currentRow = fromRow + rowStep;
        int currentCol = fromCol + colStep;

        while (currentRow != toRow || currentCol != toCol) {
            if (!board.isEmpty(currentRow, currentCol)) {
                return false;
            }

            currentRow += rowStep;
            currentCol += colStep;
        }

        return true;
    }

    private int[] parseSquare(String square) {
        if (square == null || square.length() != 2) {
            return null;
        }

        char file = square.charAt(0);
        char rank = square.charAt(1);

        if (file < 'A' || file > 'H' || rank < '1' || rank > '8') {
            return null;
        }

        int col = file - 'A';
        int row = rank - '1';

        return new int[]{row, col};
    }

    private int[] findKing(PieceColor color) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board.getPiece(row, col);

                if (piece != null &&
                        piece.getType() == PieceType.KING &&
                        piece.getColor() == color) {
                    return new int[]{row, col};
                }
            }
        }

        return null;
    }

    private boolean isSquareUnderAttack(int row, int col, PieceColor kingColor) {
        PieceColor enemyColor = (kingColor == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = board.getPiece(r, c);

                if (piece == null || piece.getColor() != enemyColor) {
                    continue;
                }

                switch (piece.getType()) {
                    case PAWN:
                        int direction = (piece.getColor() == PieceColor.WHITE) ? 1 : -1;
                        if (r + direction == row && Math.abs(c - col) == 1) {
                            return true;
                        }
                        break;

                    case KING:
                        if (Math.abs(r - row) <= 1 && Math.abs(c - col) <= 1) {
                            return true;
                        }
                        break;

                    default:
                        if (isValidMove(piece, r, c, row, col)) {
                            return true;
                        }
                        break;
                }
            }
        }

        return false;
    }

    private boolean isInCheck(PieceColor color) {
        int[] kingPos = findKing(color);

        if (kingPos == null) {
            throw new IllegalStateException("King not found for " + color);
        }

        return isSquareUnderAttack(kingPos[0], kingPos[1], color);
    }

    private boolean leavesKingInCheck(Piece piece, int fromRow, int fromCol, int toRow, int toCol) {
        Piece capturedPiece = board.getPiece(toRow, toCol);
        Piece enPassantCaptured = null;
        boolean enPassant = isValidEnPassant(piece, fromRow, fromCol, toRow, toCol);
        boolean castling = isCastlingMove(piece, fromRow, fromCol, toRow, toCol);

        if (castling) {
            int rookFromCol = (toCol > fromCol) ? 7 : 0;
            int rookToCol = (toCol > fromCol) ? 5 : 3;

            board.movePiece(fromRow, fromCol, toRow, toCol);
            board.movePiece(fromRow, rookFromCol, fromRow, rookToCol);

            boolean kingInCheck = isInCheck(piece.getColor());

            board.movePiece(toRow, toCol, fromRow, fromCol);
            board.movePiece(fromRow, rookToCol, fromRow, rookFromCol);

            return kingInCheck;
        }

        if (enPassant) {
            enPassantCaptured = board.getPiece(fromRow, toCol);
            board.movePiece(fromRow, fromCol, toRow, toCol);
            board.setPiece(fromRow, toCol, null);
        } else {
            board.movePiece(fromRow, fromCol, toRow, toCol);
        }

        boolean kingInCheck = isInCheck(piece.getColor());

        board.movePiece(toRow, toCol, fromRow, fromCol);
        board.setPiece(toRow, toCol, capturedPiece);

        if (enPassant) {
            board.setPiece(fromRow, toCol, enPassantCaptured);
        }

        return kingInCheck;
    }

    private boolean hasAnyLegalMove(PieceColor color) {
        for (int fromRow = 0; fromRow < 8; fromRow++) {
            for (int fromCol = 0; fromCol < 8; fromCol++) {
                Piece piece = board.getPiece(fromRow, fromCol);

                if (piece == null || piece.getColor() != color) {
                    continue;
                }

                for (int toRow = 0; toRow < 8; toRow++) {
                    for (int toCol = 0; toCol < 8; toCol++) {

                        if (!isValidMove(piece, fromRow, fromCol, toRow, toCol)) {
                            continue;
                        }

                        if (leavesKingInCheck(piece, fromRow, fromCol, toRow, toCol)) {
                            continue;
                        }

                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isCheckmate(PieceColor color) {
        return isInCheck(color) && !hasAnyLegalMove(color);
    }

    private boolean isStalemate(PieceColor color) {
        return !isInCheck(color) && !hasAnyLegalMove(color);
    }

    private boolean isCastlingMove(Piece piece, int fromRow, int fromCol, int toRow, int toCol) {
        return piece.getType() == PieceType.KING &&
                fromRow == toRow &&
                Math.abs(toCol - fromCol) == 2;
    }

    private boolean isValidCastling(Piece king, int fromRow, int fromCol, int toRow, int toCol) {

        if (fromRow != toRow || Math.abs(toCol - fromCol) != 2) {
            return false;
        }

        if (king == null || king.getType() != PieceType.KING) {
            return false;
        }

        if (king.hasMoved()) {
            return false;
        }

        if (isInCheck(king.getColor())) {
            return false;
        }

        int rookCol;
        int step;

        if (toCol > fromCol) {
            rookCol = 7;
            step = 1;
        } else {
            rookCol = 0;
            step = -1;
        }

        Piece rook = board.getPiece(fromRow, rookCol);

        if (rook == null || rook.getType() != PieceType.ROOK || rook.getColor() != king.getColor()) {
            return false;
        }

        if (rook.hasMoved()) {
            return false;
        }

        int currentCol = fromCol + step;
        while (currentCol != rookCol) {
            if (!board.isEmpty(fromRow, currentCol)) {
                return false;
            }
            currentCol += step;
        }

        for (int col = fromCol + step; col != toCol + step; col += step) {
            if (isSquareUnderAttack(fromRow, col, king.getColor())) {
                return false;
            }
        }

        return true;
    }

    private void performCastling(int fromRow, int fromCol, int toRow, int toCol) {
        Piece king = board.getPiece(fromRow, fromCol);

        board.movePiece(fromRow, fromCol, toRow, toCol);
        king.setHasMoved(true);

        if (toCol > fromCol) {
            Piece rook = board.getPiece(toRow, 7);
            board.movePiece(toRow, 7, toRow, 5);
            rook.setHasMoved(true);
        } else {
            Piece rook = board.getPiece(toRow, 0);
            board.movePiece(toRow, 0, toRow, 3);
            rook.setHasMoved(true);
        }
    }

    private boolean shouldPromote(Piece piece, int row) {
        return piece.getType() == PieceType.PAWN && reachesPromotionRow(piece, row);
    }

    private boolean reachesPromotionRow(Piece piece, int row) {
        if (piece.getColor() == PieceColor.WHITE) {
            return row == 7;
        }
        return row == 0;
    }

    private PieceType askPromotionChoice(Scanner scanner) {
        System.out.print("Promote pawn to (Q, R, B, N): ");
        if (!scanner.hasNext()) {
            return PieceType.QUEEN;
        }
        return parsePromotionChoice(scanner.next());
    }

    private PieceType parsePromotionChoice(String choice) {
        if (choice == null) {
            return PieceType.QUEEN;
        }

        switch (choice.trim().toUpperCase()) {
            case "R":
                return PieceType.ROOK;
            case "B":
                return PieceType.BISHOP;
            case "N":
                return PieceType.KNIGHT;
            case "Q":
            default:
                return PieceType.QUEEN;
        }
    }

    private void promotePawn(int row, int col, PieceType promotionType) {
        Piece pawn = board.getPiece(row, col);

        if (pawn == null || pawn.getType() != PieceType.PAWN) {
            return;
        }

        PieceType finalType = (promotionType == null) ? PieceType.QUEEN : promotionType;
        Piece promotedPiece = new Piece(finalType, pawn.getColor());
        promotedPiece.setHasMoved(true);

        board.setPiece(row, col, promotedPiece);
    }

    private void updateHalfMoveClock(Piece piece, boolean isCapture) {
        if (piece.getType() == PieceType.PAWN || isCapture) {
            halfMoveClock = 0;
        } else {
            halfMoveClock++;
        }
    }

    private boolean isFiftyMoveDraw() {
        return halfMoveClock >= 100;
    }

    private void recordLastMove(Piece piece, int fromRow, int fromCol, int toRow, int toCol) {
        lastMovedPiece = piece;
        lastFromRow = fromRow;
        lastFromCol = fromCol;
        lastToRow = toRow;
        lastToCol = toCol;
    }

    private void recordCurrentPosition() {
        String key = getPositionKey();
        positionCounts.put(key, positionCounts.getOrDefault(key, 0) + 1);
    }

    private boolean isThreefoldRepetition() {
        String key = getPositionKey();
        return positionCounts.getOrDefault(key, 0) >= 3;
    }

    private String getPositionKey() {
        StringBuilder sb = new StringBuilder();
        sb.append(getBoardState());
        sb.append("|turn=").append(currentTurn);
        sb.append("|castling=").append(getCastlingRights());
        sb.append("|enpassant=").append(getEnPassantTarget());
        return sb.toString();
    }

    private String getBoardState() {
        StringBuilder sb = new StringBuilder();

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board.getPiece(row, col);
                if (piece == null) {
                    sb.append('.');
                } else {
                    sb.append(piece.getSymbol());
                }
            }
            sb.append('/');
        }

        return sb.toString();
    }

    private String getCastlingRights() {
        StringBuilder rights = new StringBuilder();

        Piece whiteKing = board.getPiece(0, 4);
        Piece whiteKingsideRook = board.getPiece(0, 7);
        Piece whiteQueensideRook = board.getPiece(0, 0);

        Piece blackKing = board.getPiece(7, 4);
        Piece blackKingsideRook = board.getPiece(7, 7);
        Piece blackQueensideRook = board.getPiece(7, 0);

        if (whiteKing != null && whiteKing.getType() == PieceType.KING &&
                whiteKing.getColor() == PieceColor.WHITE && !whiteKing.hasMoved()) {

            if (whiteKingsideRook != null && whiteKingsideRook.getType() == PieceType.ROOK &&
                    whiteKingsideRook.getColor() == PieceColor.WHITE && !whiteKingsideRook.hasMoved()) {
                rights.append("K");
            }

            if (whiteQueensideRook != null && whiteQueensideRook.getType() == PieceType.ROOK &&
                    whiteQueensideRook.getColor() == PieceColor.WHITE && !whiteQueensideRook.hasMoved()) {
                rights.append("Q");
            }
        }

        if (blackKing != null && blackKing.getType() == PieceType.KING &&
                blackKing.getColor() == PieceColor.BLACK && !blackKing.hasMoved()) {

            if (blackKingsideRook != null && blackKingsideRook.getType() == PieceType.ROOK &&
                    blackKingsideRook.getColor() == PieceColor.BLACK && !blackKingsideRook.hasMoved()) {
                rights.append("k");
            }

            if (blackQueensideRook != null && blackQueensideRook.getType() == PieceType.ROOK &&
                    blackQueensideRook.getColor() == PieceColor.BLACK && !blackQueensideRook.hasMoved()) {
                rights.append("q");
            }
        }

        if (rights.length() == 0) {
            return "-";
        }

        return rights.toString();
    }

    private String getEnPassantTarget() {
        if (lastMovedPiece == null || lastMovedPiece.getType() != PieceType.PAWN) {
            return "-";
        }

        if (Math.abs(lastToRow - lastFromRow) != 2) {
            return "-";
        }

        int targetRow = (lastFromRow + lastToRow) / 2;
        int targetCol = lastToCol;

        char file = (char) ('A' + targetCol);
        char rank = (char) ('1' + targetRow);

        return "" + file + rank;
    }

    private boolean isValidEnPassant(Piece piece, int fromRow, int fromCol, int toRow, int toCol) {
        if (piece.getType() != PieceType.PAWN) {
            return false;
        }

        if (lastMovedPiece == null || lastMovedPiece.getType() != PieceType.PAWN) {
            return false;
        }

        if (lastMovedPiece.getColor() == piece.getColor()) {
            return false;
        }

        if (Math.abs(lastToRow - lastFromRow) != 2) {
            return false;
        }

        int direction = (piece.getColor() == PieceColor.WHITE) ? 1 : -1;

        if (toRow != fromRow + direction || Math.abs(toCol - fromCol) != 1) {
            return false;
        }

        if (!board.isEmpty(toRow, toCol)) {
            return false;
        }

        return lastToRow == fromRow && lastToCol == toCol;
    }

    private void performEnPassant(int fromRow, int fromCol, int toRow, int toCol) {
        Piece pawn = board.getPiece(fromRow, fromCol);

        board.movePiece(fromRow, fromCol, toRow, toCol);
        board.setPiece(fromRow, toCol, null);

        pawn.setHasMoved(true);
    }
    
    public boolean isLegalMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (gameOver) {
            return false;
        }

        if (!board.isInsideBoard(fromRow, fromCol) || !board.isInsideBoard(toRow, toCol)) {
            return false;
        }

        if (board.isEmpty(fromRow, fromCol)) {
            return false;
        }

        Piece piece = board.getPiece(fromRow, fromCol);

        if (piece.getColor() != currentTurn) {
            return false;
        }

        if (!isValidMove(piece, fromRow, fromCol, toRow, toCol)) {
            return false;
        }

        if (leavesKingInCheck(piece, fromRow, fromCol, toRow, toCol)) {
            return false;
        }

        return true;
    }

    private boolean isInsufficientMaterial() {
    int whiteBishops = 0, blackBishops = 0;
    int whiteKnights = 0, blackKnights = 0;
    int whiteOther = 0, blackOther = 0;

    boolean whiteBishopOnLight = false;
    boolean whiteBishopOnDark = false;
    boolean blackBishopOnLight = false;
    boolean blackBishopOnDark = false;

    for (int row = 0; row < 8; row++) {
        for (int col = 0; col < 8; col++) {
            Piece piece = board.getPiece(row, col);
            if (piece == null) continue;

            PieceColor color = piece.getColor();

            switch (piece.getType()) {
                case KING:
                    break;

                case BISHOP:
                    if (color == PieceColor.WHITE) {
                        whiteBishops++;
                        if ((row + col) % 2 == 0) {
                            whiteBishopOnLight = true;
                        } else {
                            whiteBishopOnDark = true;
                        }
                    } else {
                        blackBishops++;
                        if ((row + col) % 2 == 0) {
                            blackBishopOnLight = true;
                        } else {
                            blackBishopOnDark = true;
                        }
                    }
                    break;

                case KNIGHT:
                    if (color == PieceColor.WHITE) {
                        whiteKnights++;
                    } else {
                        blackKnights++;
                    }
                    break;

                default:
                    if (color == PieceColor.WHITE) {
                        whiteOther++;
                    } else {
                        blackOther++;
                    }
                    break;
            }
        }
    }

    // any pawn, rook, or queen means enough material exists
    if (whiteOther > 0 || blackOther > 0) {
        return false;
    }

    // king vs king
    if (whiteBishops == 0 && whiteKnights == 0 && blackBishops == 0 && blackKnights == 0) {
        return true;
    }

    // king + bishop vs king
    if (whiteBishops == 1 && whiteKnights == 0 && blackBishops == 0 && blackKnights == 0) {
        return true;
    }
    if (blackBishops == 1 && blackKnights == 0 && whiteBishops == 0 && whiteKnights == 0) {
        return true;
    }

    // king + knight vs king
    if (whiteKnights == 1 && whiteBishops == 0 && blackBishops == 0 && blackKnights == 0) {
        return true;
    }
    if (blackKnights == 1 && blackBishops == 0 && whiteBishops == 0 && whiteKnights == 0) {
        return true;
    }

    // king + bishop vs king + bishop, same colored bishops
    if (whiteBishops == 1 && blackBishops == 1 &&
        whiteKnights == 0 && blackKnights == 0) {

        boolean bothLight = whiteBishopOnLight && blackBishopOnLight;
        boolean bothDark = whiteBishopOnDark && blackBishopOnDark;

        if (bothLight || bothDark) {
            return true;
        }
    }

    return false;
}
}
