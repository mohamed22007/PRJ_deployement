package com.dhamet.backend.controller;
 
import com.dhamet.backend.ModelBD.Game;
import com.dhamet.backend.ModelBD.Move;
import com.dhamet.backend.ModelBD.Player;
import com.dhamet.backend.service.GameService;
import com.dhamet.backend.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;

 
import java.util.List;
import java.util.Map;
 
@RestController
@RequestMapping("/api/games")
@CrossOrigin(origins = {"http://localhost:4200"})
public class GameController {
 
    @Autowired
    private GameService gameService;
 
    @Autowired
    private PlayerService playerService;
 
    /**
     * POST /api/games/create
     * Header: X-Session-Token: <token>
     * Crée une nouvelle partie et retourne le joinToken
     */
    @PostMapping("/create")
    public ResponseEntity<?> createGame(@RequestHeader("X-Session-Token") String token) {
        try {
            Player player = playerService.getPlayerFromToken(token);
            Game game = gameService.createGame(player);
            return ResponseEntity.ok(Map.of(
                    "gameId", game.getId(),
                    "joinToken", game.getJoinToken(),
                    "status", game.getStatus()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
 
    /**
     * POST /api/games/join/{joinToken}
     * Header: X-Session-Token: <token>
     * Rejoindre une partie existante
     */
    @PostMapping("/join/{joinToken}")
    public ResponseEntity<?> joinGame(
            @PathVariable String joinToken,
            @RequestHeader("X-Session-Token") String token) {
        try {
            Player player = playerService.getPlayerFromToken(token);
            Game game = gameService.joinGame(joinToken, player);
            return ResponseEntity.ok(Map.of(
                    "gameId", game.getId(),
                    "joinToken", game.getJoinToken(), // BUG CORRIGÉ : joinToken manquait
                    "status", game.getStatus(),
                    "boardState", game.getBoardState(),
                    "playerBlack", game.getPlayerBlack().getUsername(),
                    "playerWhite", game.getPlayerWhite().getUsername()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
 
    /**
     * GET /api/games/{gameId}
     * Récupère l'état d'une partie
     */
    @GetMapping("/{gameId}")
    public ResponseEntity<?> getGame(
            @PathVariable Long gameId,
            @RequestHeader("X-Session-Token") String token) {
        try {
            playerService.getPlayerFromToken(token); // Vérification auth
            Game game = gameService.findById(gameId);
            
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("gameId", game.getId());
            response.put("status", game.getStatus());
            response.put("boardState", game.getBoardState());
            response.put("playerBlack", game.getPlayerBlack().getUsername());

            if (game.getPlayerWhite() != null) {
                response.put("playerWhite", game.getPlayerWhite().getUsername());
            }
            if (game.getWinner() != null) {
                response.put("winner", game.getWinner().getUsername());
            }

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
 
    /**
     * POST /api/games/{gameId}/move
     * Header: X-Session-Token: <token>
     * Body: { "path": [[r1,c1],[r2,c2],...] }
     * Joue un coup
     */
    @PostMapping("/{gameId}/move")
    public ResponseEntity<?> playMove(
            @PathVariable Long gameId,
            @RequestHeader("X-Session-Token") String token,
            @RequestBody Map<String, Object> body) {
        try {
            Player player = playerService.getPlayerFromToken(token);
 
            // Conversion du path depuis le JSON
            List<List<Integer>> rawPath = (List<List<Integer>>) body.get("path");
            int[][] path = rawPath.stream()
                    .map(step -> new int[]{step.get(0), step.get(1)})
                    .toArray(int[][]::new);
 
            Game game = gameService.playMove(gameId, player, path);
            java.util.Map<String, Object> moveResponse = new java.util.HashMap<>();
            moveResponse.put("boardState", game.getBoardState());
            moveResponse.put("status", game.getStatus());
            if (game.getWinner() != null) {
                moveResponse.put("winner", game.getWinner().getUsername());
            }
            return ResponseEntity.ok(moveResponse);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
 
    /**
     * POST /api/games/{gameId}/resign
     * Header: X-Session-Token: <token>
     * Abandonner une partie
     */
    @PostMapping("/{gameId}/resign")
    public ResponseEntity<?> resign(
            @PathVariable Long gameId,
            @RequestHeader("X-Session-Token") String token) {
        try {
            Player player = playerService.getPlayerFromToken(token);
            Game game = gameService.findById(gameId);
 
            // Le gagnant est l'adversaire
            Player winner = game.getPlayerBlack().getId().equals(player.getId())
                    ? game.getPlayerWhite()
                    : game.getPlayerBlack();
 
            gameService.finishGame(gameId, winner, false);
            return ResponseEntity.ok(Map.of("message", "Vous avez abandonné la partie."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
 
    /**
     * GET /api/games/waiting
     * Liste les parties en attente de joueur
     */
    @GetMapping("/waiting")
    public ResponseEntity<?> getWaitingGames(@RequestHeader("X-Session-Token") String token) {
        try {
            playerService.getPlayerFromToken(token);
            List<Game> games = gameService.getWaitingGames();
            return ResponseEntity.ok(games.stream().map(g -> Map.of(
                    "gameId", g.getId(),
                    "joinToken", g.getJoinToken(),
                    "playerBlack", g.getPlayerBlack().getUsername()
            )).toList());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    

    // ... Reste de tes méthodes dans GameController ...

    /**
     * GET /api/games/current
     * Vérifie si le joueur a déjà une partie en cours ou en attente
     */
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentGame(@RequestHeader("X-Session-Token") String token) {
        try {
            Player player = playerService.getPlayerFromToken(token);
            Game game = gameService.getCurrentActiveGame(player);
            
            if (game != null) {
                return ResponseEntity.ok(Map.of(
                        "hasGame", true,
                        "gameId", game.getId(),
                        "joinToken", game.getJoinToken(),
                        "status", game.getStatus()
                ));
            } else {
                return ResponseEntity.ok(Map.of("hasGame", false));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/games/history
     * Historique des parties terminées du joueur connecté
     */
    @GetMapping("/history")
    public ResponseEntity<?> getGameHistory(@RequestHeader("X-Session-Token") String token) {
        try {
            Player player = playerService.getPlayerFromToken(token);
            return ResponseEntity.ok(gameService.getHistoryForPlayer(player));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/games/{gameId}/moves
     * Historique des coups d'une partie
     */
    @GetMapping("/{gameId}/moves")
    public ResponseEntity<?> getMoves(
            @PathVariable Long gameId,
            @RequestHeader("X-Session-Token") String token) {
        try {
            playerService.getPlayerFromToken(token);
            List<Move> moves = gameService.getMovesForGame(gameId);
            ObjectMapper mapper = new ObjectMapper();

            return ResponseEntity.ok(moves.stream().map(m -> {
                // Re-transformer la string JSON en Liste pour le frontend
                List<List<Integer>> pathList = new ArrayList<>();
                try {
                    if (m.getPathJson() != null) {
                        pathList = mapper.readValue(m.getPathJson(), new TypeReference<List<List<Integer>>>() {});
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return Map.of(
                        "moveId", m.getId(),
                        "player", m.getPlayer().getUsername(),
                        "path", pathList, // Envoie la liste de toutes les cases traversées
                        "isCapture", m.isCapture(),
                        "timestamp", m.getTimestamp().toString()
                );
            }).toList());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}