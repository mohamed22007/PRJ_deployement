package com.dhamet.backend.ModelBD;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sessions")
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String token;

    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private boolean isActive;

    @ManyToOne
    private Player player;

    @PrePersist
    protected void onCreate() {
        if (this.token == null) {
            this.token = generateSecureToken();
        }
        if (this.loginTime == null) {
            this.loginTime = LocalDateTime.now();
        }
        this.isActive = true;
    }

    // Utilisation d'un UUID pour garantir une sécurité maximale
    private String generateSecureToken() {
        return UUID.randomUUID().toString();
    }

    // --- Getters et Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public LocalDateTime getLoginTime() { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }

    public LocalDateTime getLogoutTime() { return logoutTime; }
    public void setLogoutTime(LocalDateTime logoutTime) { this.logoutTime = logoutTime; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }

    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }
}