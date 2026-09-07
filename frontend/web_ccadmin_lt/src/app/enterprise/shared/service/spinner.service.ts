import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class SpinnerService {
  private activeOperations: number = 0;
  private readonly loadingSubject = new BehaviorSubject<boolean>(false);
  readonly IsLoading$ = this.loadingSubject.asObservable();

  get isLoading(): boolean {
    return this.loadingSubject.value;
  }

  show(action: boolean = true): void {
    if (!action) return;
    this.activeOperations++;
    if (!this.isLoading) this.loadingSubject.next(true);
  }

  hide(action: boolean = true): void {
    if (!action) return;
    this.activeOperations = Math.max(0, this.activeOperations - 1);
    if (this.activeOperations === 0 && this.isLoading) this.loadingSubject.next(false);
  }

  /** Mantiene el bloqueo durante toda la operación, incluidas consultas encadenadas. */
  async run<T>(operation: () => Promise<T>): Promise<T> {
    this.show();
    try {
      return await operation();
    } finally {
      this.hide();
    }
  }
}
