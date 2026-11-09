import { Component, OnInit, OnDestroy, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GameService } from '../../services/GameService';
import { PlayerService } from '../../services/player_services';

interface GameHistory {
  gameId: number;
  opponent: string;
  result: string;
  date: string;
}

interface RankingEntry {
  username: string;
  nom: string;
  credit: number;
}

@Component({
  selector: 'app-histoire',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './histoire.html',
  styleUrl: './histoire.css',
})
export class Histoire implements OnInit, OnDestroy {
  activeTab: 'history' | 'ranking' = 'history';
  history: GameHistory[] = [];
  ranking: RankingEntry[] = [];
  myUsername = '';
  loading = false;
  private refreshInterval: any;

  constructor(
    private gameService: GameService,
    private playerService: PlayerService,
    private zone: NgZone,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.playerService.currentUser$.subscribe(user => {
      if (user) this.myUsername = user.username || '';
    });
    this.loadHistory();
    this.loadRanking();
    this.refreshInterval = setInterval(() => {
      this.zone.run(() => {
        this.loadHistory();
        this.loadRanking();
      });
    }, 30000);
  }

  ngOnDestroy() {
    clearInterval(this.refreshInterval);
  }

  loadHistory() {
    this.loading = true;
    this.gameService.getHistory().subscribe({
      next: (data) => {
        this.history = data;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  loadRanking() {
    this.playerService.getRanking().subscribe({
      next: (data) => {
        this.ranking = data;
        this.cdr.detectChanges();
      },
      error: () => {}
    });
  }

  switchTab(tab: 'history' | 'ranking') {
    this.activeTab = tab;
    this.cdr.detectChanges();
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '-';
    const d = new Date(dateStr);
    return d.toLocaleDateString('fr-FR', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }
}
