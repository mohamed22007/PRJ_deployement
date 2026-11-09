package com.dhamet.backend.repository;
 
import com.dhamet.backend.ModelBD.Session;
import com.dhamet.backend.ModelBD.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
import java.util.Optional;
import java.util.List;
 
@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    Optional<Session> findByToken(String token);
    Optional<Session> findByPlayerAndIsActiveTrue(Player player);
    List<Session> findByPlayer(Player player);
}