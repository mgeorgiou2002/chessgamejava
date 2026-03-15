package chess;

import java.util.Scanner;

public class Game {
	private Board board; 
	private PieceColor currentTurn;
	
	private int lastFromRow = -1;
	private int lastFromCol = -1;
	private int lastToRow = -1;
	private int lastToCol = -1;
	private Piece lastMovedPiece = null;

	boolean gameOver;
	
	public Game() {
		board = new Board();
		currentTurn = PieceColor.WHITE;
		this.gameOver = false;
	}
	
	public void start() {
	    Scanner scanner = new Scanner(System.in);

	    board.showBoard();

	    while (!gameOver) {
	        System.out.println("Turn: " + currentTurn);
	        System.out.print("Enter move (e.g. E2 E4): ");

	        if (!scanner.hasNext()) break;
	        String from = scanner.next();

	        if (!scanner.hasNext()) break;
	        String to = scanner.next();

	        int[] fromPos = parseSquare(from.toUpperCase());
	        int[] toPos = parseSquare(to.toUpperCase());

	        if (fromPos == null || toPos == null) {
	            System.out.println("Invalid input. Use format like E2 E4");
	            continue;
	        }

	        makeMove(fromPos[0], fromPos[1], toPos[0], toPos[1]);
	    }

	    scanner.close();
	}
	
	public void switchTurn() {
		if(currentTurn == PieceColor.WHITE) {
		currentTurn = PieceColor.BLACK;}
		else {
			currentTurn = PieceColor.WHITE;}
	}
	
	
	
	public void makeMove(int fromRow, int fromCol, int toRow, int toCol) {

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

    if (isCastlingMove(piece, fromRow, fromCol, toRow, toCol)) {
        performCastling(fromRow, fromCol, toRow, toCol);

    } else if (isValidEnPassant(piece, fromRow, fromCol, toRow, toCol)) {
        performEnPassant(fromRow, fromCol, toRow, toCol);

        if (shouldPromote(piece, toRow)) {
            promotePawn(toRow, toCol);
        }

    } else {
        board.movePiece(fromRow, fromCol, toRow, toCol);
        piece.setHasMoved(true);

        if (shouldPromote(piece, toRow)) {
            promotePawn(toRow, toCol);
        }
    }

    recordLastMove(piece, fromRow, fromCol, toRow, toCol);

    switchTurn();
    board.showBoard();

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
	
	private boolean isValidKnightMove(Piece piece, int fromRow, int fromCol, int toRow, int toCol){
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

	    // move forward 1
	    if (toCol == fromCol && toRow == fromRow + direction && target == null) {
	        return true;
	    }

	    // move forward 2 from starting row
	    if (toCol == fromCol &&
	        fromRow == startRow &&
	        toRow == fromRow + 2 * direction &&
	        target == null &&
	        board.isEmpty(fromRow + direction, fromCol)) {
	        return true;
	    }

	    // normal diagonal capture
	    if (Math.abs(toCol - fromCol) == 1 &&
	        toRow == fromRow + direction &&
	        target != null &&
	        target.getColor() != piece.getColor()) {
	        return true;
	    }

	    // en passant
	    if (isValidEnPassant(piece, fromRow, fromCol, toRow, toCol)) {
	        return true;
	    }

	    return false;
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

            Piece rook = board.getPiece(fromRow, rookFromCol);

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

        // kingside
        if (toCol > fromCol) {
            rookCol = 7;
            step = 1;
        } 
        // queenside
        else {
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

        // squares between king and rook must be empty
        int currentCol = fromCol + step;
        while (currentCol != rookCol) {
            if (!board.isEmpty(fromRow, currentCol)) {
                return false;
            }
            currentCol += step;
        }

        // king may not pass through attacked squares
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

        // kingside
        if (toCol > fromCol) {
            Piece rook = board.getPiece(toRow, 7);
            board.movePiece(toRow, 7, toRow, 5);
            rook.setHasMoved(true);
        } 
        // queenside
        else {
            Piece rook = board.getPiece(toRow, 0);
            board.movePiece(toRow, 0, toRow, 3);
            rook.setHasMoved(true);
        }
    }
    
    private boolean shouldPromote(Piece piece, int row) {
        if (piece.getType() != PieceType.PAWN) {
            return false;
        }

        if (piece.getColor() == PieceColor.WHITE && row == 7) {
            return true;
        }

        if (piece.getColor() == PieceColor.BLACK && row == 0) {
            return true;
        }

        return false;
    }
    
    private void promotePawn(int row, int col) {
        Piece pawn = board.getPiece(row, col);

        if (pawn == null || pawn.getType() != PieceType.PAWN) {
            return;
        }

        Scanner scanner = new Scanner(System.in);

        System.out.println("Promote pawn to (Q, R, B, N): ");
        String choice = scanner.next().toUpperCase();

        PieceType newType;

        switch (choice) {
            case "R":
                newType = PieceType.ROOK;
                break;
            case "B":
                newType = PieceType.BISHOP;
                break;
            case "N":
                newType = PieceType.KNIGHT;
                break;
            default:
                newType = PieceType.QUEEN;
        }

        Piece promotedPiece = new Piece(newType, pawn.getColor());
        promotedPiece.setHasMoved(true);

        board.setPiece(row, col, promotedPiece);
    }
    
    private void recordLastMove(Piece piece, int fromRow, int fromCol, int toRow, int toCol) {
        lastMovedPiece = piece;
        lastFromRow = fromRow;
        lastFromCol = fromCol;
        lastToRow = toRow;
        lastToCol = toCol;
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

        if (lastToRow != fromRow || lastToCol != toCol) {
            return false;
        }

        return true;
    }
    
    private void performEnPassant(int fromRow, int fromCol, int toRow, int toCol) {
        Piece pawn = board.getPiece(fromRow, fromCol);

        board.movePiece(fromRow, fromCol, toRow, toCol);
        board.setPiece(fromRow, toCol, null);

        pawn.setHasMoved(true);
    }
    
	}
