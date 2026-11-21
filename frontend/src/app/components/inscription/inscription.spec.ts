import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Inscription } from './inscription';
import { PlayerService } from '../../services/player_services';

describe('Inscription', () => {
  let fixture: ComponentFixture<Inscription>;
  let component: Inscription;
  const playerService = { register: vi.fn() };
  const router = { navigate: vi.fn() };
  let alertSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    vi.clearAllMocks();
    alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});
    await TestBed.configureTestingModule({
      imports: [Inscription],
      providers: [
        { provide: PlayerService, useValue: playerService },
        { provide: Router, useValue: router },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Inscription);
    component = fixture.componentInstance;
  });

  afterEach(() => alertSpy.mockRestore());

  it('inscription réussie : message puis retour au login', () => {
    playerService.register.mockReturnValue(of({ message: 'ok' }));
    component.newPlayer = { username: 'ali', password: 'secret123', nom: 'Ali', email: 'ali@mail.com' };

    component.onCreate();

    expect(playerService.register).toHaveBeenCalledWith(component.newPlayer);
    expect(router.navigate).toHaveBeenCalledWith(['/auth/login']);
  });

  it('erreur du serveur : affiche le message renvoyé par le backend', () => {
    playerService.register.mockReturnValue(
      throwError(() => ({ error: { error: 'Ce nom d\'utilisateur est déjà pris.' } })),
    );
    component.newPlayer = { username: 'ali', password: 'secret123' };

    component.onCreate();

    expect(alertSpy).toHaveBeenCalledWith(
      "Erreur lors de l'inscription : Ce nom d'utilisateur est déjà pris.",
    );
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('sans username ni mot de passe : n\'appelle pas le serveur', () => {
    component.newPlayer = { username: '', password: '' };

    component.onCreate();

    expect(playerService.register).not.toHaveBeenCalled();
  });

  it('retour : revient à la page de connexion', () => {
    component.retour();
    expect(router.navigate).toHaveBeenCalledWith(['/auth/login']);
  });
});
