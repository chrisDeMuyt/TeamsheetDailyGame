import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { GuessResult, PlayView, Puzzle, Reveal } from './models';

/** A response body plus the server's clock at response time (from the Date header), for the countdown. */
export interface Timed<T> {
  body: T;
  serverNow: number;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  today(): Promise<Timed<Puzzle>> {
    return this.timed(this.http.get<Puzzle>('/api/puzzle/today', { observe: 'response' }));
  }

  createPlay(anonId: string): Promise<Timed<PlayView>> {
    return this.timed(this.http.post<PlayView>('/api/plays', { anonId }, { observe: 'response' }));
  }

  guess(playId: string, text: string): Promise<Timed<GuessResult>> {
    return this.timed(
      this.http.post<GuessResult>(`/api/plays/${playId}/guesses`, { text }, { observe: 'response' }),
    );
  }

  reveal(playId: string): Promise<Timed<Reveal>> {
    return this.timed(this.http.get<Reveal>(`/api/plays/${playId}/reveal`, { observe: 'response' }));
  }

  private async timed<T>(req: import('rxjs').Observable<HttpResponse<T>>): Promise<Timed<T>> {
    const res = await firstValueFrom(req);
    // Date has 1s resolution; +500ms centres the estimate. Falls back to the local clock.
    const header = res.headers.get('date');
    const parsed = header ? Date.parse(header) : NaN;
    return { body: res.body as T, serverNow: Number.isNaN(parsed) ? Date.now() : parsed + 500 };
  }
}
