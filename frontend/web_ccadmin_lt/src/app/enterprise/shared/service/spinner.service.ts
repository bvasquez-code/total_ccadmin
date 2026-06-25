import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

@Injectable({
    providedIn : 'root'
})
export class SpinnerService
{
    IsLoading$ = new Subject<Boolean>();

    show( action : boolean)
    {
        if(action) this.IsLoading$.next(true);
    }

    async hide( action : boolean)
    {
        await this.delay(500);
        if(action) this.IsLoading$.next(false);
    }

    async delay(ms: number) {
        await new Promise<void>(resolve => setTimeout(()=>resolve(), ms)).then(()=>console.log("fired"));
    }
}