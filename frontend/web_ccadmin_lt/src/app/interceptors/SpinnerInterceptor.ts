import { Injectable } from '@angular/core';
import { SpinnerService } from '../enterprise/shared/service/spinner.service';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { defer, Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';

@Injectable()
export class SpinnerInterceptor implements HttpInterceptor {
  IgnoreMethodList: string[] = [];

  constructor(private spinnerService: SpinnerService) { }

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const showLoading = !this.IgnoreMethod(req);
    return defer(() => {
      this.spinnerService.show(showLoading);
      return defer(() => next.handle(req)).pipe(
        finalize(() => this.spinnerService.hide(showLoading))
      );
    });
  }

  IgnoreMethod(req: HttpRequest<any>): boolean {
    const method = req.url.split('?')[0].split('/').pop() || '';
    return this.IgnoreMethodList.includes(method);
  }
}
