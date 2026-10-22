import { CommonModule } from '@angular/common';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TableComponent } from './table.component';
import { FormatoMonedaPeruanaPipe } from '../../pipe/FormatoMonedaPeruana.pipe';
import { DataTablaGeneticDto } from '../../model/dto/DataTablaGeneticDto';
import { HeaderTableGenericDto } from '../../model/dto/HeaderTableGenericDto';

describe('TableComponent responsive', () => {
  let fixture: ComponentFixture<TableComponent<any>>;
  let component: TableComponent<any>;
  let data: DataTablaGeneticDto<any>;
  const record = { code: '001', name: 'Producto', amount: 1250, secret: 'oculto' };

  beforeEach(async () => {
    record.name = 'Producto';
    await TestBed.configureTestingModule({
      imports: [CommonModule],
      declarations: [TableComponent, FormatoMonedaPeruanaPipe]
    }).compileComponents();
    fixture = TestBed.createComponent(TableComponent);
    component = fixture.componentInstance;
    data = new DataTablaGeneticDto();
    data.Headers = [
      { Name: 'Código', key: 'code' },
      { Name: 'Nombre', key: 'name' },
      { Name: 'Importe', key: 'amount', IsMoney: true },
      { Name: 'Opciones', ColumnAction: true, Options: [] }
    ];
    data.DataTable.data.resultSearch = [record];
    data.DataTable.data.Page = 1;
    data.DataTable.data.TotalPages = 3;
    component.data = data;
    fixture.detectChanges();
  });

  it('defaults to two data columns plus actions and keeps every field in details', () => {
    expect(data.Headers.filter(header => component.isMobileColumn(header)).map(header => header.Name))
      .toEqual(['Código', 'Nombre', 'Opciones']);
    expect(component.detailColumns.length).toBe(3);
  });

  it('honors per-view overrides and excludes hidden fields from details by default', () => {
    data.Headers[0].VisibleMobile = false;
    data.Headers[2].VisibleMobile = true;
    const hidden: HeaderTableGenericDto = { key: 'secret', VisibleDesktop: false };
    data.Headers.push(hidden);
    expect(component.isMobileColumn(data.Headers[0])).toBeFalse();
    expect(component.isMobileColumn(data.Headers[2])).toBeTrue();
    expect(component.displayColumns).not.toContain(hidden);
    expect(component.detailColumns).not.toContain(hidden);
    hidden.VisibleInDetails = true;
    expect(component.detailColumns).toContain(hidden);
  });

  it('opens and closes details by tapping the row, preserving money formatting', () => {
    spyOn(window, 'matchMedia').and.returnValue({ matches: true } as MediaQueryList);
    const row: HTMLElement = fixture.nativeElement.querySelector('.summary-row');
    row.click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.detail-row td').getAttribute('colspan')).toBe('3');
    expect(fixture.nativeElement.querySelector('.record-details').textContent).toContain('S/. 1,250');
    row.click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.detail-row')).toBeNull();
  });

  it('only toggles row clicks on mobile and leaves action clicks independent', () => {
    const media = spyOn(window, 'matchMedia').and.returnValue({ matches: true } as MediaQueryList);
    const action = jasmine.createSpy('action');
    const select = jasmine.createSpy('select');
    component.actionTableService = { getDataRow: select } as any;
    data.Headers[3].Options = [
      { Type: 'Action', Name: 'fa fa-check', Action: action },
      { Type: 'Action', Name: 'fa fa-trash', Function: () => false },
      { Type: 'Url', Name: 'fa fa-eye', Url: '/record/{code}' },
      { Type: 'Modal', Name: 'fa fa-edit', ID: 'edit-record' }
    ];
    data.Headers[3].Id = ['code'];
    fixture.detectChanges();
    const actions: HTMLElement = fixture.nativeElement.querySelector('.table-actions');
    expect(actions.children.length).toBe(3);
    expect(actions.querySelector('a')?.getAttribute('href')).toBe('/record/001');
    expect(actions.querySelector('[data-toggle="modal"]')?.getAttribute('data-target')).toBe('#edit-record');
    actions.querySelector('button')!.click();
    expect(action).toHaveBeenCalledWith(record);
    expect(select).toHaveBeenCalledWith(record);
    expect(component.expandedRows.size).toBe(0);
    const cell: HTMLElement = fixture.nativeElement.querySelector('.summary-row td');
    cell.click();
    expect(component.expandedRows.has(record)).toBeTrue();
    media.and.returnValue({ matches: false } as MediaQueryList);
    cell.click();
    expect(component.expandedRows.has(record)).toBeTrue();
  });

  it('fits mobile widths and switches visible columns and pagination at the breakpoint', async () => {
    record.name = 'Producto con una descripción extensa y un código ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    component.toggleRow(record);
    fixture.detectChanges();
    const styles = Array.from(document.querySelectorAll('style')).map(style => style.textContent).join('\n');
    for (const width of [320, 390, 767, 1024]) {
      const frame = document.createElement('iframe');
      frame.style.width = `${width}px`;
      frame.style.height = '800px';
      document.body.appendChild(frame);
      try {
        await new Promise<void>(resolve => {
          frame.onload = () => resolve();
          frame.srcdoc = `<style>${styles}</style>${fixture.nativeElement.outerHTML}`;
        });
        const page = frame.contentDocument!;
        const view = frame.contentWindow!;
        const columns = Array.from(page.querySelectorAll('thead th'));
        const visible = columns.filter(column => view.getComputedStyle(column).display !== 'none');
        expect(visible.length).withContext(`columns at ${width}px`).toBe(width < 768 ? 3 : 4);
        expect(view.getComputedStyle(columns[2]).display === 'none').toBe(width < 768);
        expect(view.getComputedStyle(page.querySelector('.mobile-pagination')!).display === 'none').toBe(width >= 768);
        expect(page.documentElement.scrollWidth).withContext(`overflow at ${width}px`).toBeLessThanOrEqual(width);
      } finally {
        frame.remove();
      }
    }
  });

  it('rejects invalid pages and resets details on navigation or replaced data', () => {
    const filter = jasmine.createSpy('filter');
    component.actionTableService = { filter } as any;
    component.toggleRow(record);
    [0, -2, 4, 1].forEach(page => component.findResultPage(page));
    expect(filter).not.toHaveBeenCalled();
    component.findResultPage(2);
    expect(filter).toHaveBeenCalledOnceWith(2);
    expect(component.expandedRows.size).toBe(0);
    component.toggleRow(record);
    component.data = data;
    expect(component.expandedRows.size).toBe(0);
  });
});
