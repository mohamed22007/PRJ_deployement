import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { LoginComponent } from './login';
import { PlayerService } from '../../services/player_services';

/**
 * Test du composant Login.
 * Le PlayerService est remplacé par un faux (mock) : on teste uniquement
 * le comportement du composant (navigation, message d'erreur).
 */
describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  const playerService = { login: vi.fn() };
  const router = { navigate: vi.fn() };

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        { provide: PlayerService, useValue: playerService },
        { provide: Router, useValue: router },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('connexion réussie : redirige vers /home', () => {
    playerService.login.mockReturnValue(of({ token: 't', playerId: 1, username: 'ali' }));
    component.username = 'ali';
    component.password = 'secret123';

    component.onLogin();

    expect(playerService.login).toHaveBeenCalledWith('ali', 'secret123');
    expect(router.navigate).toHaveBeenCalledWith(['/home']);
  });

  it('connexion échouée : affiche une alerte et reste sur la page', () => {
    playerService.login.mockReturnValue(throwError(() => new Error('400')));
    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});

    component.onLogin();

    expect(alertSpy).toHaveBeenCalledWith('Identifiant ou mot de passe incorrect');
    expect(router.navigate).not.toHaveBeenCalled();
    alertSpy.mockRestore();
  });

  it('le bouton "Creation de compte" mène à l\'inscription', () => {
    const boutons: NodeListOf<HTMLButtonElement> = fixture.nativeElement.querySelectorAll('button');
    boutons[1].click();

    expect(router.navigate).toHaveBeenCalledWith(['/auth/inscription']);
  });

  it('les champs du formulaire mettent à jour le composant', async () => {
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input[type="text"]');
    input.value = 'sara';
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();

    expect(component.username).toBe('sara');
  });
});
