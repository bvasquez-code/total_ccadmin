import { Component } from '@angular/core';
import { ModalService } from '../../service/ModalService';

@Component({
  selector: 'app-modalalert',
  templateUrl: './modalalert.component.html'
})
export class ModalalertComponent {

  constructor(public modalService: ModalService) { }

  confirmAction(){
    
  }
  
}
