package com.dhamet.backend.repository;
 
import com.dhamet.backend.ModelBD.Game;
import com.dhamet.backend.ModelBD.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
import java.util.List;
import java.util.Optional;
 
@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    Optional<Game> findByJoinToken(String joinToken);
    Optional<Game> findByJoinTokenIgnoreCase(String joinToken);
    List<Game> findByStatus(String status);
    List<Game> findByPlayerBlackOrPlayerWhite(Player playerBlack, Player playerWhite);
}