package com.dhamet.backend.repository;
 
import com.dhamet.backend.ModelBD.Mail;
import com.dhamet.backend.ModelBD.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
import java.util.List;
 
@Repository
public interface MailRepository extends JpaRepository<Mail, Long> {
    List<Mail> findByReceiver(Player receiver);
    List<Mail> findBySender(Player sender);
    List<Mail> findByReceiverAndIsReadFalse(Player receiver);
    long countByReceiverAndIsReadFalse(Player receiver);
}