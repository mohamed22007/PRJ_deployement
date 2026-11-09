package com.dhamet.backend.repository;
 
import com.dhamet.backend.ModelBD.Move;
import com.dhamet.backend.ModelBD.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
import java.util.List;
 
@Repository
public interface MoveRepository extends JpaRepository<Move, Long> {
    List<Move> findByGameOrderByTimestampAsc(Game game);
    List<Move> findByGameId(Long gameId);
}