import { provideHttpClient } from '@angular/common/http';
import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideShared, SHARED_ALL, SHARED_PROVIDERS } from './shared/shared.config';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideShared(),
    ...SHARED_PROVIDERS,
    provideHttpClient(),
  ]
};
