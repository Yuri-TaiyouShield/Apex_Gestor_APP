import { Injectable } from '@angular/core';
import { PreloadingStrategy, Route } from '@angular/router';
import { EMPTY, Observable, Subscription } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class IdlePreloadStrategy implements PreloadingStrategy {
  preload(route: Route, load: () => Observable<unknown>): Observable<unknown> {
    if (route.data?.['preload'] === false || this.prefersReducedNetwork()) {
      return EMPTY;
    }

    return new Observable((subscriber) => {
      let subscription: Subscription | undefined;
      const start = () => {
        subscription = load().subscribe(subscriber);
      };

      const requestIdle = window.requestIdleCallback;
      const cancelIdle = window.cancelIdleCallback;

      if (typeof requestIdle === 'function' && typeof cancelIdle === 'function') {
        const handle = requestIdle(start, { timeout: 2500 });
        return () => {
          cancelIdle(handle);
          subscription?.unsubscribe();
        };
      }

      const handle = window.setTimeout(start, 1200);
      return () => {
        window.clearTimeout(handle);
        subscription?.unsubscribe();
      };
    });
  }

  private prefersReducedNetwork(): boolean {
    const connection = (navigator as Navigator & {
      connection?: {
        saveData?: boolean;
        effectiveType?: string;
      };
    }).connection;

    return Boolean(connection?.saveData || connection?.effectiveType === '2g' || connection?.effectiveType === 'slow-2g');
  }
}
