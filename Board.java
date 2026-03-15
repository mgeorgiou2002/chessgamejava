package chess;

public class Board {

    private Piece[][] board = new Piece[8][8];

    public Board() {
    	setupBoard();
    }

    public void showBoard() {

        // Print column labels
        System.out.print("   ");
        for (int i = 0; i < 8; i++) {
            char ascii = (char) ('A' + i);
            System.out.print(" " + ascii + "  ");
        }
        System.out.println();

        // Print board rows
        for (int i = 0; i < 8; i++) {
            System.out.print((i + 1) + " ");

            for (int j = 0; j < 8; j++) {

                Piece piece = board[i][j];   // <-- get piece from board

                if (piece == null)
                    System.out.print("[ ] ");
                else
                    System.out.print("[" + piece.getSymbol() + "] ");
            }

            System.out.println(" " + (i + 1));
        }

        // Print column labels again
        System.out.print("   ");
        for (int i = 0; i < 8; i++) {
            char ascii = (char) ('A' + i);
            System.out.print(" " + ascii + "  ");
        }
        System.out.println();
    }
    
    public void movePiece(int fromRow, int fromCol, int toRow, int toCol) {
    	
    	if(board[fromRow][fromCol] == null) {
    		System.out.println("No piece in that position...");
    		return;
    	}
    	board[toRow][toCol] = board[fromRow][fromCol];
    	board[fromRow][fromCol] = null;	
    }
    
    public void setupBoard() {
    	board[0][0] = new Piece(PieceType.ROOK, PieceColor.WHITE);
    	board[0][1] = new Piece(PieceType.KNIGHT, PieceColor.WHITE);
    	board[0][2] = new Piece(PieceType.BISHOP, PieceColor.WHITE);
    	board[0][3] = new Piece(PieceType.QUEEN, PieceColor.WHITE);
    	board[0][4] = new Piece(PieceType.KING, PieceColor.WHITE);
    	board[0][5] = new Piece(PieceType.BISHOP, PieceColor.WHITE);
    	board[0][6] = new Piece(PieceType.KNIGHT, PieceColor.WHITE);
    	board[0][7] = new Piece(PieceType.ROOK, PieceColor.WHITE);
    	board[1][0] = new Piece(PieceType.PAWN, PieceColor.WHITE);
    	board[1][1] = new Piece(PieceType.PAWN, PieceColor.WHITE);
    	board[1][2] = new Piece(PieceType.PAWN, PieceColor.WHITE);
    	board[1][3] = new Piece(PieceType.PAWN, PieceColor.WHITE);
    	board[1][4] = new Piece(PieceType.PAWN, PieceColor.WHITE);
    	board[1][5] = new Piece(PieceType.PAWN, PieceColor.WHITE);
    	board[1][6] = new Piece(PieceType.PAWN, PieceColor.WHITE);
    	board[1][7] = new Piece(PieceType.PAWN, PieceColor.WHITE);
    	
    	board[7][0] = new Piece(PieceType.ROOK, PieceColor.BLACK);
    	board[7][1] = new Piece(PieceType.KNIGHT, PieceColor.BLACK);
    	board[7][2] = new Piece(PieceType.BISHOP, PieceColor.BLACK);
    	board[7][3] = new Piece(PieceType.QUEEN, PieceColor.BLACK);
    	board[7][4] = new Piece(PieceType.KING, PieceColor.BLACK);
    	board[7][5] = new Piece(PieceType.BISHOP, PieceColor.BLACK);
    	board[7][6] = new Piece(PieceType.KNIGHT, PieceColor.BLACK);
    	board[7][7] = new Piece(PieceType.ROOK, PieceColor.BLACK);
    	board[6][0] = new Piece(PieceType.PAWN, PieceColor.BLACK);
    	board[6][1] = new Piece(PieceType.PAWN, PieceColor.BLACK);
    	board[6][2] = new Piece(PieceType.PAWN, PieceColor.BLACK);
    	board[6][3] = new Piece(PieceType.PAWN, PieceColor.BLACK);
    	board[6][4] = new Piece(PieceType.PAWN, PieceColor.BLACK);
    	board[6][5] = new Piece(PieceType.PAWN, PieceColor.BLACK);
    	board[6][6] = new Piece(PieceType.PAWN, PieceColor.BLACK);
    	board[6][7] = new Piece(PieceType.PAWN, PieceColor.BLACK);	
    }
    
    public boolean isEmpty(int row, int col) {
    	if(board[row][col] == null)
    		return true;
    	else return false;
    }
    
    public Piece getPiece(int row, int col) {
    	return board[row][col];
    }
    
    public boolean isInsideBoard(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }
    
    public void setPiece(int row, int col, Piece piece) {
        board[row][col] = piece;
    }
}