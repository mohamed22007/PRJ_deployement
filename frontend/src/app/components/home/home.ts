import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from "@angular/router";
import { PlayerService } from '../../services/player_services';
import { Player } from '../../models/Player';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class HomeComponent implements OnInit {
  user: Player | null = null;
  
  constructor(private playerService: PlayerService, private router: Router) {}
  
  ngOnInit() {
    this.playerService.currentUser$.subscribe(data => {
      this.user = data;
    });
  }

  logout() {
    this.playerService.logout().subscribe({
      next: () => this.router.navigate(['/auth/login']),
      error: () => {
        sessionStorage.removeItem('sessionToken');
        this.router.navigate(['/auth/login']);
      }
    });
  }
}