import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { GameService } from './GameService';
import { API_BASE } from '../config/api';

describe('GameService', () => {
  let service: GameService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.setItem('sessionToken', 'tok-123');
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(GameService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  it('createGame : POST /api/games/create avec le token', () => {
    service.createGame().subscribe();

    const req = http.expectOne(`${API_BASE}/api/games/create`);
    expect(req.request.method).toBe('POST');
    expect(req.request.headers.get('X-Session-Token')).toBe('tok-123');
    req.flush({ gameId: 1, joinToken: 'ABC123', status: 'WAITING' });
  });

  it('joinGame : met le code de la partie dans l\'URL', () => {
    service.joinGame('ABC123').subscribe();

    const req = http.expectOne(`${API_BASE}/api/games/join/ABC123`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  it('playMove : envoie le chemin du coup dans le body', () => {
    const path = [[0, 0], [2, 0], [4, 0]];
    service.playMove(42, path).subscribe();

    const req = http.expectOne(`${API_BASE}/api/games/42/move`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ path });
    req.flush({});
  });

  it('resign : POST /api/games/{id}/resign', () => {
    service.resign(42).subscribe();
    const req = http.expectOne(`${API_BASE}/api/games/42/resign`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  it('getHistory : renvoie la réponse du serveur', () => {
    let result: any[] = [];
    service.getHistory().subscribe((h) => (result = h));

    http.expectOne(`${API_BASE}/api/games/history`).flush([{ gameId: 1, opponent: 'sara', result: 'Victoire' }]);

    expect(result.length).toBe(1);
    expect(result[0].result).toBe('Victoire');
  });

  it('sans token, envoie un en-tête vide (le backend refusera)', () => {
    sessionStorage.clear();
    service.getWaitingGames().subscribe();

    const req = http.expectOne(`${API_BASE}/api/games/waiting`);
    expect(req.request.headers.get('X-Session-Token')).toBe('');
    req.flush([]);
  });
});
