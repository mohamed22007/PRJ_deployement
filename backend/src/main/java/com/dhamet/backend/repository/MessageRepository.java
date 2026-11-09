package com.dhamet.backend.repository;
 
import com.dhamet.backend.ModelBD.Message;
import com.dhamet.backend.ModelBD.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
import java.util.List;
 
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByGameOrderByTimestampAsc(Game game);
    List<Message> findByGameId(Long gameId);
}