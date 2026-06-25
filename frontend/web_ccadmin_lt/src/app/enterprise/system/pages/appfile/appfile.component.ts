import { Component, ElementRef, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { AppFileEntity } from '../../model/entity/AppFileEntity';
import { AppFileDto } from '../../model/dto/AppFileDto';
import { AppFileService } from '../../service/AppFileService';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { AppfileComponentRequestDto } from '../../model/dto/AppfileComponentRequestDto';

@Component({
  selector: 'app-appfile',
  templateUrl: './appfile.component.html'
})
export class AppfileComponent {

  @ViewChild('FileInput', { static: false }) FileInput!: ElementRef;
  
  @Input() AppfileComponentRequest : AppfileComponentRequestDto | null = new AppfileComponentRequestDto();
  @Output() ResultForm = new EventEmitter<object>();

  appFileSelect : AppFileDto = new AppFileDto();

  constructor(
    private appFileService : AppFileService
  ){

  }

  base64textString: string = '';

  onFileChange(event: any) {
    const files = event.target.files;
    const file = files[0];

    if (files && file) {

      this.appFileSelect.extension = file.name.split(".")[1];
      this.appFileSelect.type = "data:"+file.type+";base64,";

      const reader = new FileReader();

      reader.onload = this.handleReaderLoaded.bind(this);

      reader.readAsBinaryString(file);
    }
  }

  handleReaderLoaded(readerEvt: any) {

    const binaryString = readerEvt.target.result;
    this.base64textString = btoa(binaryString);

  }

  async Save(){

    const appFile : AppFileDto = this.appFileSelect;

    appFile.base64 = appFile.type + this.base64textString;

    if(this.AppfileComponentRequest!=null){
      appFile.groupTypeFile = this.AppfileComponentRequest.groupTypeFile;
    }

    const rpt : ResponseWsDto = await this.appFileService.Save(appFile);

    if( !rpt.ErrorStatus ){

      let appFileResult : AppFileEntity = rpt.Data;

      console.log(appFileResult);

      this.EmitResultForm(appFileResult);

    }
  }

  EmitResultForm(appFileResult : AppFileEntity)
  {
    this.ResultForm.emit(appFileResult);
    this.FileInput.nativeElement.value = '';
    this.base64textString = "";
  }

}
