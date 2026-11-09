package com.dhamet.backend.service;
 
import com.dhamet.backend.ModelBD.Game;
import com.dhamet.backend.ModelBD.Move;
import com.dhamet.backend.ModelBD.Player;
import com.dhamet.backend.engine.board.ZammaBoard;
import com.dhamet.backend.engine.model.PlayerColor;
import com.dhamet.backend.repository.GameRepository;
import com.dhamet.backend.repository.MoveRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
 
@Service
@Transactional
public class GameService {
 
    private static final int BOARD_SIZE = 9; // Plateau Zamma 9x9 (standard)
 
    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private MoveRepository moveRepository;

    @Autowired
    private SimpMessagingTemplate ws;
 
    public Game createGame(Player player) {
        if (player.isBusy()) {
            throw new RuntimeException("Vous êtes déjà dans une partie.");
        }
 
        ZammaBoard board = new ZammaBoard(BOARD_SIZE);
 
        Game game = new Game();
        game.setPlayerBlack(player);
        game.setStatus("WAITING");
        game.setBoardState(board.toBoardString());
        game.setJoinToken(UUID.randomUUID().toString().substring(0, 6).toUpperCase()); // BUG CORRIGÉ : génération du token
 
        player.setBusy(true);
        return gameRepository.save(game);
    }
 
    public Game joinGame(String joinToken, Player player) {
        String normalizedToken = joinToken == null ? "" : joinToken.trim().toUpperCase();
        Game game = gameRepository.findByJoinTokenIgnoreCase(normalizedToken)
                .orElseThrow(() -> new RuntimeException("Partie introuvable avec ce code."));
 
        if (!game.getStatus().equals("WAITING")) {
            throw new RuntimeException("Cette partie a déjà commencé ou est terminée.");
        }
        if (game.getPlayerBlack().getId().equals(player.getId())) {
            throw new RuntimeException("Vous ne pouvez pas rejoindre votre propre partie.");
        }
        if (player.isBusy()) {
            throw new RuntimeException("Vous êtes déjà dans une partie.");
        }
 
        game.setPlayerWhite(player);
        game.setStatus("PLAYING");
        player.setBusy(true);
        Game saved = gameRepository.save(game);
        broadcastGame(saved);
        return saved;
    }

    // Ajoutez ceci à la fin de GameService.java
    public Game getCurrentActiveGame(Player player) {
        List<Game> games = gameRepository.findByPlayerBlackOrPlayerWhite(player, player);
        Optional<Game> activeGame = games.stream()
                .filter(g -> g.getStatus().equals("WAITING") || g.getStatus().equals("PLAYING"))
                .findFirst();
        
        // Auto-correction : si le joueur est marqué occupé mais n'a pas de partie active
        if (activeGame.isEmpty() && player.isBusy()) {
            player.setBusy(false);
        }
        return activeGame.orElse(null);
    }
 
    public Game playMove(Long gameId, Player player, int[][] path) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Partie introuvable."));
 
        if (!game.getStatus().equals("PLAYING")) {
            throw new RuntimeException("La partie n'est pas en cours.");
        }
 
        PlayerColor color;
        if (player.getId().equals(game.getPlayerBlack().getId())) {
            color = PlayerColor.BLACK;
        } else if (player.getId().equals(game.getPlayerWhite().getId())) {
            color = PlayerColor.WHITE;
        } else {
            throw new RuntimeException("Vous ne faites pas partie de cette partie.");
        }
 
        ZammaBoard board = new ZammaBoard(game.getBoardState(), BOARD_SIZE);
        board.playSequence(color, path);

        // 1. Convertir le tableau 2D en String JSON
        String pathString = "[]";
        try {
            ObjectMapper mapper = new ObjectMapper();
            pathString = mapper.writeValueAsString(path);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. Déterminer s'il y a eu capture (Distance de saut > 1 sur le premier pas)
        boolean isCapture = false;
        if (path.length >= 2) {
            int dRow = Math.abs(path[1][0] - path[0][0]);
            int dCol = Math.abs(path[1][1] - path[0][1]);
            isCapture = (dRow > 1 || dCol > 1);
        }

        Move move = new Move();
        move.setGame(game);
        move.setPlayer(player);
        move.setPathJson(pathString);
        move.setCapture(isCapture);
        move.setTimestamp(LocalDateTime.now());
        moveRepository.save(move);

        game.setBoardState(board.toBoardString());

        // 3. Détection automatique de fin de partie (adversaire sans pièces)
        PlayerColor opponentColor = (color == PlayerColor.BLACK) ? PlayerColor.WHITE : PlayerColor.BLACK;
        if (board.countPieces(opponentColor) == 0) {
            game.setStatus("FINISHED");
            game.setEndTime(LocalDateTime.now());
            game.setDraw(false);
            game.setWinner(player);
            player.setCredit(player.getCredit() + 20);
            Player opponent = (color == PlayerColor.BLACK) ? game.getPlayerWhite() : game.getPlayerBlack();
            if (opponent != null) {
                opponent.setCredit(Math.max(0, opponent.getCredit() - 10));
                opponent.setBusy(false);
            }
            player.setBusy(false);
        }

        Game saved = gameRepository.save(game);
        broadcastGame(saved);
        return saved;
    }

    public Game finishGame(Long gameId, Player winner, boolean isDraw) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Partie introuvable."));
 
        game.setStatus("FINISHED");
        game.setEndTime(LocalDateTime.now());
        game.setDraw(isDraw);
 
        if (!isDraw && winner != null) {
            game.setWinner(winner);
            winner.setCredit(winner.getCredit() + 20);
            Player loser = winner.getId().equals(game.getPlayerBlack().getId())
                    ? game.getPlayerWhite()
                    : game.getPlayerBlack();
            if (loser != null) {
                loser.setCredit(Math.max(0, loser.getCredit() - 10));
            }
        }

        game.getPlayerBlack().setBusy(false);
        if (game.getPlayerWhite() != null) {
            game.getPlayerWhite().setBusy(false);
        }

        Game saved = gameRepository.save(game);
        broadcastGame(saved);
        return saved;
    }
 
    public Game findById(Long id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Partie introuvable."));
    }
 
    public List<Game> getWaitingGames() {
        return gameRepository.findByStatus("WAITING");
    }
 
    public List<Move> getMovesForGame(Long gameId) {
        Game game = findById(gameId);
        return moveRepository.findByGameOrderByTimestampAsc(game);
    }

    // ─── Diffusion WebSocket ───────────────────────────────────────────────
    private void broadcastGame(Game game) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("gameId",      game.getId());
        payload.put("status",      game.getStatus());
        payload.put("boardState",  game.getBoardState());
        payload.put("playerBlack", game.getPlayerBlack() != null ? game.getPlayerBlack().getUsername() : null);
        payload.put("playerWhite", game.getPlayerWhite() != null ? game.getPlayerWhite().getUsername() : null);
        payload.put("winner",      game.getWinner()      != null ? game.getWinner().getUsername()      : null);
        ws.convertAndSend("/topic/game/" + game.getId(), payload);
    }

    public List<Map<String, Object>> getHistoryForPlayer(Player player) {
        List<Game> games = gameRepository.findByPlayerBlackOrPlayerWhite(player, player);
        List<Map<String, Object>> result = new ArrayList<>();
        games.stream()
            .filter(g -> "FINISHED".equals(g.getStatus()))
            .sorted((a, b) -> {
                if (a.getEndTime() == null) return 1;
                if (b.getEndTime() == null) return -1;
                return b.getEndTime().compareTo(a.getEndTime());
            })
            .forEach(g -> {
                String opponent;
                if (g.getPlayerBlack() != null && g.getPlayerBlack().getId().equals(player.getId())) {
                    opponent = g.getPlayerWhite() != null ? g.getPlayerWhite().getUsername() : "?";
                } else {
                    opponent = g.getPlayerBlack() != null ? g.getPlayerBlack().getUsername() : "?";
                }
                String outcome;
                if (g.isDraw()) {
                    outcome = "Nul";
                } else if (g.getWinner() != null && g.getWinner().getId().equals(player.getId())) {
                    outcome = "Victoire";
                } else {
                    outcome = "Défaite";
                }
                Map<String, Object> entry = new HashMap<>();
                entry.put("gameId", g.getId());
                entry.put("opponent", opponent);
                entry.put("result", outcome);
                entry.put("date", g.getEndTime() != null ? g.getEndTime().toString() : "");
                result.add(entry);
            });
        return result;
    }
}