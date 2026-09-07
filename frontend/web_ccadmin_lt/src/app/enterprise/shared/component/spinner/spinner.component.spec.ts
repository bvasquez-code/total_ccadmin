import { CommonModule } from '@angular/common';
import { Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { Router } from '@angular/router';
import { AppComponent } from '../../../../app.component';
import { DataSesionService } from '../../../compartido/service/datasesion.service';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SpinnerService } from '../../service/spinner.service';
import { SpinnerComponent } from './spinner.component';

describe('SpinnerComponent', () => {
  let fixture: ComponentFixture<SpinnerComponent>;
  let spinnerService: SpinnerService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CommonModule], declarations: [SpinnerComponent]
    }).compileComponents();
    fixture = TestBed.createComponent(SpinnerComponent);
    spinnerService = TestBed.inject(SpinnerService);
    fixture.detectChanges();
  });

  it('renders a viewport overlay above modals while loading and removes it after the final response', () => {
    spinnerService.show();
    spinnerService.show();
    fixture.detectChanges();
    const overlay: HTMLElement = fixture.nativeElement.querySelector('.loading-overlay');
    expect(overlay.textContent).toContain('Procesando solicitud');
    expect(getComputedStyle(overlay).position).toBe('fixed');
    expect(Number(getComputedStyle(overlay).zIndex)).toBeGreaterThan(999999);
    expect(getComputedStyle(overlay.querySelector('.loading-indicator')!).animationName).not.toBe('none');
    spinnerService.hide();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.loading-overlay')).not.toBeNull();
    spinnerService.hide();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.loading-overlay')).toBeNull();
  });

  it('blocks page shortcuts and scrolling until the loader closes', () => {
    const shortcut = jasmine.createSpy('shortcut');
    document.addEventListener('keydown', shortcut);
    try {
      spinnerService.show();
      const key = new KeyboardEvent('keydown', { key: 'b', altKey: true, bubbles: true, cancelable: true });
      document.dispatchEvent(key);
      expect(key.defaultPrevented).toBeTrue();
      expect(shortcut).not.toHaveBeenCalled();
      const wheel = new WheelEvent('wheel', { cancelable: true });
      document.dispatchEvent(wheel);
      expect(wheel.defaultPrevented).toBeTrue();
      expect(document.documentElement.style.overflow).toBe('hidden');
      spinnerService.hide();
      document.dispatchEvent(new KeyboardEvent('keydown', { key: 'b', altKey: true }));
      expect(shortcut).toHaveBeenCalledTimes(1);
    } finally {
      document.removeEventListener('keydown', shortcut);
    }
  });

  it('restores previous scrolling styles and removes its event listeners on destruction', () => {
    const pageStyle = document.documentElement.style;
    const originalOverflow = pageStyle.getPropertyValue('overflow');
    const originalPriority = pageStyle.getPropertyPriority('overflow');
    try {
      pageStyle.setProperty('overflow', 'auto', 'important');
      spinnerService.show();
      fixture.destroy();
      expect(pageStyle.getPropertyValue('overflow')).toBe('auto');
      expect(pageStyle.getPropertyPriority('overflow')).toBe('important');
      const key = new KeyboardEvent('keydown', { key: 'Enter', cancelable: true });
      document.dispatchEvent(key);
      expect(key.defaultPrevented).toBeFalse();
    } finally {
      pageStyle.setProperty('overflow', originalOverflow, originalPriority);
    }
  });
});

@Component({ selector: 'router-outlet', template: '' })
class PendingRequestComponent {
  constructor(spinnerService: SpinnerService) { spinnerService.show(); }
}

describe('Global loader during initial page requests', () => {
  it('can block the application when a routed page starts loading during creation', async () => {
    await TestBed.configureTestingModule({
      imports: [CommonModule],
      declarations: [AppComponent, SpinnerComponent, PendingRequestComponent],
      providers: [
        { provide: DataSesionService, useValue: { SessionExists: () => true } },
        { provide: Router, useValue: {} }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
    const fixture = TestBed.createComponent(AppComponent);
    expect(() => fixture.detectChanges()).not.toThrow();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.loading-overlay')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.body-container').hasAttribute('inert')).toBeTrue();
    TestBed.inject(SpinnerService).hide();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.loading-overlay')).toBeNull();
    expect(fixture.nativeElement.querySelector('.body-container').hasAttribute('inert')).toBeFalse();
  });
});
