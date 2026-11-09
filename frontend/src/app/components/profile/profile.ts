import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { PlayerService } from '../../services/player_services';
import { Player } from '../../models/Player';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './profile.html',
  styleUrls: ['./profile.css']
})
export class Profile implements OnInit {

  jouer: Player | null = null;
  stickers = { label: '', color: '' };

  constructor(private playerService: PlayerService, private router: Router) {}

  ngOnInit(): void {
    this.playerService.currentUser$.subscribe(data => {
      this.jouer = data;
      if (this.jouer?.busy) {
        this.stickers = { label: 'En Partie', color: '#ff0000' };
      } else {
        this.stickers = { label: 'Disponible', color: '#00dc0b' };
      }
    });
  }

  deconnecter(): void {
    this.playerService.logout().subscribe({
      next: () => this.router.navigate(['/auth/login']),
      error: () => {
        // Même en cas d'erreur réseau, nettoyer localement
        sessionStorage.removeItem('sessionToken');
        this.router.navigate(['/auth/login']);
      }
    });
  }
}
