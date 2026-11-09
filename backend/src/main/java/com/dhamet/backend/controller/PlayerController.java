package com.dhamet.backend.controller;
 
import com.dhamet.backend.ModelBD.Player;
import com.dhamet.backend.ModelBD.Session;
import com.dhamet.backend.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;
import java.util.Map;
 
@RestController
@RequestMapping("/api/players")
@CrossOrigin(origins = {"http://localhost:4200"})
public class PlayerController {
 
    @Autowired
    private PlayerService playerService;
 
    /**
     * POST /api/players/register
     * Body: { "username": "...", "password": "...", "nom": "...", "email": "..." }
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            if (body == null) return ResponseEntity.badRequest().body(Map.of("error", "Corps de requête manquant."));
            Player player = playerService.register(
                    body.get("username"),
                    body.get("password"),
                    body.get("nom"),
                    body.get("email")
            );
            return ResponseEntity.ok(Map.of(
                    "message", "Inscription réussie",
                    "playerId", player.getId(),
                    "username", player.getUsername()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
 
    /**
     * POST /api/players/login
     * Body: { "username": "...", "password": "..." }
     * Retourne le token de session
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            Session session = playerService.login(body.get("username"), body.get("password"));
            return ResponseEntity.ok(Map.of(
                    "token", session.getToken(),
                    "playerId", session.getPlayer().getId(),
                    "username", session.getPlayer().getUsername()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
 
    /**
     * POST /api/players/logout
     * Header: X-Session-Token: <token>
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("X-Session-Token") String token) {
        try {
            playerService.logout(token);
            return ResponseEntity.ok(Map.of("message", "Déconnexion réussie"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
 
    /**
     * GET /api/players/me
     * Header: X-Session-Token: <token>
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMe(@RequestHeader("X-Session-Token") String token) {
        try {
            Player player = playerService.getPlayerFromToken(token);
            return ResponseEntity.ok(Map.of(
                    "id", player.getId(),
                    "username", player.getUsername(),
                    "nom", player.getNom(),
                    "email", player.getEmail(),
                    "credit", player.getCredit(),
                    "busy", player.isBusy()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/players/ranking
     * Classement général par crédits
     */
    @GetMapping("/ranking")
    public ResponseEntity<?> getRanking(@RequestHeader("X-Session-Token") String token) {
        try {
            playerService.getPlayerFromToken(token);
            List<Player> players = playerService.getAllSortedByCredit();
            return ResponseEntity.ok(players.stream().map(p -> Map.of(
                "username", p.getUsername(),
                "nom", p.getNom() != null ? p.getNom() : p.getUsername(),
                "credit", p.getCredit()
            )).toList());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/players/search?username=xxx
     * Recherche un joueur par nom d'utilisateur
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchByUsername(
            @RequestParam String username,
            @RequestHeader("X-Session-Token") String token) {
        try {
            playerService.getPlayerFromToken(token);
            Player found = playerService.findByUsername(username);
            if (found == null) {
                return ResponseEntity.ok(Map.of("found", false));
            }
            return ResponseEntity.ok(Map.of(
                    "found", true,
                    "id", found.getId(),
                    "username", found.getUsername()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}