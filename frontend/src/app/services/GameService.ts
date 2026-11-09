import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { HttpClient, HttpHeaders } from "@angular/common/http";
import { Game } from "../models/Game";
import { Move } from "../models/Move";
import { API_BASE } from "../config/api";

@Injectable({ providedIn: 'root' })
export class GameService {

    private apiUrl = `${API_BASE}/api/games`;

    constructor(private http: HttpClient) {}

    private getHeaders(): HttpHeaders {
        return new HttpHeaders({
            'X-Session-Token': sessionStorage.getItem('sessionToken') || ''
        });
    }


    // À ajouter dans GameService.ts
    getCurrentGame(): Observable<any> {
        return this.http.get<any>(`${this.apiUrl}/current`, { headers: this.getHeaders() });
    }
    
    createGame(): Observable<Game> {
        return this.http.post<Game>(`${this.apiUrl}/create`, {}, { headers: this.getHeaders() });
    }

    joinGame(joinToken: string): Observable<Game> {
        return this.http.post<Game>(`${this.apiUrl}/join/${joinToken}`, {}, { headers: this.getHeaders() });
    }

    getGame(gameId: number): Observable<Game> {
        return this.http.get<Game>(`${this.apiUrl}/${gameId}`, { headers: this.getHeaders() });
    }

    playMove(gameId: number, path: number[][]): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/${gameId}/move`, { path }, { headers: this.getHeaders() });
    }

    resign(gameId: number): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/${gameId}/resign`, {}, { headers: this.getHeaders() });
    }

    getWaitingGames(): Observable<Game[]> {
        return this.http.get<Game[]>(`${this.apiUrl}/waiting`, { headers: this.getHeaders() });
    }

    getMoves(gameId: number): Observable<Move[]> {
        return this.http.get<Move[]>(`${this.apiUrl}/${gameId}/moves`, { headers: this.getHeaders() });
    }

    getHistory(): Observable<any[]> {
        return this.http.get<any[]>(`${this.apiUrl}/history`, { headers: this.getHeaders() });
    }
}