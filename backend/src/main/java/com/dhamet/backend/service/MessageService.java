package com.dhamet.backend.service;
 
import com.dhamet.backend.ModelBD.Game;
import com.dhamet.backend.ModelBD.Message;
import com.dhamet.backend.ModelBD.Player;
import com.dhamet.backend.repository.GameRepository;
import com.dhamet.backend.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.time.LocalDateTime;
import java.util.List;
 
@Service
@Transactional
public class MessageService {
 
    @Autowired
    private MessageRepository messageRepository;
 
    @Autowired
    private GameRepository gameRepository;
 
    public Message sendMessage(Player sender, Long gameId, String content) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Partie introuvable."));
 
        // Vérifier que le joueur fait partie de la partie
        boolean isParticipant = sender.getId().equals(game.getPlayerBlack().getId())
                || (game.getPlayerWhite() != null && sender.getId().equals(game.getPlayerWhite().getId()));
 
        if (!isParticipant) {
            throw new RuntimeException("Vous ne faites pas partie de cette partie.");
        }
 
        Message message = new Message();
        message.setSender(sender);
        message.setGame(game);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());
        return messageRepository.save(message);
    }
 
    public List<Message> getChat(Long gameId) {
        return messageRepository.findByGameId(gameId);
    }
}
 