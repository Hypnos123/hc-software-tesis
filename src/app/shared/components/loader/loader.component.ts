import { Component } from '@angular/core';
import { LoaderService } from '../../services/loader.service';
import { SHARED_ALL } from '@app/shared/shared.config';

@Component({
  selector: 'app-loader',
  templateUrl: './loader.component.html',
  styleUrls: ['./loader.component.scss'],
  standalone: true,
  imports: [SHARED_ALL]
})
export class LoaderComponent {
  isLoading: boolean = true;

  constructor(private loaderService: LoaderService) {
    this.loaderService.loadingStatus.subscribe(status => {
      this.isLoading = status;
    });
  }
}
