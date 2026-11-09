import { Injectable, NgZone, OnDestroy } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import { Observable, ReplaySubject } from 'rxjs';
import { switchMap, take } from 'rxjs/operators';
import { API_WS } from '../config/api';

@Injectable({ providedIn: 'root' })
export class WsService implements OnDestroy {
  private client: Client | null = null;
  private ready$ = new ReplaySubject<void>(1);

  constructor(private ngZone: NgZone) {}

  connect(token: string): void {
    if (this.client?.active) return;
    this.ready$ = new ReplaySubject<void>(1);
    this.ngZone.runOutsideAngular(() => {
      this.client = new Client({
        brokerURL: API_WS,
        connectHeaders: { token },
        reconnectDelay: 5000,
        onConnect:    () => this.ready$.next(),
        onStompError: () => {},
      });
      this.client.activate();
    });
  }

  watch<T>(topic: string): Observable<T> {
    return this.ready$.pipe(
      take(1),
      switchMap(() => new Observable<T>(observer => {
        const sub = this.client!.subscribe(topic, (msg: IMessage) => {
          try { observer.next(JSON.parse(msg.body) as T); } catch { /* ignore */ }
        });
        return () => sub.unsubscribe();
      }))
    );
  }

  disconnect(): void {
    this.client?.deactivate();
    this.client = null;
    this.ready$ = new ReplaySubject<void>(1);
  }

  ngOnDestroy(): void { this.disconnect(); }
}
