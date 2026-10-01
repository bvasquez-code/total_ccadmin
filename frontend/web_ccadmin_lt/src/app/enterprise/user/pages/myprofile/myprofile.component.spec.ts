import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { SessionStorageDto } from '../../../compartido/entity/SessionStorageDto';
import { DataSesionService } from '../../../compartido/service/datasesion.service';
import { MyProfileComponent } from './myprofile.component';

describe('MyProfileComponent', () => {
  let fixture: ComponentFixture<MyProfileComponent>;
  let session: SessionStorageDto;

  beforeEach(async () => {
    session = new SessionStorageDto();
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      declarations: [MyProfileComponent],
      providers: [{
        provide: DataSesionService,
        useValue: { getSessionStorageDto: () => session }
      }]
    }).compileComponents();
    fixture = TestBed.createComponent(MyProfileComponent);
  });

  it('muestra los datos propios sin exponer credenciales ni identificadores internos', () => {
    session.Names = ' Ana Pérez ';
    session.UserCod = ' ANA01 ';
    session.Email = ' ana@example.com ';
    session.StoreCod = ' T001 ';
    session.Token = 'token-privado-no-mostrar';
    session.SessionID = 987654321;
    session.PersonCod = 'persona-interna-no-mostrar';
    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;
    const values = Array.from(element.querySelectorAll('dd')).map(field => field.textContent?.trim());
    expect(values).toEqual(['Ana Pérez', 'ANA01', 'ana@example.com', 'T001']);
    expect(element.innerHTML).not.toContain(session.Token);
    expect(element.innerHTML).not.toContain(String(session.SessionID));
    expect(element.innerHTML).not.toContain(session.PersonCod);
    expect(element.querySelector('input, textarea, select')).toBeNull();
  });

  it('indica los datos ausentes sin mostrar valores vacíos', () => {
    session.Names = '   ';
    session.Email = '   ';
    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;
    const values = Array.from(element.querySelectorAll('dd')).map(field => field.textContent?.trim());
    expect(values).toEqual([
      'No registrado', 'No registrado', 'No registrado', 'Sin tienda seleccionada'
    ]);
  });
});
