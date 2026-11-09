package com.dhamet.backend.service;

import com.dhamet.backend.ModelBD.Mail;
import com.dhamet.backend.ModelBD.Player;
import com.dhamet.backend.repository.MailRepository;
import com.dhamet.backend.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class MailService {

    @Autowired
    private MailRepository mailRepository;

    @Autowired
    private PlayerRepository playerRepository;

    public Mail sendMail(Player sender, Long receiverId, String subject, String content) {
        if (subject == null || subject.isBlank()) throw new RuntimeException("Sujet requis.");
        if (content == null || content.isBlank()) throw new RuntimeException("Contenu requis.");
        if (content.length() > 1000) throw new RuntimeException("Contenu trop long (max 1000 caractères).");

        Player receiver = playerRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Destinataire introuvable."));

        Mail mail = new Mail();
        mail.setSender(sender);
        mail.setReceiver(receiver);
        mail.setSubject(subject.trim());
        mail.setContent(content.trim());
        mail.setSentDate(LocalDateTime.now());
        mail.setRead(false);
        return mailRepository.save(mail);
    }

    public List<Mail> getInbox(Player player) {
        return mailRepository.findByReceiverAndIsReadFalse(player);
    }

    public List<Mail> getUnread(Player player) {
        return mailRepository.findByReceiverAndIsReadFalse(player);
    }

    public Mail markAsRead(Long mailId, Player player) {
        Mail mail = mailRepository.findById(mailId)
                .orElseThrow(() -> new RuntimeException("Mail introuvable."));
        if (!mail.getReceiver().getId().equals(player.getId())) {
            throw new RuntimeException("Ce mail ne vous appartient pas.");
        }
        mail.setRead(true);
        return mailRepository.save(mail);
    }

    public void deleteMail(Long mailId, Player player) {
        Mail mail = mailRepository.findById(mailId)
                .orElseThrow(() -> new RuntimeException("Mail introuvable."));
        if (!mail.getReceiver().getId().equals(player.getId())) {
            throw new RuntimeException("Ce mail ne vous appartient pas.");
        }
        mailRepository.delete(mail);
    }

    public long countUnread(Player player) {
        return mailRepository.countByReceiverAndIsReadFalse(player);
    }
}
