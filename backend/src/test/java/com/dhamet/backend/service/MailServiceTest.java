package com.dhamet.backend.service;

import com.dhamet.backend.ModelBD.Mail;
import com.dhamet.backend.ModelBD.Player;
import com.dhamet.backend.repository.MailRepository;
import com.dhamet.backend.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock private MailRepository mailRepository;
    @Mock private PlayerRepository playerRepository;

    @InjectMocks
    private MailService mailService;

    private static Player joueur(Long id) {
        Player p = new Player();
        p.setId(id);
        p.setUsername("j" + id);
        return p;
    }

    @Test
    void sendMail_creeUnMailNonLu() {
        Player a = joueur(1L), b = joueur(2L);
        when(playerRepository.findById(2L)).thenReturn(Optional.of(b));
        when(mailRepository.save(any(Mail.class))).thenAnswer(inv -> inv.getArgument(0));

        Mail m = mailService.sendMail(a, 2L, "  Salut ", " Partie ce soir ? ");

        assertSame(a, m.getSender());
        assertSame(b, m.getReceiver());
        assertEquals("Salut", m.getSubject());
        assertEquals("Partie ce soir ?", m.getContent());
        assertFalse(m.isRead());
    }

    @Test
    void sendMail_refuseUnSujetVide() {
        assertThrows(RuntimeException.class, () -> mailService.sendMail(joueur(1L), 2L, " ", "texte"));
    }

    @Test
    void sendMail_refuseUnContenuDePlusDe1000Caracteres() {
        String tropLong = "a".repeat(1001);
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> mailService.sendMail(joueur(1L), 2L, "sujet", tropLong));
        assertTrue(e.getMessage().contains("trop long"));
    }

    @Test
    void sendMail_destinataireInconnu() {
        when(playerRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> mailService.sendMail(joueur(1L), 99L, "s", "c"));
    }

    @Test
    void markAsRead_parLeDestinataire() {
        Player b = joueur(2L);
        Mail m = new Mail();
        m.setReceiver(b);
        when(mailRepository.findById(5L)).thenReturn(Optional.of(m));
        when(mailRepository.save(m)).thenReturn(m);

        assertTrue(mailService.markAsRead(5L, b).isRead());
    }

    @Test
    void markAsRead_refuseSiLeMailNeNousAppartientPas() {
        Mail m = new Mail();
        m.setReceiver(joueur(2L));
        when(mailRepository.findById(5L)).thenReturn(Optional.of(m));

        assertThrows(RuntimeException.class, () -> mailService.markAsRead(5L, joueur(3L)));
        verify(mailRepository, never()).save(any());
    }

    @Test
    void deleteMail_refuseSiLeMailNeNousAppartientPas() {
        Mail m = new Mail();
        m.setReceiver(joueur(2L));
        when(mailRepository.findById(5L)).thenReturn(Optional.of(m));

        assertThrows(RuntimeException.class, () -> mailService.deleteMail(5L, joueur(3L)));
        verify(mailRepository, never()).delete(any());
    }
}
