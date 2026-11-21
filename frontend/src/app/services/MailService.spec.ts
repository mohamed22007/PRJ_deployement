import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MailService } from './MailService';
import { API_BASE } from '../config/api';

describe('MailService', () => {
  let service: MailService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.setItem('sessionToken', 'tok-123');
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(MailService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  it('sendMail : POST avec destinataire, sujet et contenu', () => {
    service.sendMail(2, 'Salut', 'Partie ce soir ?').subscribe();

    const req = http.expectOne(`${API_BASE}/api/mails/send`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ receiverId: 2, subject: 'Salut', content: 'Partie ce soir ?' });
    expect(req.request.headers.get('X-Session-Token')).toBe('tok-123');
    req.flush({});
  });

  it('markAsRead : PUT /api/mails/{id}/read', () => {
    service.markAsRead(5).subscribe();
    const req = http.expectOne(`${API_BASE}/api/mails/5/read`);
    expect(req.request.method).toBe('PUT');
    req.flush({});
  });

  it('deleteMail : DELETE /api/mails/{id}', () => {
    service.deleteMail(5).subscribe();
    const req = http.expectOne(`${API_BASE}/api/mails/5`);
    expect(req.request.method).toBe('DELETE');
    req.flush({});
  });

  it('getUnreadCount : renvoie le compteur', () => {
    let count = -1;
    service.getUnreadCount().subscribe((r) => (count = r.count));
    http.expectOne(`${API_BASE}/api/mails/unread-count`).flush({ count: 3 });
    expect(count).toBe(3);
  });
});
