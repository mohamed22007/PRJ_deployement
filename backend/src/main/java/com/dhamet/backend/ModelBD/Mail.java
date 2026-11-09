package com.dhamet.backend.ModelBD;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mails")
public class Mail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String subject;
    
    @Column(length = 1000)
    private String content;
    
    private LocalDateTime sentDate;
    private boolean isRead;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private Player sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private Player receiver;

    public Mail() {
    }

    // --- Getters et Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getSentDate() { return sentDate; }
    public void setSentDate(LocalDateTime sentDate) { this.sentDate = sentDate; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public Player getSender() { return sender; }
    public void setSender(Player sender) { this.sender = sender; }

    public Player getReceiver() { return receiver; }
    public void setReceiver(Player receiver) { this.receiver = receiver; }
}