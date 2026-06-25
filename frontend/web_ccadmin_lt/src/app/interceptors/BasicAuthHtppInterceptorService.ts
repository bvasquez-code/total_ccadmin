import {Injectable} from '@angular/core';
import {HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse} from '@angular/common/http';
import {Observable, throwError} from 'rxjs';
import {catchError} from 'rxjs/operators';


@Injectable({
    providedIn: 'root'
})
export class BasicAuthHtppInterceptorService implements HttpInterceptor {
    constructor() {
    }

    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
   
        return next.handle(req).pipe(
            catchError((err: HttpErrorResponse) => {
                if (err.status === 401) {
                    sessionStorage.clear();
                    location.href = "http://localhost:4200/login";
                }

                return throwError(err);

            })
        );

    }
}
