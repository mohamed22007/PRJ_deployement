import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { HttpClient, HttpHeaders } from "@angular/common/http";
import { Mail } from "../models/Mail";
import { API_BASE } from "../config/api";

@Injectable({ providedIn: 'root' })
export class MailService {

    private apiUrl = `${API_BASE}/api/mails`;

    constructor(private http: HttpClient) {}

    private getHeaders(): HttpHeaders {
        return new HttpHeaders({
            'X-Session-Token': sessionStorage.getItem('sessionToken') || ''
        });
    }

    sendMail(receiverId: number, subject: string, content: string): Observable<any> {
        return this.http.post<any>(
            `${this.apiUrl}/send`, 
            { receiverId, subject, content }, 
            { headers: this.getHeaders() }
        );
    }

    getInbox(): Observable<Mail[]> {
        return this.http.get<Mail[]>(`${this.apiUrl}/inbox`, { headers: this.getHeaders() });
    }

    getUnreadCount(): Observable<{count: number}> {
        return this.http.get<{count: number}>(`${this.apiUrl}/unread-count`, { headers: this.getHeaders() });
    }

    markAsRead(mailId: number): Observable<any> {
        return this.http.put<any>(`${this.apiUrl}/${mailId}/read`, {}, { headers: this.getHeaders() });
    }

    deleteMail(mailId: number): Observable<any> {
        return this.http.delete<any>(`${this.apiUrl}/${mailId}`, { headers: this.getHeaders() });
    }
}