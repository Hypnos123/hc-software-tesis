import { provideHttpClient } from '@angular/common/http';
import { ApplicationConfig, LOCALE_ID } from '@angular/core';
import { provideRouter, withHashLocation } from '@angular/router';
import { routes } from './app.routes';
import { provideAnimations } from '@angular/platform-browser/animations';
import { CurrencyPipe, DatePipe, registerLocaleData } from '@angular/common';
import { MessageService } from 'primeng/api';
import localeEsPe from '@angular/common/locales/es-PE';

registerLocaleData(localeEsPe);

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withHashLocation()),
    provideHttpClient(),
    provideAnimations(),
    { provide: LOCALE_ID, useValue: 'es-PE' },
    MessageService,
    CurrencyPipe,
    DatePipe
  ]
};
