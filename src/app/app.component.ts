import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SHARED_ALL } from './shared/shared.config';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, SHARED_ALL],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent {
  title = 'hc-app';
}
