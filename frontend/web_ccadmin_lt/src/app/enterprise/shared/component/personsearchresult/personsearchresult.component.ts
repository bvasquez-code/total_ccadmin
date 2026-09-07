import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-personsearchresult',
  templateUrl: './personsearchresult.component.html',
  styleUrls: ['./personsearchresult.component.css']
})
export class PersonSearchResultComponent {
  @Input() name: string = '';
  @Input() documentType: string = '';
  @Input() documentNumber: string = '';
  @Input() address: string = '';
  @Input() contextLabel: string = '';

  getDocumentLabel(): string {
    const documentType = this.documentType.replace(/^0+/, '');
    if (documentType === '6') return 'RUC';
    if (documentType === '1') return 'DNI';
    if (documentType === '4') return 'Carnet de extranjería';
    return 'Documento';
  }

  isCompany(): boolean {
    return this.documentType.replace(/^0+/, '') === '6';
  }
}
