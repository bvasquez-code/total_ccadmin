import { Component, HostListener } from '@angular/core';
import { Router } from '@angular/router';
import { DataSesionService } from './enterprise/compartido/service/datasesion.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {

  title = 'ccadmin2';

  constructor(
    private dataSesionService: DataSesionService,
    private router: Router
  ) {
  }

  getSession(): boolean {
    return this.dataSesionService.SessionExists();
  }

  @HostListener('window:storage', ['$event'])
  synchronizeSession(event: StorageEvent): void {
    if (!this.dataSesionService.IsSessionSynchronizationEvent(event)) {
      return;
    }

    this.dataSesionService.ClearCurrentTabData();
    this.dataSesionService.ReloadSession();

    if (!this.dataSesionService.SessionExists()) {
      this.router.navigate(['/login']);
      return;
    }

    location.reload();
  }

}
