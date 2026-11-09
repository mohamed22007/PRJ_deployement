package com.dhamet.backend.service;

import com.dhamet.backend.ModelBD.Player;
import com.dhamet.backend.ModelBD.Session;
import com.dhamet.backend.repository.PlayerRepository;
import com.dhamet.backend.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class PlayerService {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private SessionRepository sessionRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** Inscription d'un nouveau joueur */
    public Player register(String username, String password, String nom, String email) {
        if (username == null || username.isBlank()) throw new RuntimeException("Nom d'utilisateur requis.");
        if (password == null || password.length() < 6) throw new RuntimeException("Mot de passe trop court (6 caractères minimum).");
        if (nom == null || nom.isBlank()) throw new RuntimeException("Nom requis.");
        if (email == null || !email.contains("@")) throw new RuntimeException("Email invalide.");

        if (playerRepository.existsByUsername(username)) {
            throw new RuntimeException("Ce nom d'utilisateur est déjà pris.");
        }
        if (playerRepository.existsByEmail(email)) {
            throw new RuntimeException("Cet email est déjà utilisé.");
        }
        Player player = new Player();
        player.setUsername(username.trim());
        player.setPassword(passwordEncoder.encode(password));
        player.setNom(nom.trim());
        player.setEmail(email.trim().toLowerCase());
        player.setCredit(100);
        return playerRepository.save(player);
    }

    /** Connexion et création d'une session */
    public Session login(String username, String password) {
        if (username == null || password == null) {
            throw new RuntimeException("Identifiants requis.");
        }
        Player player = playerRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Joueur introuvable."));

        if (!passwordEncoder.matches(password, player.getPassword())) {
            throw new RuntimeException("Mot de passe incorrect.");
        }

        // Invalider les sessions actives existantes
        sessionRepository.findByPlayerAndIsActiveTrue(player).ifPresent(s -> {
            s.setActive(false);
            s.setLogoutTime(LocalDateTime.now());
            sessionRepository.save(s);
        });

        Session session = new Session();
        session.setPlayer(player);
        return sessionRepository.save(session);
    }

    /** Déconnexion */
    public void logout(String token) {
        Session session = sessionRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Session introuvable."));
        session.setActive(false);
        session.setLogoutTime(LocalDateTime.now());
        sessionRepository.save(session);
    }

    /** Récupère le joueur depuis son token de session */
    public Player getPlayerFromToken(String token) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("Token manquant.");
        }
        Session session = sessionRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Session invalide."));
        if (!session.isActive()) {
            throw new RuntimeException("Session expirée.");
        }
        return session.getPlayer();
    }

    public Player findById(Long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Joueur introuvable."));
    }

    public Player findByUsername(String username) {
        return playerRepository.findByUsername(username).orElse(null);
    }

    public List<Player> getAllSortedByCredit() {
        return playerRepository.findAll().stream()
            .sorted((a, b) -> Integer.compare(b.getCredit(), a.getCredit()))
            .collect(java.util.stream.Collectors.toList());
    }
}
