package com.dhamet.backend.ModelBD;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Entity
@Table(name = "games")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String joinToken;

    @ManyToOne
    @JoinColumn(name = "player_black_id")
    private Player playerBlack;

    @ManyToOne
    @JoinColumn(name = "player_white_id")
    private Player playerWhite;

    @Column(nullable = false)
    private String status; // "WAITING", "PLAYING", "FINISHED"

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private Player winner;

    private boolean isDraw;

    @Column(columnDefinition = "TEXT")
    private String boardState;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> chat = new ArrayList<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Move> moves = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.joinToken == null) {
            this.joinToken = String.valueOf(10000 + new Random().nextInt(90000));
        }
        if (this.status == null) {
            this.status = "WAITING";
        }
        this.startTime = LocalDateTime.now();
    }

    // --- Getters et Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getJoinToken() { return joinToken; }
    public void setJoinToken(String joinToken) { this.joinToken = joinToken; }

    public Player getPlayerBlack() { return playerBlack; }
    public void setPlayerBlack(Player playerBlack) { this.playerBlack = playerBlack; }

    public Player getPlayerWhite() { return playerWhite; }
    public void setPlayerWhite(Player playerWhite) { this.playerWhite = playerWhite; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public Player getWinner() { return winner; }
    public void setWinner(Player winner) { this.winner = winner; }

    public boolean isDraw() { return isDraw; }
    public void setDraw(boolean draw) { isDraw = draw; }

    public String getBoardState() { return boardState; }
    public void setBoardState(String boardState) { this.boardState = boardState; }

    public List<Message> getChat() { return chat; }
    public void setChat(List<Message> chat) { this.chat = chat; }

    public List<Move> getMoves() { return moves; }
    public void setMoves(List<Move> moves) { this.moves = moves; }
}