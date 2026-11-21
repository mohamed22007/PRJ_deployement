package com.dhamet.backend.service;

import com.dhamet.backend.ModelBD.Game;
import com.dhamet.backend.ModelBD.Player;
import com.dhamet.backend.engine.exception.InvalidMoveException;
import com.dhamet.backend.repository.GameRepository;
import com.dhamet.backend.repository.MoveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de GameService (création, rejoindre, jouer, fin de partie).
 * Le WebSocket (SimpMessagingTemplate) est aussi un mock : on vérifie juste qu'il est appelé.
 */
@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock private GameRepository gameRepository;
    @Mock private MoveRepository moveRepository;
    @Mock private SimpMessagingTemplate ws;

    @InjectMocks
    private GameService gameService;

    private Player noir;
    private Player blanc;

    @BeforeEach
    void setUp() {
        lenient().when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));
        noir = joueur(1L, "noir");
        blanc = joueur(2L, "blanc");
    }

    private static Player joueur(Long id, String username) {
        Player p = new Player();
        p.setId(id);
        p.setUsername(username);
        p.setCredit(100);
        return p;
    }

    private Game partieEnCours(String boardState) {
        Game g = new Game();
        g.setId(10L);
        g.setPlayerBlack(noir);
        g.setPlayerWhite(blanc);
        g.setStatus("PLAYING");
        g.setBoardState(boardState);
        noir.setBusy(true);
        blanc.setBusy(true);
        when(gameRepository.findById(10L)).thenReturn(Optional.of(g));
        return g;
    }

    // ---------- Création ----------

    @Test
    void createGame_creeUnePartieEnAttenteAvecUnCodeDe6Caracteres() {
        Game g = gameService.createGame(noir);

        assertEquals("WAITING", g.getStatus());
        assertSame(noir, g.getPlayerBlack());
        assertEquals(6, g.getJoinToken().length());
        assertTrue(noir.isBusy(), "le créateur est marqué occupé");
        assertNotNull(g.getBoardState());
    }

    @Test
    void createGame_refuseSiLeJoueurEstDejaEnPartie() {
        noir.setBusy(true);
        assertThrows(RuntimeException.class, () -> gameService.createGame(noir));
        verify(gameRepository, never()).save(any());
    }

    // ---------- Rejoindre ----------

    @Test
    void joinGame_lancerLaPartieEtPrevenirParWebSocket() {
        Game g = new Game();
        g.setId(10L);
        g.setPlayerBlack(noir);
        g.setStatus("WAITING");
        when(gameRepository.findByJoinTokenIgnoreCase("ABC123")).thenReturn(Optional.of(g));

        Game result = gameService.joinGame(" abc123 ", blanc);

        assertEquals("PLAYING", result.getStatus());
        assertSame(blanc, result.getPlayerWhite());
        assertTrue(blanc.isBusy());
        verify(ws).convertAndSend(eq("/topic/game/10"), any(Object.class));
    }

    @Test
    void joinGame_refuseDeRejoindreSaProprePartie() {
        Game g = new Game();
        g.setPlayerBlack(noir);
        g.setStatus("WAITING");
        when(gameRepository.findByJoinTokenIgnoreCase("ABC123")).thenReturn(Optional.of(g));

        assertThrows(RuntimeException.class, () -> gameService.joinGame("ABC123", noir));
    }

    @Test
    void joinGame_refuseUnePartieDejaCommencee() {
        Game g = new Game();
        g.setPlayerBlack(noir);
        g.setStatus("PLAYING");
        when(gameRepository.findByJoinTokenIgnoreCase("ABC123")).thenReturn(Optional.of(g));

        assertThrows(RuntimeException.class, () -> gameService.joinGame("ABC123", blanc));
    }

    @Test
    void joinGame_codeInconnu() {
        when(gameRepository.findByJoinTokenIgnoreCase("ZZZZZZ")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> gameService.joinGame("zzzzzz", blanc));
    }

    // ---------- Jouer ----------

    @Test
    void playMove_coupValide_metAJourLePlateauEtEnregistreLeCoup() {
        // Plateau 9x9 initial : les noirs jouent (3,4) -> (4,4), la case centrale vide
        Game g = partieEnCours(new com.dhamet.backend.engine.board.ZammaBoard(9).toBoardString());
        String avant = g.getBoardState();

        gameService.playMove(10L, noir, new int[][]{{3, 4}, {4, 4}});

        assertNotEquals(avant, g.getBoardState());
        assertTrue(g.getBoardState().endsWith(".W"), "au tour des blancs");
        verify(moveRepository).save(any());
    }

    @Test
    void playMove_refuseUnJoueurExterieurALaPartie() {
        partieEnCours(new com.dhamet.backend.engine.board.ZammaBoard(9).toBoardString());
        Player intrus = joueur(99L, "intrus");

        assertThrows(RuntimeException.class,
                () -> gameService.playMove(10L, intrus, new int[][]{{3, 4}, {4, 4}}));
    }

    @Test
    void playMove_coupIllegal_leveInvalidMoveException() {
        partieEnCours(new com.dhamet.backend.engine.board.ZammaBoard(9).toBoardString());

        // Ce n'est pas le tour des blancs
        assertThrows(InvalidMoveException.class,
                () -> gameService.playMove(10L, blanc, new int[][]{{5, 4}, {4, 4}}));
        verify(moveRepository, never()).save(any());
    }

    @Test
    void playMove_capturerLaDernierePiece_termineLaPartieEtDonneLesCredits() {
        // Plateau 9x9 avec un seul pion blanc en (1,0), capturable par le noir en (0,0)
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (c > 0) sb.append(",");
                if (r == 0 && c == 0) sb.append("1");
                else if (r == 1 && c == 0) sb.append("2");
                else sb.append("0");
            }
            sb.append(";");
        }
        sb.append(".0,0.B");
        Game g = partieEnCours(sb.toString());

        gameService.playMove(10L, noir, new int[][]{{0, 0}, {2, 0}});

        assertEquals("FINISHED", g.getStatus());
        assertSame(noir, g.getWinner());
        assertEquals(120, noir.getCredit(), "gagnant : +20");
        assertEquals(90, blanc.getCredit(), "perdant : -10");
        assertFalse(noir.isBusy());
        assertFalse(blanc.isBusy());
    }

    // ---------- Fin de partie ----------

    @Test
    void finishGame_abandon_creditsEtJoueursLiberes() {
        Game g = partieEnCours("x");

        gameService.finishGame(10L, blanc, false);

        assertEquals("FINISHED", g.getStatus());
        assertSame(blanc, g.getWinner());
        assertEquals(120, blanc.getCredit());
        assertEquals(90, noir.getCredit());
        assertFalse(noir.isBusy());
        assertFalse(blanc.isBusy());
    }

    @Test
    void finishGame_lesCreditsNeDeviennentJamaisNegatifs() {
        noir.setCredit(5);
        partieEnCours("x");

        gameService.finishGame(10L, blanc, false);

        assertEquals(0, noir.getCredit());
    }

    @Test
    void finishGame_matchNul_creditsInchanges() {
        Game g = partieEnCours("x");

        gameService.finishGame(10L, null, true);

        assertTrue(g.isDraw());
        assertNull(g.getWinner());
        assertEquals(100, noir.getCredit());
        assertEquals(100, blanc.getCredit());
    }
}
