import { Component } from '@angular/core';
import { LoadingService } from '../../service/loading.service';

@Component({
  selector: 'app-loading',
  templateUrl: './loading.component.html',
  styleUrls: ['./loading.component.css']
})
export class LoadingComponent {
  public readonly Loading$ = this.loadingService.Loading$;

  public constructor(private loadingService: LoadingService) {
  }
}
