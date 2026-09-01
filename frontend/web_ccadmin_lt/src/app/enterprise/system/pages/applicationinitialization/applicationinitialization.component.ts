import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { DataSesionService } from 'src/app/enterprise/compartido/service/datasesion.service';
import { StoreEntity } from 'src/app/enterprise/shared/model/entity/StoreEntity';

interface InitializationStep {
  title: string;
  description: string;
  icon: string;
}

@Component({
  selector: 'app-applicationinitialization',
  templateUrl: './applicationinitialization.component.html',
  styleUrls: ['./applicationinitialization.component.css']
})
export class ApplicationInitializationComponent implements OnInit {

  readonly Steps: InitializationStep[] = [
    {
      title: 'Compañía',
      description: 'Identidad y domicilio fiscal',
      icon: 'fa-building'
    },
    {
      title: 'Tienda',
      description: 'Primer establecimiento',
      icon: 'fa-store'
    },
    {
      title: 'Administrador',
      description: 'Primer usuario operativo',
      icon: 'fa-user-shield'
    },
    {
      title: 'Producto',
      description: 'Primera creación rápida',
      icon: 'fa-box-open'
    }
  ];

  CurrentStep: number = 0;
  ConfiguredStoreCod: string = '';

  private readonly progressStorageKey = 'ApplicationInitializationStep';
  private CompanyStepPending: boolean = true;
  private StoreStepPending: boolean = true;

  constructor(
    private dataSesionService: DataSesionService,
    private router: Router,
    private toastrService: ToastrService
  ) {
  }

  ngOnInit(): void {
    const savedStep = Number(sessionStorage.getItem(this.progressStorageKey));
    const hasSavedProgress = Number.isInteger(savedStep)
      && savedStep >= 0
      && savedStep < this.Steps.length;
    const session = this.dataSesionService.getSessionStorageDto();
    if (session.UserCod.toUpperCase() !== 'ROOT'
        || (!this.dataSesionService.RequiresApplicationInitialization()
          && !hasSavedProgress)) {
      void this.router.navigate(['/']);
      return;
    }

    this.ConfiguredStoreCod = session.DefaultStoreCod || session.StoreCod || '';
    this.CompanyStepPending = hasSavedProgress && savedStep > 0
      ? false
      : session.CompanyInitializationPending;
    this.StoreStepPending = hasSavedProgress && savedStep > 1
      ? false
      : session.StoreInitializationPending;

    this.CurrentStep = hasSavedProgress
        ? savedStep
        : (session.CompanyInitializationPending ? 0 : 1);
  }

  companyConfigured(): void {
    this.CompanyStepPending = false;
    this.unlockApplicationWhenDefaultsAreConfigured();
    this.advanceFrom(0);
  }

  storeConfigured(store: StoreEntity): void {
    this.ConfiguredStoreCod = store?.StoreCod || this.ConfiguredStoreCod;
    this.StoreStepPending = false;
    this.unlockApplicationWhenDefaultsAreConfigured();
    this.advanceFrom(1);
  }

  administratorConfigured(): void {
    this.advanceFrom(2);
  }

  productConfigured(): void {
    if (this.CurrentStep !== 3) return;

    sessionStorage.removeItem(this.progressStorageKey);
    this.dataSesionService.CompleteApplicationInitialization();
  }

  skipProductConfiguration(): void {
    if (this.CurrentStep !== 3) return;

    sessionStorage.removeItem(this.progressStorageKey);
    this.dataSesionService.CompleteApplicationInitialization();
    this.toastrService.info('Puede crear productos y registrar su stock más adelante.');
    void this.router.navigate(['/enterprise/product/pages/listProduct']);
  }

  isCompleted(stepIndex: number): boolean {
    return stepIndex < this.CurrentStep;
  }

  private advanceFrom(stepIndex: number): void {
    if (this.CurrentStep !== stepIndex) return;

    this.CurrentStep = stepIndex === 0 && !this.StoreStepPending
      ? 2
      : stepIndex + 1;
    sessionStorage.setItem(this.progressStorageKey, String(this.CurrentStep));
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  private unlockApplicationWhenDefaultsAreConfigured(): void {
    if (this.CompanyStepPending || this.StoreStepPending) return;
    this.dataSesionService.CompleteApplicationInitialization();
  }
}
