import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class LoadingService {
  private pendingRequests: number = 0;
  private loadingSubject = new BehaviorSubject<boolean>(false);

  public readonly Loading$: Observable<boolean> = this.loadingSubject.asObservable();

  public begin(): void {
    this.pendingRequests++;
    this.loadingSubject.next(true);
  }

  public end(): void {
    this.pendingRequests = Math.max(0, this.pendingRequests - 1);
    this.loadingSubject.next(this.pendingRequests > 0);
  }
}
