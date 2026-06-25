import { Component, Input, OnInit } from '@angular/core';
import { ActionModalConfirmService } from '../../interface/ActionModalConfirmService';

@Component({
  selector: 'app-modalconfirm',
  templateUrl: './modalconfirm.component.html'
})
export class ModalconfirmComponent implements OnInit {

  constructor() { }

  htmlId : string = "";
  htmlTitle : string = "Confirmar acción";
  htmlMessage : string = "¿Esta seguro que desea realizar esta acción?";
  actionModalConfirmService? : ActionModalConfirmService;


  @Input() set id(htmlId: string) {
    this.htmlId = htmlId;
  }

  @Input() set title(htmlTitle: string) {
    this.htmlTitle = htmlTitle;
  }

  @Input() set message(htmlMessage: string) {
    this.htmlMessage = htmlMessage;
  }

  @Input() set action(actionModalConfirmService : ActionModalConfirmService)
  {
    this.actionModalConfirmService = actionModalConfirmService;
  }

  ngOnInit(): void {
  }

  actionModal()
  {
    this.actionModalConfirmService?.actionModal(this.htmlId);
  }

}
