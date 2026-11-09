package com.dhamet.backend.engine.model;

public class Piece {
    private PlayerColor color;
    private PieceType type;

    public Piece(PlayerColor color, PieceType type) {
        this.color = color;
        this.type = type;
    }

    public PlayerColor getColor() {
        return color;
    }

    public PieceType getType() {
        return type;
    }

    public void setType(PieceType type) {
        this.type = type;
    }

    // juste pour les tests avant de le deployer 
    @Override
    public String toString() {
        if (color == PlayerColor.BLACK) return type == PieceType.NORMAL ? "B" : "BM";
        if (color == PlayerColor.WHITE) return type == PieceType.NORMAL ? "W" : "WM";
        return ".";
    }
}