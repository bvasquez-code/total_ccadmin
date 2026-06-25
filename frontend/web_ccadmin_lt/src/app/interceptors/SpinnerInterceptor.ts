import { Injectable } from '@angular/core';
import { SpinnerService } from '../enterprise/shared/service/spinner.service';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';

@Injectable()
export class SpinnerInterceptor implements HttpInterceptor
{
    IgnoreMethodList : string[] = [];

    constructor(private spinnerService : SpinnerService)
    {
    }


    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        
        this.spinnerService.show(!this.IgnoreMethod(req));

        return next.handle(req).pipe(
            finalize(() => this.spinnerService.hide(!this.IgnoreMethod(req)))
        );

    }

    IgnoreMethod(req : HttpRequest<any>) : boolean
    {
        try{
            let ListInvocation = req.url.split("/");
            let Method = ListInvocation[ListInvocation.length - 1];
            return this.IgnoreMethodList.includes(Method);
        }catch
        {
            return false;
        }
    }
    
}