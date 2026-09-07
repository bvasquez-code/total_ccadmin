import { PersonEntity } from '../model/entity/PersonEntity';

export class PersonNameHelper {
  static applyCommercialNameFallback(person: PersonEntity): void {
    if (person.PersonType !== '04' && person.DocumentType !== '06') return;
    const commercialName = (person.CommercialName || '').trim();
    person.CommercialName = commercialName && commercialName !== '-'
      ? commercialName
      : (person.BusinessName || '').trim();
  }
}
