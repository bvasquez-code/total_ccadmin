import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ModalDetalleProductoVentaComponent } from './modal-detalle-producto-venta.component';

describe('ModalDetalleProductoVentaComponent', () => {
  let component: ModalDetalleProductoVentaComponent;
  let fixture: ComponentFixture<ModalDetalleProductoVentaComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ModalDetalleProductoVentaComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ModalDetalleProductoVentaComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
