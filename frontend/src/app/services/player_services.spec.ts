import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PlayerService } from './player_services';
import { API_BASE } from '../config/api';

/**
 * Tests du PlayerService.
 * HttpTestingController intercepte les requêtes HTTP : aucun backend n'est appelé.
 * On vérifie l'URL, la méthode, les en-têtes, puis on "répond" à la place du serveur.
 */
describe('PlayerService', () => {
  let service: PlayerService;
  let http: HttpTestingController;

  function creerService() {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PlayerService);
    http = TestBed.inject(HttpTestingController);
  }

  beforeEach(() => sessionStorage.clear());
  afterEach(() => http.verify()); // échoue s'il reste une requête non vérifiée

  it('login : envoie les identifiants, stocke le token et charge le profil', () => {
    creerService();
    let users: any[] = [];
    service.currentUser$.subscribe((u) => users.push(u));

    service.login('ali', 'secret123').subscribe();

    const req = http.expectOne(`${API_BASE}/api/players/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ username: 'ali', password: 'secret123' });
    req.flush({ token: 'tok-123', playerId: 1, username: 'ali' });

    expect(sessionStorage.getItem('sessionToken')).toBe('tok-123');

    // Après le login, le service demande /me avec le token dans l'en-tête
    const me = http.expectOne(`${API_BASE}/api/players/me`);
    expect(me.request.headers.get('X-Session-Token')).toBe('tok-123');
    me.flush({ id: 1, username: 'ali', credit: 100 });

    expect(users.at(-1)).toEqual({ id: 1, username: 'ali', credit: 100 });
  });

  it('logout : supprime le token et vide l\'utilisateur courant', () => {
    creerService();
    sessionStorage.setItem('sessionToken', 'tok-123');
    let current: any = 'pas encore';
    service.currentUser$.subscribe((u) => (current = u));

    service.logout().subscribe();
    const req = http.expectOne(`${API_BASE}/api/players/logout`);
    expect(req.request.method).toBe('POST');
    expect(req.request.headers.get('X-Session-Token')).toBe('tok-123');
    req.flush({ message: 'ok' });

    expect(sessionStorage.getItem('sessionToken')).toBeNull();
    expect(current).toBeNull();
  });

  it('au démarrage, recharge le profil si un token existe déjà', () => {
    sessionStorage.setItem('sessionToken', 'tok-ancien');
    creerService();

    const me = http.expectOne(`${API_BASE}/api/players/me`);
    expect(me.request.headers.get('X-Session-Token')).toBe('tok-ancien');
    me.flush({ id: 7, username: 'sara' });
  });

  it('au démarrage, sans token, ne fait aucune requête', () => {
    creerService();
    http.expectNone(`${API_BASE}/api/players/me`);
  });

  it('searchByUsername : encode le nom dans l\'URL', () => {
    creerService();
    service.searchByUsername('a b&c').subscribe();

    const req = http.expectOne((r) => r.url.startsWith(`${API_BASE}/api/players/search`));
    expect(req.request.url).toBe(`${API_BASE}/api/players/search?username=a%20b%26c`);
    req.flush({ found: false });
  });

  it('register : envoie le joueur en JSON', () => {
    creerService();
    const joueur = { username: 'ali', password: 'secret123', nom: 'Ali', email: 'ali@mail.com' };

    service.register(joueur).subscribe();

    const req = http.expectOne(`${API_BASE}/api/players/register`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(joueur);
    req.flush({});
  });
});
