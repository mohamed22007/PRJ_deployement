package com.dhamet.backend.service;

import com.dhamet.backend.ModelBD.Player;
import com.dhamet.backend.ModelBD.Session;
import com.dhamet.backend.repository.PlayerRepository;
import com.dhamet.backend.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de PlayerService.
 * Les repositories sont remplacés par des "mocks" (Mockito) : de faux objets
 * dont on choisit les réponses. On teste la logique du service, pas la base.
 */
@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private SessionRepository sessionRepository;

    @InjectMocks
    private PlayerService playerService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void saveRetourneLObjet() {
        // save() renvoie l'objet qu'on lui donne (comme une vraie base)
        lenient().when(playerRepository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Player joueur(String username, String motDePasse) {
        Player p = new Player();
        p.setId(1L);
        p.setUsername(username);
        p.setPassword(encoder.encode(motDePasse));
        return p;
    }

    // ---------- Inscription ----------

    @Test
    void register_creeLeJoueurAvec100CreditsEtMotDePasseHache() {
        Player p = playerService.register(" ali ", "secret123", "Ali", "Ali@Mail.com");

        assertEquals("ali", p.getUsername(), "espaces retirés");
        assertEquals("ali@mail.com", p.getEmail(), "email en minuscules");
        assertEquals(100, p.getCredit());
        assertNotEquals("secret123", p.getPassword(), "le mot de passe ne doit JAMAIS être stocké en clair");
        assertTrue(encoder.matches("secret123", p.getPassword()));
    }

    @Test
    void register_refuseUnMotDePasseTropCourt() {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> playerService.register("ali", "123", "Ali", "ali@mail.com"));
        assertTrue(e.getMessage().contains("trop court"));
        verify(playerRepository, never()).save(any());
    }

    @Test
    void register_refuseUnEmailInvalide() {
        assertThrows(RuntimeException.class,
                () -> playerService.register("ali", "secret123", "Ali", "pas-un-email"));
    }

    @Test
    void register_refuseUnUsernameVide() {
        assertThrows(RuntimeException.class,
                () -> playerService.register("  ", "secret123", "Ali", "ali@mail.com"));
    }

    @Test
    void register_refuseUnUsernameDejaPris() {
        when(playerRepository.existsByUsername("ali")).thenReturn(true);

        RuntimeException e = assertThrows(RuntimeException.class,
                () -> playerService.register("ali", "secret123", "Ali", "ali@mail.com"));
        assertTrue(e.getMessage().contains("déjà pris"));
    }

    @Test
    void register_refuseUnEmailDejaUtilise() {
        when(playerRepository.existsByEmail("ali@mail.com")).thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> playerService.register("ali", "secret123", "Ali", "ali@mail.com"));
    }

    // ---------- Connexion ----------

    @Test
    void login_avecBonMotDePasse_creeUneSession() {
        Player ali = joueur("ali", "secret123");
        when(playerRepository.findByUsername("ali")).thenReturn(Optional.of(ali));
        when(sessionRepository.findByPlayerAndIsActiveTrue(ali)).thenReturn(Optional.empty());

        Session s = playerService.login("ali", "secret123");

        assertSame(ali, s.getPlayer());
    }

    @Test
    void login_desactiveLAncienneSession() {
        Player ali = joueur("ali", "secret123");
        Session ancienne = new Session();
        ancienne.setActive(true);
        when(playerRepository.findByUsername("ali")).thenReturn(Optional.of(ali));
        when(sessionRepository.findByPlayerAndIsActiveTrue(ali)).thenReturn(Optional.of(ancienne));

        playerService.login("ali", "secret123");

        assertFalse(ancienne.isActive());
        assertNotNull(ancienne.getLogoutTime());
    }

    @Test
    void login_avecMauvaisMotDePasse_estRefuse() {
        when(playerRepository.findByUsername("ali")).thenReturn(Optional.of(joueur("ali", "secret123")));

        RuntimeException e = assertThrows(RuntimeException.class,
                () -> playerService.login("ali", "mauvais"));
        assertTrue(e.getMessage().contains("incorrect"));
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void login_joueurInconnu_estRefuse() {
        when(playerRepository.findByUsername("inconnu")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> playerService.login("inconnu", "x"));
    }

    // ---------- Token de session ----------

    @Test
    void getPlayerFromToken_sessionActive_renvoieLeJoueur() {
        Player ali = joueur("ali", "secret123");
        Session s = new Session();
        s.setActive(true);
        s.setPlayer(ali);
        when(sessionRepository.findByToken("tok")).thenReturn(Optional.of(s));

        assertSame(ali, playerService.getPlayerFromToken("tok"));
    }

    @Test
    void getPlayerFromToken_sessionExpiree_estRefusee() {
        Session s = new Session();
        s.setActive(false);
        when(sessionRepository.findByToken("tok")).thenReturn(Optional.of(s));

        RuntimeException e = assertThrows(RuntimeException.class,
                () -> playerService.getPlayerFromToken("tok"));
        assertTrue(e.getMessage().contains("expirée"));
    }

    @Test
    void getPlayerFromToken_tokenVide_estRefuse() {
        assertThrows(RuntimeException.class, () -> playerService.getPlayerFromToken(""));
        assertThrows(RuntimeException.class, () -> playerService.getPlayerFromToken(null));
    }

    @Test
    void logout_desactiveLaSession() {
        Session s = new Session();
        s.setActive(true);
        when(sessionRepository.findByToken("tok")).thenReturn(Optional.of(s));

        playerService.logout("tok");

        ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(captor.capture());
        assertFalse(captor.getValue().isActive());
    }

    // ---------- Classement ----------

    @Test
    void classement_trieParCreditDecroissant() {
        Player a = joueur("a", "xxxxxx"); a.setCredit(50);
        Player b = joueur("b", "xxxxxx"); b.setCredit(200);
        Player c = joueur("c", "xxxxxx"); c.setCredit(120);
        when(playerRepository.findAll()).thenReturn(List.of(a, b, c));

        List<Player> ranking = playerService.getAllSortedByCredit();

        assertEquals(List.of("b", "c", "a"), ranking.stream().map(Player::getUsername).toList());
    }
}
