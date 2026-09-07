import { DOCUMENT } from '@angular/common';
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { SpinnerService } from '../../service/spinner.service';

@Component({
  selector: 'app-spinner',
  templateUrl: './spinner.component.html',
  styleUrls: ['./spinner.component.css']
})
export class SpinnerComponent implements OnInit, OnDestroy {
  readonly IsLoading$ = this.spinnerService.IsLoading$;
  private loadingSubscription?: Subscription;
  private previousOverflow: string = '';
  private previousOverflowPriority: string = '';
  private isScrollLocked: boolean = false;
  private readonly blockedEvents = ['keydown', 'keyup', 'keypress', 'wheel', 'touchmove'];
  private readonly blockInteraction = (event: Event): void => {
    if (!this.spinnerService.isLoading) return;
    event.preventDefault();
    event.stopImmediatePropagation();
  };

  constructor(
    private spinnerService: SpinnerService,
    @Inject(DOCUMENT) private document: Document
  ) { }

  ngOnInit(): void {
    this.loadingSubscription = this.IsLoading$.subscribe(isLoading => {
      const pageStyle = this.document.documentElement.style;
      if (isLoading && !this.isScrollLocked) {
        this.previousOverflow = pageStyle.getPropertyValue('overflow');
        this.previousOverflowPriority = pageStyle.getPropertyPriority('overflow');
        pageStyle.setProperty('overflow', 'hidden');
        this.isScrollLocked = true;
      } else if (!isLoading) {
        this.restoreScrolling();
      }
    });
    this.blockedEvents.forEach(eventName => {
      this.document.addEventListener(eventName, this.blockInteraction, { capture: true, passive: false });
    });
  }

  ngOnDestroy(): void {
    this.loadingSubscription?.unsubscribe();
    this.restoreScrolling();
    this.blockedEvents.forEach(eventName => {
      this.document.removeEventListener(eventName, this.blockInteraction, true);
    });
  }
  private restoreScrolling(): void {
    if (!this.isScrollLocked) return;
    const pageStyle = this.document.documentElement.style;
    if (this.previousOverflow) {
      pageStyle.setProperty('overflow', this.previousOverflow, this.previousOverflowPriority);
    } else {
      pageStyle.removeProperty('overflow');
    }
    this.isScrollLocked = false;
  }
}
