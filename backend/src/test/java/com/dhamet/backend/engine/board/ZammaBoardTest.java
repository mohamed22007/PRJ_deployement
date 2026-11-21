package com.dhamet.backend.engine.board;

import com.dhamet.backend.engine.exception.InvalidMoveException;
import com.dhamet.backend.engine.model.PieceType;
import com.dhamet.backend.engine.model.PlayerColor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires du moteur de jeu Zamma.
 * Aucune base de données, aucun Spring : on teste la logique pure, donc c'est très rapide.
 *
 * Codage d'un plateau en texte (voir ZammaBoard.toBoardString) :
 *   0 = vide, 1 = pion noir, 2 = pion blanc, 3 = mullah noir, 4 = mullah blanc
 *   lignes séparées par ";", puis ".scoreBlanc,scoreNoir", puis ".B" ou ".W" (tour)
 */
class ZammaBoardTest {

    /** Construit un plateau 5x5 lisible : chaque String = une ligne, ex "1,0,0,0,0". */
    private static ZammaBoard board5(String turn, String... rows) {
        return new ZammaBoard(String.join(";", rows) + ";.0,0." + turn, 5);
    }

    @Nested
    @DisplayName("Plateau initial")
    class Initialisation {

        @Test
        void plateau9x9_a40PiecesParCouleurEtCentreVide() {
            ZammaBoard board = new ZammaBoard(9);

            assertEquals(40, board.countPieces(PlayerColor.BLACK));
            assertEquals(40, board.countPieces(PlayerColor.WHITE));
            assertTrue(board.isEmpty(4, 4), "La case centrale doit être vide");
        }

        @Test
        void lesNoirsCommencent() {
            ZammaBoard board = new ZammaBoard(9);
            assertTrue(board.toBoardString().endsWith(".B"));
        }

        @Test
        void scoreInitialAZero() {
            assertArrayEquals(new int[]{0, 0}, new ZammaBoard(9).getScore());
        }
    }

    @Nested
    @DisplayName("Sauvegarde texte (toBoardString / constructeur)")
    class Serialisation {

        @Test
        void allerRetour_donneLeMemePlateau() {
            ZammaBoard original = new ZammaBoard(9);
            String saved = original.toBoardString();

            ZammaBoard reloaded = new ZammaBoard(saved, 9);

            assertEquals(saved, reloaded.toBoardString());
        }

        @Test
        void lesMullahsEtLeScoreSontConserves() {
            ZammaBoard board = new ZammaBoard("3,0,0;0,0,0;0,0,4;.2,5.W", 3);

            assertEquals(PieceType.MULLAH, board.getPiece(0, 0).getType());
            assertEquals(PlayerColor.BLACK, board.getPiece(0, 0).getColor());
            assertEquals(PlayerColor.WHITE, board.getPiece(2, 2).getColor());
            assertArrayEquals(new int[]{2, 5}, board.getScore());
            assertEquals("3,0,0;0,0,0;0,0,4;.2,5.W", board.toBoardString());
        }
    }

    @Nested
    @DisplayName("Validation des coups : check_move (0 = interdit, 1 = simple, 2 = capture)")
    class CheckMove {

        @Test
        void pion_avanceDUneCaseOrthogonale() {
            ZammaBoard b = board5("B",
                    "0,0,0,0,0",
                    "0,1,0,0,0",
                    "0,0,0,0,0",
                    "0,0,0,0,0",
                    "0,0,0,0,0");
            assertEquals(1, b.check_move(1, 1, 2, 1));
            assertEquals(1, b.check_move(1, 1, 1, 2));
        }

        @Test
        void diagonale_autoriseeSeulementSurUnNoeudFort() {
            // (2,2) : ligne et colonne de même parité = noeud fort
            // (1,2) : parités différentes = noeud faible
            ZammaBoard b = board5("B",
                    "0,0,0,0,0",
                    "0,0,1,0,0",
                    "0,0,1,0,0",
                    "0,0,0,0,0",
                    "0,0,0,0,0");
            assertEquals(1, b.check_move(2, 2, 3, 3), "noeud fort : diagonale permise");
            assertEquals(0, b.check_move(1, 2, 2, 3), "noeud faible : diagonale interdite");
        }

        @Test
        void pion_neBougePasDeDeuxCasesSansCapture() {
            ZammaBoard b = board5("B",
                    "1,0,0,0,0",
                    "0,0,0,0,0",
                    "0,0,0,0,0",
                    "0,0,0,0,0",
                    "0,0,0,0,0");
            assertEquals(0, b.check_move(0, 0, 2, 0));
        }

        @Test
        void sautAuDessusDUnAdversaire_estUneCapture() {
            ZammaBoard b = board5("B",
                    "1,0,0,0,0",
                    "2,0,0,0,0",
                    "0,0,0,0,0",
                    "0,0,0,0,0",
                    "0,0,0,0,0");
            assertEquals(2, b.check_move(0, 0, 2, 0));
        }

        @Test
        void onNeSautePasSurSaPropreCouleur() {
            ZammaBoard b = board5("B",
                    "1,0,0,0,0",
                    "1,0,0,0,0",
                    "0,0,0,0,0",
                    "0,0,0,0,0",
                    "0,0,0,0,0");
            assertEquals(0, b.check_move(0, 0, 2, 0));
        }

        @Test
        void horsPlateauOuCaseOccupee_interdit() {
            ZammaBoard b = board5("B",
                    "1,2,0,0,0",
                    "0,0,0,0,0",
                    "0,0,0,0,0",
                    "0,0,0,0,0",
                    "0,0,0,0,0");
            assertEquals(0, b.check_move(0, 0, -1, 0), "hors plateau");
            assertEquals(0, b.check_move(0, 0, 0, 1), "case d'arrivée occupée");
            assertEquals(0, b.check_move(3, 3, 3, 4), "case de départ vide");
        }

        @Test
        void mullah_glisseSurPlusieursCasesEtCaptureDeLoin() {
            ZammaBoard b = board5("B",
                    "3,0,0,2,0",
                    "0,0,0,0,0",
                    "0,0,0,0,0",
                    "0,0,0,0,0",
                    "0,0,0,0,0");
            assertEquals(1, b.check_move(0, 0, 0, 2), "déplacement long sans obstacle");
            assertEquals(2, b.check_move(0, 0, 0, 4), "capture à distance");
            assertEquals(1, b.check_move(0, 0, 4, 4), "longue diagonale depuis un noeud fort");
        }

        @Test
        void mullah_neSautePasDeuxPieces() {
            ZammaBoard b = board5("B",
                    "3,2,2,0,0",
                    "0,0,0,0,0",
                    "0,0,0,0,0",
                    "0,0,0,0,0",
                    "0,0,0,0,0");
            assertEquals(0, b.check_move(0, 0, 0, 3));
        }
    }

    @Nested
    @DisplayName("Jouer une séquence : playSequence")
    class PlaySequence {

        @Test
        void coupSimple_deplaceLaPieceEtChangeLeTour() {
            ZammaBoard b = board5("B",
                    "0,0,0,0,0",
                    "0,1,0,0,0",
                    "0,0,0,0,0",
                    "0,0,0,0,0",
                    "0,0,0,2,0");

            b.playSequence(PlayerColor.BLACK, new int[][]{{1, 1}, {2, 1}});

            assertTrue(b.isEmpty(1, 1));
            assertEquals(PlayerColor.BLACK, b.getPiece(2, 1).getColor());
            assertTrue(b.toBoardString().endsWith(".W"), "c'est maintenant aux blancs");
        }

        @Test
        void refuseSiCeNEstPasLeTourDuJoueur() {
            ZammaBoard b = board5("B",
                    "0,0,0,0,0",
                    "0,0,0,0,0",
                    "0,0,0,0,0",
                    "0,0,0,2,0",
                    "0,0,0,0,0");

            InvalidMoveException e = assertThrows(InvalidMoveException.class,
                    () -> b.playSequence(PlayerColor.WHITE, new int[][]{{3, 3}, {2, 3}}));
            assertTrue(e.getMessage().contains("pas votre tour"));
        }

        @Test
        void refuseDeJouerLaPieceDeLAdversaire() {
            ZammaBoard b = board5("B",
                    "0,0,0,0,0",
                    "0,0,0,0,0",
                    "0,0,0,0,0",
                    "0,0,0,2,0",
                    "0,0,0,0,0");

            assertThrows(InvalidMoveException.class,
                    () -> b.playSequence(PlayerColor.BLACK, new int[][]{{3, 3}, {2, 3}}));
        }

        @Test
        void refuseUnCheminDUneSeuleCase() {
            ZammaBoard b = new ZammaBoard(9);
            assertThrows(InvalidMoveException.class,
                    () -> b.playSequence(PlayerColor.BLACK, new int[][]{{0, 0}}));
        }

        @Test
        void captureObligatoire_unCoupSimpleEstRefuse() {
            // Le noir en (0,0) peut prendre le blanc en (1,0) : il DOIT le faire
            ZammaBoard b = board5("B",
                    "1,0,0,0,1",
                    "2,0,0,0,0",
                    "0,0,0,0,0",
                    "0,0,0,0,0",
                    "0,0,0,0,0");

            InvalidMoveException e = assertThrows(InvalidMoveException.class,
                    () -> b.playSequence(PlayerColor.BLACK, new int[][]{{0, 4}, {1, 4}}));
            assertTrue(e.getMessage().contains("Capture obligatoire"));
        }

        @Test
        void capture_retireLaPieceAdverseEtAugmenteLeScore() {
            ZammaBoard b = board5("B",
                    "1,0,0,0,0",
                    "2,0,0,0,0",
                    "0,0,0,0,0",
                    "0,0,0,0,0",
                    "0,0,0,0,2");

            b.playSequence(PlayerColor.BLACK, new int[][]{{0, 0}, {2, 0}});

            assertTrue(b.isEmpty(1, 0), "le pion blanc capturé a disparu");
            assertEquals(PlayerColor.BLACK, b.getPiece(2, 0).getColor());
            assertEquals(1, b.countPieces(PlayerColor.WHITE));
            assertEquals(1, b.getScore()[1], "score[1] = score des noirs");
        }

        @Test
        void captureMultiple_enUneSeuleSequence() {
            ZammaBoard b = board5("B",
                    "1,0,0,0,0",
                    "2,0,0,0,0",
                    "0,0,0,0,0",
                    "2,0,0,0,0",
                    "0,0,0,0,0");

            b.playSequence(PlayerColor.BLACK, new int[][]{{0, 0}, {2, 0}, {4, 0}});

            assertEquals(0, b.countPieces(PlayerColor.WHITE));
            assertEquals(2, b.getScore()[1]);
        }

        @Test
        void interdictionDArreterUneCaptureEnCours() {
            ZammaBoard b = board5("B",
                    "1,0,0,0,0",
                    "2,0,0,0,0",
                    "0,0,0,0,0",
                    "2,0,0,0,0",
                    "0,0,0,0,0");

            InvalidMoveException e = assertThrows(InvalidMoveException.class,
                    () -> b.playSequence(PlayerColor.BLACK, new int[][]{{0, 0}, {2, 0}}));
            assertTrue(e.getMessage().contains("continuer la capture"));
        }
    }

    @Nested
    @DisplayName("Promotion en mullah")
    class Promotion {

        @Test
        void pionNoirArrivantSurLaDerniereLigne_devientMullah() {
            ZammaBoard b = board5("B",
                    "0,0,0,0,0",
                    "0,0,0,0,0",
                    "0,0,0,0,0",
                    "1,0,0,0,0",
                    "0,0,0,0,0");

            b.move(3, 0, 4, 0);

            assertEquals(PieceType.MULLAH, b.getPiece(4, 0).getType());
        }

        @Test
        void pionBlancArrivantSurLaLigne0_devientMullah() {
            ZammaBoard b = board5("W",
                    "0,0,0,0,0",
                    "2,0,0,0,0",
                    "0,0,0,0,0",
                    "0,0,0,0,0",
                    "0,0,0,0,0");

            b.move(1, 0, 0, 0);

            assertEquals(PieceType.MULLAH, b.getPiece(0, 0).getType());
        }
    }
}
