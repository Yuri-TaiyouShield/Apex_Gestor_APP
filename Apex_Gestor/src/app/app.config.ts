import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideRouter, withComponentInputBinding, withPreloading } from '@angular/router';
import { RouteReuseStrategy } from '@angular/router';
import { provideIonicAngular } from '@ionic/angular/standalone';

import { routes } from './app.routes';
import { ApexRouteReuseStrategy } from './core/apex-route-reuse.strategy';
import { authInterceptor } from './core/auth.interceptor';
import { IdlePreloadStrategy } from './core/idle-preload.strategy';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true, runCoalescing: true }),
    provideIonicAngular({
      mode: 'md'
    }),
    {
      provide: RouteReuseStrategy,
      useClass: ApexRouteReuseStrategy
    },
    provideRouter(routes, withPreloading(IdlePreloadStrategy), withComponentInputBinding()),
    provideHttpClient(withFetch(), withInterceptors([authInterceptor]))
  ]
};
