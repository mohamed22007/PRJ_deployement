import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { HttpClient, HttpHeaders } from "@angular/common/http";
import { Message } from "../models/Message";
import { API_BASE } from "../config/api";

@Injectable({ providedIn: 'root' })
export class MessageService {

    private apiUrl = `${API_BASE}/api/games`;

    constructor(private http: HttpClient) {}

    private getHeaders(): HttpHeaders {
        return new HttpHeaders({
            'X-Session-Token': sessionStorage.getItem('sessionToken') || ''
        });
    }

    sendMessage(gameId: number, content: string): Observable<Message> {
        return this.http.post<Message>(
            `${this.apiUrl}/${gameId}/chat`, 
            { content }, 
            { headers: this.getHeaders() }
        );
    }

    getChat(gameId: number): Observable<Message[]> {
        return this.http.get<Message[]>(`${this.apiUrl}/${gameId}/chat`, { headers: this.getHeaders() });
    }
}