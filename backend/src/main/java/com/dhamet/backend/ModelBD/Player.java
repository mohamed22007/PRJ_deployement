package com.dhamet.backend.ModelBD;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "players")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String nom;
    private String email;
    private int credit;
    private boolean busy;

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Session> sessions = new ArrayList<>();

    // Parties où ce joueur joue les noirs
    @OneToMany(mappedBy = "playerBlack")
    private List<Game> gamesAsBlack = new ArrayList<>();

    // Parties où ce joueur joue les blancs
    @OneToMany(mappedBy = "playerWhite")
    private List<Game> gamesAsWhite = new ArrayList<>();

    // Parties gagnées
    @OneToMany(mappedBy = "winner")
    private List<Game> wonGames = new ArrayList<>();

    // Mails envoyés/reçus
    @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL)
    private List<Mail> sentMails = new ArrayList<>();

    @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL)
    private List<Mail> receivedMails = new ArrayList<>();

    public Player() {}

    // --- Getters et Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getCredit() { return credit; }
    public void setCredit(int credit) { this.credit = credit; }

    public boolean isBusy() { return busy; }
    public void setBusy(boolean busy) { this.busy = busy; }

    public List<Session> getSessions() { return sessions; }
    public void setSessions(List<Session> sessions) { this.sessions = sessions; }

    public List<Game> getGamesAsBlack() { return gamesAsBlack; }
    public void setGamesAsBlack(List<Game> gamesAsBlack) { this.gamesAsBlack = gamesAsBlack; }

    public List<Game> getGamesAsWhite() { return gamesAsWhite; }
    public void setGamesAsWhite(List<Game> gamesAsWhite) { this.gamesAsWhite = gamesAsWhite; }

    public List<Game> getWonGames() { return wonGames; }
    public void setWonGames(List<Game> wonGames) { this.wonGames = wonGames; }

    public List<Mail> getSentMails() { return sentMails; }
    public void setSentMails(List<Mail> sentMails) { this.sentMails = sentMails; }

    public List<Mail> getReceivedMails() { return receivedMails; }
    public void setReceivedMails(List<Mail> receivedMails) { this.receivedMails = receivedMails; }
}