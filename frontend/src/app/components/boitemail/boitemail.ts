import { Component, OnInit, OnDestroy, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MailService } from '../../services/MailService';
import { Mail } from '../../models/Mail';

@Component({
  selector: 'app-boitemail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './boitemail.html',
  styleUrl: './boitemail.css',
})
export class Boitemail implements OnInit, OnDestroy {
  inbox: Mail[] = [];
  openMail: Mail | null = null;
  isLoading = false;
  erreur = '';
  private refreshInterval: any;

  constructor(
    private mailService: MailService,
    private router: Router,
    private zone: NgZone,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.chargerMails();
    // Utilise zone.run pour garantir la détection de changements Angular
    this.refreshInterval = setInterval(() => {
      this.zone.run(() => this.chargerMails());
    }, 20000);
  }

  ngOnDestroy() {
    clearInterval(this.refreshInterval);
  }

  get unreadCount(): number {
    return this.inbox.filter(m => !m.isRead).length;
  }

  chargerMails() {
    this.erreur = '';
    this.isLoading = true;
    this.mailService.getInbox().subscribe({
      next: (mails) => {
        this.inbox = mails;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.erreur = 'Impossible de charger les mails. Vérifiez que vous êtes connecté.';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  ouvrirMail(mail: Mail) {
    if (this.openMail?.id === mail.id) {
      // Fermeture : retirer le mail lu de l'affichage
      if (mail.isRead) {
        this.inbox = this.inbox.filter(m => m.id !== mail.id);
      }
      this.openMail = null;
      this.cdr.detectChanges();
      return;
    }

    this.openMail = mail;
    this.cdr.detectChanges();

    if (mail.id && !mail.isRead) {
      this.mailService.markAsRead(mail.id).subscribe({
        next: () => {
          mail.isRead = true;
          // Invitation → suppression depuis la base de données
          if (this.extractGameToken(mail) && mail.id) {
            this.mailService.deleteMail(mail.id).subscribe();
          }
          this.cdr.detectChanges();
        }
      });
    }
  }

  extractGameToken(mail: Mail): string | null {
    if (!mail.subject && !mail.content) return null;
    const source = (mail.subject || '') + ' ' + (mail.content || '');
    const match = source.match(/[Cc]ode\s*[:：]\s*([A-Z0-9]{4,8})/);
    return match ? match[1] : null;
  }

  rejoindreDepuisMail(token: string, event: Event) {
    event.stopPropagation();
    sessionStorage.setItem('pendingGameToken', token);
    this.router.navigate(['/home/game']);
  }
}
