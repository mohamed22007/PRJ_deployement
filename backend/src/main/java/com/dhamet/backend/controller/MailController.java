package com.dhamet.backend.controller;

import com.dhamet.backend.ModelBD.Mail;
import com.dhamet.backend.ModelBD.Player;
import com.dhamet.backend.service.MailService;
import com.dhamet.backend.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mails")
@CrossOrigin(origins = {"http://localhost:4200"})
public class MailController {

    @Autowired
    private MailService mailService;

    @Autowired
    private PlayerService playerService;

    /** POST /api/mails/send */
    @PostMapping("/send")
    public ResponseEntity<?> sendMail(
            @RequestHeader("X-Session-Token") String token,
            @RequestBody Map<String, Object> body) {
        try {
            if (body.get("receiverId") == null || body.get("subject") == null || body.get("content") == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Champs manquants."));
            }
            Player sender = playerService.getPlayerFromToken(token);
            Long receiverId = Long.valueOf(body.get("receiverId").toString());
            Mail mail = mailService.sendMail(sender, receiverId,
                    body.get("subject").toString(),
                    body.get("content").toString());
            return ResponseEntity.ok(Map.of("mailId", mail.getId(), "message", "Mail envoyé"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/mails/inbox — retourne seulement les mails non lus */
    @GetMapping("/inbox")
    public ResponseEntity<?> getInbox(@RequestHeader("X-Session-Token") String token) {
        try {
            Player player = playerService.getPlayerFromToken(token);
            List<Mail> mails = mailService.getInbox(player);
            return ResponseEntity.ok(mails.stream().map(m -> {
                java.util.Map<String, Object> mailMap = new java.util.HashMap<>();
                mailMap.put("id", m.getId());
                mailMap.put("subject", m.getSubject() != null ? m.getSubject() : "");
                mailMap.put("sender", m.getSender().getUsername());
                mailMap.put("sentDate", m.getSentDate().toString());
                mailMap.put("isRead", m.isRead());
                mailMap.put("content", m.getContent() != null ? m.getContent() : "");
                return mailMap;
            }).toList());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/mails/unread-count */
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(@RequestHeader("X-Session-Token") String token) {
        try {
            Player player = playerService.getPlayerFromToken(token);
            return ResponseEntity.ok(Map.of("count", mailService.countUnread(player)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** PUT /api/mails/{mailId}/read */
    @PutMapping("/{mailId}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long mailId,
            @RequestHeader("X-Session-Token") String token) {
        try {
            Player player = playerService.getPlayerFromToken(token);
            mailService.markAsRead(mailId, player);
            return ResponseEntity.ok(Map.of("message", "Mail marqué comme lu"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** DELETE /api/mails/{mailId} */
    @DeleteMapping("/{mailId}")
    public ResponseEntity<?> deleteMail(
            @PathVariable Long mailId,
            @RequestHeader("X-Session-Token") String token) {
        try {
            Player player = playerService.getPlayerFromToken(token);
            mailService.deleteMail(mailId, player);
            return ResponseEntity.ok(Map.of("message", "Mail supprimé"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
