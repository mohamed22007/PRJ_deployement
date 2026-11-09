package com.dhamet.backend.ModelBD;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String content;
    private LocalDateTime timestamp;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private Player sender;

    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

    public Message() {
    }

    // --- Getters et Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Player getSender() { return sender; }
    public void setSender(Player sender) { this.sender = sender; }

    public Game getGame() { return game; }
    public void setGame(Game game) { this.game = game; }
}