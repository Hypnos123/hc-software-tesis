import { Component } from '@angular/core';
import { LoaderService } from '../../services/loader.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-loader',
  templateUrl: './loader.component.html',
  styleUrls: ['./loader.component.scss'],
  standalone: true,
  imports: [CommonModule]
})
export class LoaderComponent {
  isLoading: boolean = true;

  constructor(private loaderService: LoaderService) {
    this.loaderService.loadingStatus.subscribe(status => {
      this.isLoading = status;
    });
  }
}
