package com.dhamet.backend.engine.board;

import com.dhamet.backend.engine.exception.InvalidMoveException;
import com.dhamet.backend.engine.model.Piece;
import com.dhamet.backend.engine.model.PieceType;
import com.dhamet.backend.engine.model.PlayerColor;

public class ZammaBoard {

    private PlayerColor currentTurn;
    private int size ;
    private Piece[][] grid;
    private int[] score ;

    public ZammaBoard(int size) {
        if (size > 1) {
            this.size = size;
            this.grid = new Piece[size][size];
            initializeBoard();
            score = new int[2];
            score[0] = 0;
            score[1] = 0;
            this.currentTurn = PlayerColor.BLACK;
        }
    }

    public ZammaBoard(String boardString, int size) {
        this.size = size;
        this.grid = new Piece[size][size];
        this.score = new int[2];

        // 1. Séparer la grille du score (le point final)
        String[] parts = boardString.split("\\.");
        String gridPart = parts[0];
        String scorePart = parts[1];
        String turnString = parts[2];
        this.currentTurn = turnString.equals("B") ? PlayerColor.BLACK : PlayerColor.WHITE;


        // 2. Reconstruire la grille
        String[] rows = gridPart.split(";");
        for (int r = 0; r < size; r++) {
            String[] cols = rows[r].split(",");
            for (int c = 0; c < size; c++) {
                String val = cols[c];
                switch (val) {
                    case "0": grid[r][c] = null; break;
                    case "1": grid[r][c] = new Piece(PlayerColor.BLACK, PieceType.NORMAL); break;
                    case "2": grid[r][c] = new Piece(PlayerColor.WHITE, PieceType.NORMAL); break;
                    case "3": grid[r][c] = new Piece(PlayerColor.BLACK, PieceType.MULLAH); break;
                    case "4": grid[r][c] = new Piece(PlayerColor.WHITE, PieceType.MULLAH); break;
                }
            }
        }

        // 3. Reconstruire le score
        String[] scores = scorePart.split(",");
        this.score[0] = Integer.parseInt(scores[0]);
        this.score[1] = Integer.parseInt(scores[1]);
    }


    public int[] getScore(){
        return score ;
    }

    public int countPieces(PlayerColor player) {
        int count = 0;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                Piece p = grid[r][c];
                if (p != null && p.getColor() == player) count++;
            }
        }
        return count;
    }

    public boolean canPieceCaptureFrom(int r, int c) {
        for (int r2 = 0; r2 < size; r2++) {
            for (int c2 = 0; c2 < size; c2++) {
                if (check_move(r, c, r2, c2) == 2) return true;
            }
        }
        return false;
    }

    public boolean hasCaptureAvailable(PlayerColor player) {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                Piece p = grid[r][c];
                if (p != null && p.getColor() == player && canPieceCaptureFrom(r, c)) return true;
            }
        }
        return false;
    }

    /**
     * @param player La couleur du joueur qui fait la requête
     * @param path Un tableau de coordonnées, ex: {{startI, startJ}, {step1I, step1J}, {endI, endJ}}
     */
    public void playSequence(PlayerColor player, int[][] path) {
        if (path.length < 2) {
            throw new InvalidMoveException("Un mouvement nécessite au moins une case de départ et d'arrivée.");
        }

        // 1. Vérifier si c'est bien le tour du joueur
        if (player != currentTurn) {
            throw new InvalidMoveException("Ce n'est pas votre tour de jouer.");
        }

        int startRow = path[0][0];
        int startCol = path[0][1];
        Piece movingPiece = getPiece(startRow, startCol);

        // 2. Vérifier que la pièce appartient bien au joueur
        if (movingPiece == null || movingPiece.getColor() != player) {
            throw new InvalidMoveException("Vous devez jouer avec l'une de vos pièces.");
        }

        boolean capturesAvailableAtStart = hasCaptureAvailable(player);
        boolean isCaptureSequence = false;

        // 3. Boucler sur chaque étape du chemin
        for (int k = 0; k < path.length - 1; k++) {
            int r1 = path[k][0];
            int c1 = path[k][1];
            int r2 = path[k+1][0];
            int c2 = path[k+1][1];

            int res = check_move(r1, c1, r2, c2);

            if (res == 0) {
                throw new InvalidMoveException("Séquence invalide à l'étape " + (k+1));
            }

            if (res == 1) { // Mouvement simple
                if (path.length > 2) {
                    throw new InvalidMoveException("Un déplacement simple ne peut pas faire partie d'une séquence multiple.");
                }
                if (capturesAvailableAtStart) {
                    throw new InvalidMoveException("Capture obligatoire ! Vous devez prendre un pion adverse.");
                }
                move(r1, c1, r2, c2);
            }

            if (res == 2) { // Capture
                isCaptureSequence = true;
                move(r1, c1, r2, c2);
            }
        }

        // 4. Vérifier que le joueur n'a pas coupé une séquence de captures
        if (isCaptureSequence) {
            int finalRow = path[path.length - 1][0];
            int finalCol = path[path.length - 1][1];
            if (canPieceCaptureFrom(finalRow, finalCol)) {
                throw new InvalidMoveException("Vous devez continuer la capture !");
            }
        }

        // 5. Si toute la séquence est valide, on change de tour
        switchTurn();
    }

    private void switchTurn() {
        this.currentTurn = (this.currentTurn == PlayerColor.BLACK) ? PlayerColor.WHITE : PlayerColor.BLACK;
    }


    public void move(int anci, int ancj, int nexti, int nextj){
         int res = check_move(anci, ancj, nexti, nextj);
         if (res == 0){
             throw new InvalidMoveException("Mouvement impossible");
         } 
         // Déplacement de la pièce
         Piece movingPiece = grid[anci][ancj];
         grid[nexti][nextj] = movingPiece;
         grid[anci][ancj] = null;

         if (res == 2) {
             // On doit trouver et supprimer la pièce capturée
             int stepRow = Integer.compare(nexti, anci);
             int stepCol = Integer.compare(nextj, ancj);
             
             int r = anci + stepRow;
             int c = ancj + stepCol;
             
             // On cherche la pièce sur la trajectoire pour la supprimer
             while (r != nexti || c != nextj) {
                 if (!isEmpty(r, c)) {
                     grid[r][c] = null; 
                     break;
                 }
                 r += stepRow;
                 c += stepCol;
             }
             
             // Mise à jour du score
             int playerIndex = (movingPiece.getColor() == PlayerColor.WHITE) ? 0 : 1;
             score[playerIndex]++;
         }

         // PROMOTION EN MULLAH
         if (movingPiece.getType() == PieceType.NORMAL) {
             if (movingPiece.getColor() == PlayerColor.BLACK && nexti == size - 1) {
                 movingPiece.setType(PieceType.MULLAH);
             }
             else if (movingPiece.getColor() == PlayerColor.WHITE && nexti == 0) {
                 movingPiece.setType(PieceType.MULLAH);
             }
         }
    }

public int check_move(int anci, int ancj, int nexti, int nextj) {

        if (isOutOfBounds(anci, ancj) || isOutOfBounds(nexti, nextj)) return 0;
        if (isEmpty(anci, ancj) || !isEmpty(nexti, nextj)) return 0;

        Piece movingPiece = getPiece(anci, ancj);
        boolean isStrongNode = (anci % 2 == ancj % 2);

        if (movingPiece.getType() == PieceType.MULLAH) { 
            int dRow = Math.abs(nexti - anci);
            int dCol = Math.abs(nextj - ancj);

            // Vérifier que le mouvement est en ligne droite ou en diagonale stricte
            if (dRow != dCol && dRow != 0 && dCol != 0) return 0;
            
            // Si c'est une diagonale, il faut être sur un noeud fort
            if (dRow == dCol && !isStrongNode) return 0;

            // Déterminer la direction du pas (-1, 0, ou 1)
            int stepRow = Integer.compare(nexti, anci);
            int stepCol = Integer.compare(nextj, ancj);

            int r = anci + stepRow;
            int c = ancj + stepCol;
            int piecesInPath = 0;
            Piece jumpedPiece = null;

            // Parcourir la trajectoire
            while (r != nexti || c != nextj) {
                if (!isEmpty(r, c)) {
                    piecesInPath++;
                    jumpedPiece = getPiece(r, c);
                }
                r += stepRow;
                c += stepCol;
            }

            if (piecesInPath == 0) return 1; // Déplacement simple
            if (piecesInPath == 1 && jumpedPiece.getColor() != movingPiece.getColor()) return 2; // Capture
            
            return 0; // S'il y a plus d'une pièce ou une pièce alliée sur le chemin
        }

        // LOGIQUE POUR UN PION NORMAL 
        int dRow = Math.abs(nexti - anci);
        int dCol = Math.abs(nextj - ancj);

        boolean isOrthogonalMove = (dRow + dCol == 1);
        boolean isDiagonalMove = (dRow == 1 && dCol == 1);

        if (isOrthogonalMove || (isStrongNode && isDiagonalMove)) return 1;

        boolean isOrthogonalJump = ((dRow == 2 && dCol == 0) || (dRow == 0 && dCol == 2));
        boolean isDiagonalJump = (dRow == 2 && dCol == 2);

        if (isOrthogonalJump || (isStrongNode && isDiagonalJump)) {
            int midRow = (anci + nexti) / 2;
            int midCol = (ancj + nextj) / 2;

            if (!isEmpty(midRow, midCol)) {
                Piece jumpedPiece = getPiece(midRow, midCol);
                if (movingPiece.getColor() != jumpedPiece.getColor()) return 2;
            }
        }

        return 0;
    }


    private void initializeBoard() {
        int milieu = size / 2;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (r == milieu && c == milieu) {
                    grid[r][c] = null; // Case centrale vide
                } else if (r < milieu || (r == milieu && c < milieu)) {
                    grid[r][c] = new Piece(PlayerColor.BLACK, PieceType.NORMAL);
                } else {
                    grid[r][c] = new Piece(PlayerColor.WHITE, PieceType.NORMAL);
                }
            }
        }
    }

    public Piece getPiece(int row, int col) {
        if (isOutOfBounds(row, col)) return null;
        return grid[row][col];
    }

    public void setPiece(int row, int col, Piece piece) {
        if ((!isOutOfBounds(row, col))&&(isEmpty(row, col))) {
            grid[row][col] = piece;
        }
    }

    public boolean isOutOfBounds(int row, int col) {
        return row < 0 || row >= size || col < 0 || col >= size;
    }

    public boolean isEmpty(int row, int col) {
        return getPiece(row, col) == null;
    }

    // transformer carte to string pour enregistrements 
    public String toBoardString() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (c > 0) sb.append(","); // Séparateur AVANT chaque colonne sauf la première
                Piece p = grid[r][c];
                if (p == null) sb.append("0");
                else if (p.getColor() == PlayerColor.BLACK && p.getType() == PieceType.NORMAL)  sb.append("1");
                else if (p.getColor() == PlayerColor.WHITE && p.getType() == PieceType.NORMAL)  sb.append("2");
                else if (p.getColor() == PlayerColor.BLACK && p.getType() == PieceType.MULLAH)  sb.append("3"); // BUG CORRIGÉ : Mullah noir
                else if (p.getColor() == PlayerColor.WHITE && p.getType() == PieceType.MULLAH)  sb.append("4"); // BUG CORRIGÉ : Mullah blanc
            }
            sb.append(";"); // Séparateur de ligne
        }
        sb.append(".");
        sb.append(score[0]);
        sb.append(",");
        sb.append(score[1]);
        sb.append(".");
        sb.append(currentTurn == PlayerColor.BLACK ? "B" : "W");
        return sb.toString() ;
    }
}