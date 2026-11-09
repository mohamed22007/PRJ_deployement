import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Player } from '../../models/Player';
import { PlayerService } from '../../services/player_services';

@Component({
  selector: 'app-inscription',
  standalone: true, 
  imports: [FormsModule],
  templateUrl: './inscription.html',
  styleUrl: './inscription.css',
})
export class Inscription {

    newPlayer: Player = { 
      username: '', 
      nom: '', 
      email: '', 
      password: ''
    };

    constructor(private router: Router, private playerService: PlayerService) {}

    retour() {
      this.router.navigate(['/auth/login']);
    }

    onCreate() {
      if (this.newPlayer.username && this.newPlayer.password) {
        this.playerService.register(this.newPlayer).subscribe({
          next: () => {
            alert('Inscription réussie ! Connectez-vous.');
            this.router.navigate(['/auth/login']);
          },
          error: (err) => {
            alert("Erreur lors de l'inscription : " + err.error?.error);
          }
        });
      }
    }
}