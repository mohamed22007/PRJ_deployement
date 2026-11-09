import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PlayerService } from '../../services/player_services'; // Adaptez le chemin

@Component({
  selector: 'app-login',
  standalone: true, 
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrls: ['./login.css']
})
export class LoginComponent {
  username = '';
  password = '';

  constructor(private router: Router, private playerService: PlayerService) {}

  onLogin() {
    this.playerService.login(this.username, this.password).subscribe({
      next: () => {
        // Le token est sauvegardé par le service, on redirige vers l'accueil
        this.router.navigate(['/home']);
      },
      error: () => {
        alert('Identifiant ou mot de passe incorrect');
      }
    });
  }

  onRegistre() {
    this.router.navigate(['/auth/inscription']);
  }
}