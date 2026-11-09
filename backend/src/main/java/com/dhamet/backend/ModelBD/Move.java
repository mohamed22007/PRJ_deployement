package com.dhamet.backend.ModelBD;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "moves")
public class Move {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Stocke le chemin complet sous forme JSON, ex: "[[2,2],[4,4],[6,6]]"
    @Column(columnDefinition = "TEXT")
    private String pathJson;
    
    private boolean isCapture;
    private LocalDateTime timestamp;

    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player player;

    public Move() {
    }

    // --- Getters et Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPathJson() { return pathJson; }
    public void setPathJson(String pathJson) { this.pathJson = pathJson; }

    public boolean isCapture() { return isCapture; }
    public void setCapture(boolean capture) { this.isCapture = capture; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Game getGame() { return game; }
    public void setGame(Game game) { this.game = game; }

    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }
}