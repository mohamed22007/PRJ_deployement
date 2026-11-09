import { Injectable } from "@angular/core";
import { BehaviorSubject, Observable } from "rxjs";
import { HttpClient, HttpHeaders } from "@angular/common/http";
import { tap } from 'rxjs/operators';
import { Player, AuthResponse } from "../models/Player";
import { API_BASE } from "../config/api";

@Injectable({ providedIn: 'root' })
export class PlayerService {

    private apiUrl = `${API_BASE}/api/players`;

    // Stocke les infos de l'utilisateur connecté
    private currentUserSubject = new BehaviorSubject<Player | null>(null);
    public currentUser$ = this.currentUserSubject.asObservable();
    
    constructor(private http: HttpClient) {
        // Au démarrage, on peut vérifier si un token existe pour recharger le profil
        if (this.getToken()) {
            this.getMe().subscribe({
                next: (player) => this.currentUserSubject.next(player),
                error: () => this.logout() // Token invalide ou expiré
            });
        }
    }

    private getToken(): string | null {
        return sessionStorage.getItem('sessionToken');
    }

    private getHeaders(): HttpHeaders {
        return new HttpHeaders({
            'X-Session-Token': this.getToken() || ''
        });
    }

    register(player: Player): Observable<any> {
        // Le backend attend un JSON dans le body
        return this.http.post<any>(`${this.apiUrl}/register`, player);
    }
    
    login(username: string, password: string): Observable<AuthResponse> {
        // Le backend attend un body JSON pour le login, pas des HttpParams
        return this.http.post<AuthResponse>(`${this.apiUrl}/login`, { username, password }).pipe(
            tap(response => {
                sessionStorage.setItem('sessionToken', response.token);
                // On met à jour un joueur basique, qui sera complété par getMe()
                this.currentUserSubject.next({ id: response.playerId, username: response.username });
                this.getMe().subscribe(p => this.currentUserSubject.next(p));
            })
        );
    }

    logout(): Observable<any> {
        return this.http.post(`${this.apiUrl}/logout`, {}, { headers: this.getHeaders() }).pipe(
            tap(() => {
                sessionStorage.removeItem('sessionToken');
                this.currentUserSubject.next(null);
            })
        );
    }

    getMe(): Observable<Player> {
        return this.http.get<Player>(`${this.apiUrl}/me`, { headers: this.getHeaders() });
    }

    refreshProfile() {
        this.getMe().subscribe({
            next: (p) => this.currentUserSubject.next(p),
            error: () => {}
        });
    }

    searchByUsername(username: string): Observable<{found: boolean, id?: number, username?: string}> {
        return this.http.get<any>(`${this.apiUrl}/search?username=${encodeURIComponent(username)}`, { headers: this.getHeaders() });
    }

    getRanking(): Observable<{username: string, nom: string, credit: number}[]> {
        return this.http.get<any[]>(`${this.apiUrl}/ranking`, { headers: this.getHeaders() });
    }
}