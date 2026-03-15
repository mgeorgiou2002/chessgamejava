package chess;

public class Piece {

    private PieceType type;
    private PieceColor color;
    private boolean hasMoved;

    public Piece(PieceType type, PieceColor color) {
        this.type = type;
        this.color = color;
        this.hasMoved = false;
    }

    public PieceType getType() {
        return type;
    }

    public PieceColor getColor() {
        return color;
    }
    
    public boolean hasMoved() {
        return hasMoved;
    }
    
    public void setHasMoved(boolean hasMoved) {
        this.hasMoved = hasMoved;
    }

    public char getSymbol() {

        char symbol;

        switch(type) {
            case KING: symbol = 'K'; break;
            case QUEEN: symbol = 'Q'; break;
            case ROOK: symbol = 'R'; break;
            case BISHOP: symbol = 'B'; break;
            case KNIGHT: symbol = 'N'; break;
            case PAWN: symbol = 'P'; break;
            default: symbol = '?';
        }

        if(color == PieceColor.BLACK)
            symbol = Character.toLowerCase(symbol);

        return symbol;
    }
}