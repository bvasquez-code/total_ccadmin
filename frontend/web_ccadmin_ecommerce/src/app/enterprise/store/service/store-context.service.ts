import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { StorageConstants } from '../../shared/model/constants/StorageConstants';
import { StoreContextDto } from '../model/dto/StoreContextDto';
import { StoreEntity } from '../model/entity/StoreEntity';

@Injectable({ providedIn: 'root' })
export class StoreContextService {
  private contextSubject = new BehaviorSubject<StoreContextDto | null>(this.readContext());
  private locationSelectionSubject = new Subject<void>();

  public readonly Context$: Observable<StoreContextDto | null> = this.contextSubject.asObservable();
  public readonly LocationSelectionRequested$: Observable<void> = this.locationSelectionSubject.asObservable();

  public getCurrent(): StoreContextDto | null {
    return this.contextSubject.value;
  }

  public setContext(context: StoreContextDto): void {
    const normalized = Object.assign(new StoreContextDto(), context);
    normalized.Store = Object.assign(new StoreEntity(), context.Store ?? {});
    localStorage.setItem(StorageConstants.STORE_CONTEXT, JSON.stringify(normalized));
    this.contextSubject.next(normalized);
  }

  public clear(): void {
    localStorage.removeItem(StorageConstants.STORE_CONTEXT);
    this.contextSubject.next(null);
  }

  public requestLocationSelection(): void {
    this.locationSelectionSubject.next();
  }

  private readContext(): StoreContextDto | null {
    const value = localStorage.getItem(StorageConstants.STORE_CONTEXT);
    if (!value) return null;

    try {
      const parsed = JSON.parse(value);
      const context = Object.assign(new StoreContextDto(), parsed);
      context.Store = Object.assign(new StoreEntity(), parsed.Store ?? {});
      return context.Store.StoreCod ? context : null;
    } catch {
      localStorage.removeItem(StorageConstants.STORE_CONTEXT);
      return null;
    }
  }
}
