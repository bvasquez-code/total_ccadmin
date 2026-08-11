import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, finalize } from 'rxjs';
import { LoadingService } from '../enterprise/shared/service/loading.service';

@Injectable()
export class LoadingInterceptor implements HttpInterceptor {

  public constructor(private loadingService: LoadingService) {
  }

  public intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    this.loadingService.begin();
    return next.handle(request).pipe(finalize(() => this.loadingService.end()));
  }
}
