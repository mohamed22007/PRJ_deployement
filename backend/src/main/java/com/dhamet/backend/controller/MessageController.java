package com.dhamet.backend.controller;
 
import com.dhamet.backend.ModelBD.Message;
import com.dhamet.backend.ModelBD.Player;
import com.dhamet.backend.service.MessageService;
import com.dhamet.backend.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
 
@RestController
@RequestMapping("/api/games")
@CrossOrigin(origins = {"http://localhost:4200"})
public class MessageController {
 
    @Autowired
    private MessageService messageService;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private SimpMessagingTemplate ws;
 
    /** POST /api/games/{gameId}/chat */
    @PostMapping("/{gameId}/chat")
    public ResponseEntity<?> sendMessage(
            @PathVariable Long gameId,
            @RequestHeader("X-Session-Token") String token,
            @RequestBody Map<String, String> body) {
        try {
            Player player = playerService.getPlayerFromToken(token);
            Message message = messageService.sendMessage(player, gameId, body.get("content"));
            Map<String, Object> resp = new HashMap<>();
            resp.put("messageId", message.getId());
            resp.put("sender", player.getUsername());
            resp.put("content", message.getContent() != null ? message.getContent() : "");
            resp.put("timestamp", message.getTimestamp().toString());
            // Diffusion WebSocket vers les deux joueurs
            ws.convertAndSend("/topic/chat/" + gameId, resp);
            return ResponseEntity.ok(resp);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
 
    /** GET /api/games/{gameId}/chat */
    @GetMapping("/{gameId}/chat")
    public ResponseEntity<?> getChat(
            @PathVariable Long gameId,
            @RequestHeader("X-Session-Token") String token) {
        try {
            playerService.getPlayerFromToken(token);
            List<Message> messages = messageService.getChat(gameId);
            return ResponseEntity.ok(messages.stream().map(m -> {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("sender", m.getSender() != null ? m.getSender().getUsername() : "");
                map.put("content", m.getContent() != null ? m.getContent() : "");
                map.put("timestamp", m.getTimestamp() != null ? m.getTimestamp().toString() : "");
                return map;
            }).toList());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
 