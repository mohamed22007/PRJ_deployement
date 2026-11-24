import { TestBed, ComponentFixture } from '@angular/core/testing';
import { of, throwError, BehaviorSubject, NEVER } from 'rxjs';
import { vi } from 'vitest';
import { GameComponent } from './game';
import { GameService } from '../../services/GameService';
import { PlayerService } from '../../services/player_services';
import { MessageService } from '../../services/MessageService';
import { MailService } from '../../services/MailService';
import { WsService } from '../../services/WsService';

/**
 * Bug corrigé : après "Rejoindre", l'écran restait sur le menu (appli zoneless,
 * pas de rafraîchissement après la réponse HTTP). Le 2e clic renvoyait alors
 * "Cette partie a déjà commencé" avant d'afficher enfin le plateau.
 */
describe('GameComponent - rejoindre une partie', () => {
  let fixture: ComponentFixture<GameComponent>;
  let gameService: any;

  const partie = {
    gameId: 1, joinToken: 'ABC123', status: 'PLAYING',
    playerBlack: 'ali', playerWhite: 'sara',
    boardState: '1,1,1,1,1,1,1,1,1;1,1,1,1,1,1,1,1,1;1,1,1,1,1,1,1,1,1;1,1,1,1,1,1,1,1,1;1,1,1,1,0,2,2,2,2;2,2,2,2,2,2,2,2,2;2,2,2,2,2,2,2,2,2;2,2,2,2,2,2,2,2,2;2,2,2,2,2,2,2,2,2;.0,0.B',
  };

  beforeEach(() => {
    gameService = {
      getCurrentGame: vi.fn(() => of({ hasGame: false })),
      joinGame: vi.fn(() => of(partie)),
      getGame: vi.fn(() => of(partie)),
    };
    TestBed.configureTestingModule({
      imports: [GameComponent],
      providers: [
        { provide: GameService, useValue: gameService },
        { provide: PlayerService, useValue: { currentUser$: new BehaviorSubject({ username: 'sara' }), refreshProfile: vi.fn() } },
        { provide: MessageService, useValue: { getChat: vi.fn(() => of([])) } },
        { provide: MailService, useValue: {} },
        { provide: WsService, useValue: { connect: vi.fn(), watch: vi.fn(() => NEVER), disconnect: vi.fn() } },
      ],
    });
    fixture = TestBed.createComponent(GameComponent);
    fixture.detectChanges();
  });

  afterEach(() => fixture.destroy());

  function cliquerRejoindre(code: string) {
    fixture.componentInstance.joinTokenInput = code;
    const bouton: HTMLButtonElement = fixture.nativeElement.querySelector('.join-section button');
    bouton.click();
  }

  it('un seul clic suffit : l\'écran quitte le menu et je suis Blanc', () => {
    cliquerRejoindre('abc123');

    expect(gameService.joinGame).toHaveBeenCalledTimes(1);
    expect(gameService.joinGame).toHaveBeenCalledWith('ABC123');
    expect(fixture.componentInstance.gameState).toBe('PLAYING');
    expect(fixture.componentInstance.myColor).toBe('white');
    // Le DOM a bien été rafraîchi : plus de bouton "Rejoindre" affiché
    expect(fixture.nativeElement.querySelector('.join-section')).toBeNull();
  });

  it('pendant la requête, un 2e clic ne renvoie pas de 2e demande', () => {
    gameService.joinGame.mockReturnValue(NEVER); // réponse jamais reçue
    cliquerRejoindre('ABC123');
    fixture.componentInstance.rejoindrePartie();

    expect(gameService.joinGame).toHaveBeenCalledTimes(1);
    expect(fixture.componentInstance.joining).toBe(true);
  });

  it('code invalide : message d\'erreur et le bouton redevient cliquable', () => {
    const alerte = vi.spyOn(window, 'alert').mockImplementation(() => {});
    gameService.joinGame.mockReturnValue(throwError(() => ({ error: { error: 'Partie introuvable avec ce code.' } })));

    cliquerRejoindre('ZZZ999');

    expect(alerte).toHaveBeenCalledWith('Code invalide : Partie introuvable avec ce code.');
    expect(fixture.componentInstance.joining).toBe(false);
    expect(fixture.componentInstance.gameState).toBe('MENU');
    alerte.mockRestore();
  });
});
