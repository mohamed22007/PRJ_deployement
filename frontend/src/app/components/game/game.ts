import { Component, OnInit, OnDestroy, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, interval, Subscription } from 'rxjs';
import { switchMap, catchError, takeUntil } from 'rxjs/operators';
import { of } from 'rxjs';
import { GameService } from '../../services/GameService';
import { PlayerService } from '../../services/player_services';
import { MessageService } from '../../services/MessageService';
import { MailService } from '../../services/MailService';
import { WsService } from '../../services/WsService';
import { Game } from '../../models/Game';
import { Message } from '../../models/Message';
import { PlateauComponent } from './plateau/plateau';
import { GameChatComponent } from './chat/chat';

@Component({
  selector: 'app-game',
  standalone: true,
  imports: [CommonModule, FormsModule, PlateauComponent, GameChatComponent],
  templateUrl: './game.html',
  styleUrl: './game.css',
})
export class GameComponent implements OnInit, OnDestroy {

  readonly taille = 9;
  carres = Array((this.taille - 1) ** 2).fill(0).map((_, i) => i);
  plateau: number[] = Array(this.taille * this.taille).fill(0);

  // ── Sélection / mouvement ────────────────────────────────────────────
  piececlick: number | null = null;
  validMoveTargets: number[] = [];
  mustCaptureIndices: number[] = [];

  // ── Séquence de capture ───────────────────────────────────────────────
  currentPath: number[][] = [];
  pendingCapture = false;
  lastMovedIdx: number | null = null;

  // ── État de la partie ─────────────────────────────────────────────────
  gameState: 'MENU' | 'WAITING' | 'PLAYING' | 'FINISHED' = 'MENU';
  joinTokenInput = '';
  currentGame: Game | null = null;

  myUsername = '';
  myColor: 'black' | 'white' | null = null;
  currentTurn: 'black' | 'white' = 'black';
  isMyTurn = false;

  gameResult = '';
  moveError = '';
  creating = false;

  // ── Chat ──────────────────────────────────────────────────────────────
  messages: Message[] = [];

  // ── Invitation ───────────────────────────────────────────────────────
  showInviteForm = false;
  inviteUsername = '';
  inviteStatus = '';

  private destroy$ = new Subject<void>();
  private pollingSub: Subscription | null = null;
  private chatSub: Subscription | null = null;
  private wsPollSub: Subscription | null = null;
  private wsChatSub: Subscription | null = null;

  constructor(
    private gameService: GameService,
    private playerService: PlayerService,
    private messageService: MessageService,
    private mailService: MailService,
    private wsService: WsService,
    private ngZone: NgZone,
    private cdr: ChangeDetectorRef
  ) {}

  // ═════════════════════════════════════════════════════════════════════
  // Cycle de vie
  // ═════════════════════════════════════════════════════════════════════

  ngOnInit() {
    this.playerService.currentUser$.subscribe(user => {
      if (user) { this.myUsername = user.username; this.updateMyColor(); }
    });

    const token = sessionStorage.getItem('sessionToken');
    if (token) this.wsService.connect(token);

    const pendingToken = sessionStorage.getItem('pendingGameToken');
    if (pendingToken) {
      sessionStorage.removeItem('pendingGameToken');
      this.joinTokenInput = pendingToken;
      setTimeout(() => this.rejoindrePartie(), 300);
    }

    this.gameService.getCurrentGame().subscribe({
      next: (res) => {
        if (res.hasGame) {
          this.currentGame = { gameId: res.gameId, joinToken: res.joinToken, status: res.status } as Game;
          this.gameState = res.status as typeof this.gameState;
          this.chargerPlateau(res.gameId);
          this.startPolling(res.gameId);
          if (res.status === 'PLAYING') this.startChatPolling(res.gameId);
        }
      },
      error: () => {}
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    this.stopPolling();
    this.chatSub?.unsubscribe();
    this.wsChatSub?.unsubscribe();
    this.wsService.disconnect();
  }

  // ═════════════════════════════════════════════════════════════════════
  // Actions lobby
  // ═════════════════════════════════════════════════════════════════════

  creerPartie() {
    if (this.creating) return;
    this.creating = true;
    this.gameService.createGame().subscribe({
      next: (game) => {
        this.currentGame = game;
        this.gameState = 'WAITING';
        this.creating = false;
        this.startPolling(game.gameId);
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.creating = false;
        alert('Erreur création : ' + (err.error?.error ?? err.message));
      }
    });
  }

  rejoindrePartie() {
    const token = (this.joinTokenInput || '').trim().toUpperCase();
    if (!token) return;
    this.gameService.joinGame(token).subscribe({
      next: (game) => {
        this.currentGame = game;
        this.gameState = 'PLAYING';
        this.updateMyColor();
        if (game.boardState) this.parseBoardState(game.boardState);
        this.startPolling(game.gameId);
        this.startChatPolling(game.gameId);
      },
      error: (err) => alert('Code invalide : ' + (err.error?.error ?? err.message))
    });
  }

  quitterPartie() {
    if (!this.currentGame?.gameId) return;
    this.gameService.resign(this.currentGame.gameId).subscribe({
      next: () => this.resetGame(),
      error: () => {}
    });
  }

  resetGame() {
    this.stopPolling();
    this.chatSub?.unsubscribe();    this.chatSub    = null;
    this.wsChatSub?.unsubscribe();  this.wsChatSub  = null;
    this.gameState = 'MENU';
    this.currentGame = null;
    this.myColor = null;
    this.currentTurn = 'black';
    this.isMyTurn = false;
    this.piececlick = null;
    this.validMoveTargets = [];
    this.currentPath = [];
    this.pendingCapture = false;
    this.mustCaptureIndices = [];
    this.moveError = '';
    this.plateau = Array(this.taille * this.taille).fill(0);
    this.messages = [];
    this.gameResult = '';
    this.showInviteForm = false;
    this.inviteStatus = '';
  }

  // ═════════════════════════════════════════════════════════════════════
  // Polling
  // ═════════════════════════════════════════════════════════════════════

  private startPolling(gameId: number) {
    this.stopPolling();

    // WebSocket : mise à jour instantanée
    this.wsPollSub = this.wsService.watch<Game>(`/topic/game/${gameId}`)
      .pipe(takeUntil(this.destroy$))
      .subscribe(game => this.ngZone.run(() => this.handleGameUpdate(game)));

    // Polling HTTP de secours (10 s) — rattrape les événements WS manqués
    this.pollingSub = this.ngZone.run(() =>
      interval(10000).pipe(
        takeUntil(this.destroy$),
        switchMap(() => this.gameService.getGame(gameId).pipe(catchError(() => of(null))))
      ).subscribe(game => {
        if (game) this.ngZone.run(() => this.handleGameUpdate(game));
      })
    );
  }

  stopPolling() {
    this.pollingSub?.unsubscribe();   this.pollingSub = null;
    this.wsPollSub?.unsubscribe();    this.wsPollSub  = null;
  }

  private startChatPolling(gameId: number) {
    this.chatSub?.unsubscribe();
    this.wsChatSub?.unsubscribe();

    // WebSocket : messages de l'adversaire en temps réel (les miens arrivent via REST)
    this.wsChatSub = this.wsService.watch<any>(`/topic/chat/${gameId}`)
      .pipe(takeUntil(this.destroy$))
      .subscribe(msg => this.ngZone.run(() => {
        if (msg?.sender && msg.sender !== this.myUsername) {
          this.messages = [...this.messages, { sender: msg.sender, content: msg.content, timestamp: msg.timestamp }];
          this.cdr.detectChanges();
        }
      }));

    // Polling HTTP de secours (15 s) — garde la liste synchronisée
    this.chatSub = this.ngZone.run(() =>
      interval(15000).pipe(
        takeUntil(this.destroy$),
        switchMap(() => this.messageService.getChat(gameId).pipe(catchError(() => of([]))))
      ).subscribe(msgs => {
        this.ngZone.run(() => { this.messages = msgs; this.cdr.detectChanges(); });
      })
    );
  }

  chargerPlateau(gameId: number) {
    this.gameService.getGame(gameId).subscribe({
      next: (game) => this.ngZone.run(() => this.handleGameUpdate(game)),
      error: () => {}
    });
  }

  private handleGameUpdate(game: Game) {
    if (!this.currentGame) {
      this.currentGame = game;
    } else {
      this.currentGame.status = game.status;
      this.currentGame.boardState = game.boardState;
      if (game.playerBlack) this.currentGame.playerBlack = game.playerBlack;
      if (game.playerWhite) this.currentGame.playerWhite = game.playerWhite;
      if (game.winner)      this.currentGame.winner      = game.winner;
    }

    this.updateMyColor();

    if (game.status === 'PLAYING' && this.gameState !== 'PLAYING' && this.gameState !== 'FINISHED') {
      this.gameState = 'PLAYING';
      if (this.currentGame?.gameId) this.startChatPolling(this.currentGame.gameId);
    }

    if (game.status === 'FINISHED' && this.gameState !== 'FINISHED') {
      this.gameState = 'FINISHED';
      this.stopPolling();
      this.chatSub?.unsubscribe(); this.chatSub = null;
      this.gameResult = game.winner === this.myUsername
        ? 'Vous avez gagné ! +20 crédits'
        : game.winner ? 'Vous avez perdu. -10 crédits' : 'Match nul !';
      this.playerService.refreshProfile();
    }

    if (game.boardState && game.status === 'PLAYING' && !this.pendingCapture) {
      this.parseBoardState(game.boardState);
    }

    this.cdr.detectChanges();
  }

  // ═════════════════════════════════════════════════════════════════════
  // Plateau
  // ═════════════════════════════════════════════════════════════════════

  private parseBoardState(boardString: string) {
    const parts = boardString.split('.');
    const gridRows = parts[0].split(';').filter(r => r.length > 0);
    const next = Array(this.taille * this.taille).fill(0);
    let idx = 0;
    for (let r = 0; r < this.taille; r++) {
      const cols = gridRows[r]?.split(',') ?? [];
      for (let c = 0; c < this.taille; c++) {
        next[idx++] = parseInt(cols[c]?.trim() || '0') || 0;
      }
    }
    this.plateau = next;
    if (parts.length >= 3 && !this.pendingCapture) {
      this.currentTurn = parts[2].trim() === 'B' ? 'black' : 'white';
      this.isMyTurn = this.myColor === this.currentTurn;
    }
    this.refreshMustCapture();
  }

  private updateMyColor() {
    if (!this.currentGame || !this.myUsername) return;
    if (this.currentGame.playerBlack === this.myUsername)      this.myColor = 'black';
    else if (this.currentGame.playerWhite === this.myUsername) this.myColor = 'white';
    this.isMyTurn = this.myColor === this.currentTurn;
    this.refreshMustCapture();
  }

  private refreshMustCapture() {
    if (!this.isMyTurn || this.gameState !== 'PLAYING' || !this.myColor) {
      this.mustCaptureIndices = []; return;
    }
    const result: number[] = [];
    for (let i = 0; i < this.plateau.length; i++) {
      if (this.isMyPiece(this.plateau[i])) {
        const r = Math.floor(i / this.taille), c = i % this.taille;
        if (this.canCaptureFrom(r, c, this.plateau)) result.push(i);
      }
    }
    this.mustCaptureIndices = result;
  }

  // Compteurs de pièces pour l'affichage
  get blackPieceCount(): number { return this.plateau.filter(v => v === 1 || v === 3).length; }
  get whitePieceCount(): number { return this.plateau.filter(v => v === 2 || v === 4).length; }

  // ═════════════════════════════════════════════════════════════════════
  // Logique — helpers
  // ═════════════════════════════════════════════════════════════════════

  private isMyPiece(v: number): boolean {
    return this.myColor === 'black' ? v === 1 || v === 3 : v === 2 || v === 4;
  }
  private isOpponent(v: number): boolean {
    return this.myColor === 'black' ? v === 2 || v === 4 : v === 1 || v === 3;
  }
  private isStrongNode(r: number, c: number): boolean { return r % 2 === c % 2; }
  private isMullah(v: number): boolean { return v === 3 || v === 4; }

  canCaptureFrom(r: number, c: number, board: number[]): boolean {
    const T = this.taille;
    const piece = board[r * T + c];
    if (!this.isMullah(piece)) {
      const jumps: [number, number][] = [[r-2,c],[r+2,c],[r,c-2],[r,c+2]];
      if (this.isStrongNode(r, c)) jumps.push([r-2,c-2],[r-2,c+2],[r+2,c-2],[r+2,c+2]);
      for (const [tr, tc] of jumps) {
        if (tr < 0 || tr >= T || tc < 0 || tc >= T || board[tr*T+tc] !== 0) continue;
        const mr = (r+tr)/2, mc = (c+tc)/2;
        if (this.isOpponent(board[Math.floor(mr)*T+Math.floor(mc)])) return true;
      }
    } else {
      const dirs: [number, number][] = this.isStrongNode(r, c)
        ? [[0,1],[0,-1],[1,0],[-1,0],[1,1],[1,-1],[-1,1],[-1,-1]]
        : [[0,1],[0,-1],[1,0],[-1,0]];
      for (const [dr, dc] of dirs) {
        let opp = false;
        let cr = r + dr, cc = c + dc;
        while (cr >= 0 && cr < T && cc >= 0 && cc < T) {
          const v = board[cr*T+cc];
          if (v === 0) { if (opp) return true; }
          else if (this.isOpponent(v) && !opp) opp = true;
          else break;
          cr += dr; cc += dc;
        }
      }
    }
    return false;
  }

  private getValidTargets(pieceIdx: number, board: number[], captureOnly = false): number[] {
    const T = this.taille;
    const r = Math.floor(pieceIdx / T), c = pieceIdx % T;
    const piece = board[r * T + c];
    const targets: number[] = [];

    if (!this.isMullah(piece)) {
      if (!captureOnly) {
        const simple: [number, number][] = [[r-1,c],[r+1,c],[r,c-1],[r,c+1]];
        if (this.isStrongNode(r, c)) simple.push([r-1,c-1],[r-1,c+1],[r+1,c-1],[r+1,c+1]);
        for (const [tr, tc] of simple) {
          if (tr >= 0 && tr < T && tc >= 0 && tc < T && board[tr*T+tc] === 0) targets.push(tr*T+tc);
        }
      }
      const jumps: [number, number][] = [[r-2,c],[r+2,c],[r,c-2],[r,c+2]];
      if (this.isStrongNode(r, c)) jumps.push([r-2,c-2],[r-2,c+2],[r+2,c-2],[r+2,c+2]);
      for (const [tr, tc] of jumps) {
        if (tr < 0 || tr >= T || tc < 0 || tc >= T || board[tr*T+tc] !== 0) continue;
        const mr = (r+tr)/2, mc = (c+tc)/2;
        if (this.isOpponent(board[Math.floor(mr)*T+Math.floor(mc)])) targets.push(tr*T+tc);
      }
    } else {
      const dirs: [number, number][] = this.isStrongNode(r, c)
        ? [[0,1],[0,-1],[1,0],[-1,0],[1,1],[1,-1],[-1,1],[-1,-1]]
        : [[0,1],[0,-1],[1,0],[-1,0]];
      for (const [dr, dc] of dirs) {
        let opp = false;
        let cr = r + dr, cc = c + dc;
        while (cr >= 0 && cr < T && cc >= 0 && cc < T) {
          const v = board[cr*T+cc];
          if (v === 0) {
            if (!captureOnly || opp) targets.push(cr*T+cc);
          } else if (this.isOpponent(v) && !opp) {
            opp = true;
          } else break;
          cr += dr; cc += dc;
        }
      }
    }
    return targets;
  }

  // Applique un saut localement — gère mullahs + promotion
  private applyLocalJump(fr: number, fc: number, tr: number, tc: number, board: number[]): number[] {
    const T = this.taille;
    const next = [...board];
    const piece = next[fr * T + fc];
    next[tr * T + tc] = piece;
    next[fr * T + fc] = 0;
    // Parcourir le chemin pour trouver et supprimer la pièce capturée
    const stepR = Math.sign(tr - fr), stepC = Math.sign(tc - fc);
    let mr = fr + stepR, mc = fc + stepC;
    while (mr !== tr || mc !== tc) {
      if (next[mr * T + mc] !== 0) { next[mr * T + mc] = 0; break; }
      mr += stepR; mc += stepC;
    }
    // Promotion
    if (piece === 1 && tr === T - 1) next[tr * T + tc] = 3;
    if (piece === 2 && tr === 0)     next[tr * T + tc] = 4;
    return next;
  }

  // ═════════════════════════════════════════════════════════════════════
  // NOYAU — gestion des clics (UX moderne : auto-submit)
  // ═════════════════════════════════════════════════════════════════════

  selctepiece(index: number) {
    if (this.gameState !== 'PLAYING' || !this.isMyTurn) return;
    this.moveError = '';

    const piece    = this.plateau[index];
    const isMyPiece = this.isMyPiece(piece);
    const isTarget  = this.validMoveTargets.includes(index);

    // ── En séquence de capture ──────────────────────────────────────────
    if (this.pendingCapture) {
      if (isMyPiece) { this.cancelCapture(); return; }
      if (isTarget)  { this.continueCapture(index); }
      return;
    }

    // ── Aucune pièce sélectionnée ───────────────────────────────────────
    if (this.piececlick === null) {
      if (!isMyPiece) return;
      if (this.mustCaptureIndices.length > 0 && !this.mustCaptureIndices.includes(index)) {
        this.moveError = 'Capture obligatoire ! Choisissez une pièce surlignée en vert.';
        return;
      }
      this.selectPiece(index);
      return;
    }

    // ── Pièce déjà sélectionnée ─────────────────────────────────────────
    if (index === this.piececlick) { this.deselectPiece(); return; }

    if (isMyPiece) {
      if (this.mustCaptureIndices.length > 0 && !this.mustCaptureIndices.includes(index)) {
        this.moveError = 'Capture obligatoire ! Choisissez une pièce surlignée en vert.';
        return;
      }
      this.selectPiece(index);
      return;
    }

    if (!isTarget) return;

    const fr = Math.floor(this.piececlick / this.taille), fc = this.piececlick % this.taille;
    const tr = Math.floor(index / this.taille),            tc = index % this.taille;
    const dist = Math.max(Math.abs(tr - fr), Math.abs(tc - fc));

    if (dist > 1) {
      this.startCapture(fr, fc, tr, tc, index);
    } else {
      this.executeSimpleMove(fr, fc, tr, tc);
    }
  }

  private selectPiece(index: number) {
    this.piececlick = index;
    const captureOnly = this.mustCaptureIndices.length > 0;
    this.validMoveTargets = this.getValidTargets(index, this.plateau, captureOnly);
  }

  private deselectPiece() {
    this.piececlick = null;
    this.validMoveTargets = [];
  }

  private setLastMoved(idx: number) {
    this.lastMovedIdx = idx;
    setTimeout(() => { this.lastMovedIdx = null; this.cdr.detectChanges(); }, 600);
  }

  private startCapture(fr: number, fc: number, tr: number, tc: number, newIdx: number) {
    this.currentPath = [[fr, fc], [tr, tc]];
    this.pendingCapture = true;
    this.plateau = this.applyLocalJump(fr, fc, tr, tc, this.plateau);
    this.piececlick = newIdx;
    this.setLastMoved(newIdx);
    this.afterJump(tr, tc);
  }

  private continueCapture(index: number) {
    const fr = Math.floor(this.piececlick! / this.taille), fc = this.piececlick! % this.taille;
    const tr = Math.floor(index / this.taille),            tc = index % this.taille;
    this.currentPath.push([tr, tc]);
    this.plateau = this.applyLocalJump(fr, fc, tr, tc, this.plateau);
    this.piececlick = index;
    this.setLastMoved(index);
    this.afterJump(tr, tc);
  }

  // Après chaque saut : si encore des captures → montrer cibles, sinon auto-submit
  private afterJump(tr: number, tc: number) {
    if (this.canCaptureFrom(tr, tc, this.plateau)) {
      this.validMoveTargets = this.getValidTargets(this.piececlick!, this.plateau, true);
      this.cdr.detectChanges();
    } else {
      this.validMoveTargets = [];
      this.cdr.detectChanges();
      setTimeout(() => this.submitCaptureSequence(), 200);
    }
  }

  private cancelCapture() {
    if (this.currentGame?.boardState) this.parseBoardState(this.currentGame.boardState);
    this.currentPath = [];
    this.pendingCapture = false;
    this.piececlick = null;
    this.validMoveTargets = [];
  }

  private executeSimpleMove(fr: number, fc: number, tr: number, tc: number) {
    this.gameService.playMove(this.currentGame!.gameId, [[fr, fc], [tr, tc]]).subscribe({
      next: (res) => {
        if (this.currentGame) this.currentGame.boardState = res.boardState;
        const destIdx = tr * this.taille + tc;
        this.setLastMoved(destIdx);
        this.parseBoardState(res.boardState);
        this.deselectPiece();
        if (res.status === 'FINISHED') this.handleFinished(res);
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.moveError = err.error?.error || 'Coup invalide';
        this.deselectPiece();
        this.cdr.detectChanges();
      }
    });
  }

  private submitCaptureSequence() {
    if (!this.pendingCapture || this.currentPath.length < 2) return;
    const path = [...this.currentPath];
    this.gameService.playMove(this.currentGame!.gameId, path).subscribe({
      next: (res) => {
        if (this.currentGame) this.currentGame.boardState = res.boardState;
        this.parseBoardState(res.boardState);
        this.currentPath = [];
        this.pendingCapture = false;
        this.deselectPiece();
        if (res.status === 'FINISHED') this.handleFinished(res);
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.moveError = err.error?.error || 'Coup invalide';
        this.cancelCapture();
        this.cdr.detectChanges();
      }
    });
  }

  private handleFinished(res: any) {
    this.gameState = 'FINISHED';
    this.stopPolling();
    this.chatSub?.unsubscribe(); this.chatSub = null;
    this.gameResult = res.winner === this.myUsername
      ? 'Vous avez gagné ! +20 crédits'
      : res.winner ? 'Vous avez perdu. -10 crédits' : 'Match nul !';
    this.playerService.refreshProfile();
  }

  // ═════════════════════════════════════════════════════════════════════
  // Chat
  // ═════════════════════════════════════════════════════════════════════

  onMessageSent(text: string) {
    if (!text.trim() || !this.currentGame?.gameId) return;
    this.messageService.sendMessage(this.currentGame.gameId, text.trim()).subscribe({
      next: (msg) => { this.messages = [...this.messages, msg]; this.cdr.detectChanges(); },
      error: () => {}
    });
  }

  // ═════════════════════════════════════════════════════════════════════
  // Invitation
  // ═════════════════════════════════════════════════════════════════════

  sendInvitation() {
    if (!this.inviteUsername.trim() || !this.currentGame?.joinToken) return;
    this.inviteStatus = 'Recherche du joueur...';
    this.playerService.searchByUsername(this.inviteUsername.trim()).subscribe({
      next: (res) => {
        if (!res.found || !res.id) { this.inviteStatus = 'Joueur introuvable.'; return; }
        const subject = `Invitation Dhamet – Code : ${this.currentGame!.joinToken}`;
        const body    = `${this.myUsername} t'invite à jouer ! Code : ${this.currentGame!.joinToken}`;
        this.mailService.sendMail(res.id, subject, body).subscribe({
          next: () => {
            this.inviteStatus = `Invitation envoyée à ${res.username} !`;
            this.inviteUsername = '';
            this.cdr.detectChanges();
          },
          error: () => { this.inviteStatus = "Erreur lors de l'envoi."; }
        });
      },
      error: () => { this.inviteStatus = 'Erreur réseau.'; }
    });
  }
}
