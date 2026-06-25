import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { DataTablaGeneticDto } from '../../model/dto/DataTablaGeneticDto';
import { TableService } from '../../service/table.service';
import { HeaderTableGenericDto } from '../../model/dto/HeaderTableGenericDto';
import { OptionTableGenericDto } from '../../model/dto/OptionTableGenericDto';
import { InfoPageDto } from '../../model/dto/InfoPageDto';
import { ResponsePageSearch } from '../../model/dto/ResponsePageSearch';
import { ActionTableService } from '../../interface/ActionTableService';

@Component({
  selector: 'app-table',
  templateUrl: './table.component.html'
})
export class TableComponent<T> implements OnInit{

  dataSource : DataTablaGeneticDto<T> = new DataTablaGeneticDto();
  actionTableService? : ActionTableService<T>;

  @Input() set data(data: DataTablaGeneticDto<T>) {
    this.dataSource = data;
  }

  @Input() set action(actionTableService : ActionTableService<T>) {
    this.actionTableService = actionTableService;
  }


  constructor()
  {
  }

  ngOnInit(): void {

  }

  get LoadingPages():InfoPageDto[]
  {
    const ResponsePage : ResponsePageSearch<T> = this.dataSource.DataTable.data;
    if(ResponsePage.TotalPages > 13){
      return this.LoadingPagesMax13();
    }else{
      return this.LoadingPagesMin13();
    }
  }

  LoadingPagesMax13():InfoPageDto[]
  {
    let InfoPageList : InfoPageDto[] = [];
    let InfoPageListFinal : InfoPageDto[] = [];
    const ResponsePage : ResponsePageSearch<T> = this.dataSource.DataTable.data;

    for (let index = 0; index < ResponsePage.TotalPages; index++) 
    {
      let Page = index + 1;
      let Button : InfoPageDto = new InfoPageDto();
      Button.IsActive = true;
      Button.NameButton = Page.toString();
      Button.ValueButton = Page;

      if(ResponsePage.Page === Page){
        Button.IsCurrent = true;
      }
      InfoPageList.push(Button);   
    }

    InfoPageListFinal = InfoPageList.filter( e =>
      e.ValueButton == 1 ||
      e.ValueButton == 2 ||
      e.ValueButton == 3 ||
      e.ValueButton === (ResponsePage.Page -2) ||
      e.ValueButton === (ResponsePage.Page - 1) ||
      e.ValueButton === ResponsePage.Page ||
      e.ValueButton === (ResponsePage.Page + 1) ||
      e.ValueButton === (ResponsePage.Page + 2) ||
      e.ValueButton == ResponsePage.TotalPages ||
      e.ValueButton == (ResponsePage.TotalPages - 1) ||
      e.ValueButton == (ResponsePage.TotalPages - 2) ||
      e.NameButton === "Next" ||
      e.NameButton === "Prev"
    );

    if(ResponsePage.Page - 2 > 3){
      let ButtonCenter : InfoPageDto = new InfoPageDto();
      ButtonCenter.IsActive = true;
      ButtonCenter.NameButton = "...";
      ButtonCenter.ValueButton = -2;


      let InfoPageListTmp1 : InfoPageDto[] = InfoPageListFinal.filter( e => e.ValueButton < (ResponsePage.Page - 2));
      InfoPageListTmp1.push(ButtonCenter);
      let InfoPageListTmp2 : InfoPageDto[] = InfoPageListFinal.filter( e => e.ValueButton >= (ResponsePage.Page - 2));

      InfoPageListFinal = [...InfoPageListTmp1, ...InfoPageListTmp2];
    }

    if(ResponsePage.Page + 2 < ResponsePage.TotalPages){
      let ButtonCenter : InfoPageDto = new InfoPageDto();
      ButtonCenter.IsActive = true;
      ButtonCenter.NameButton = "...";
      ButtonCenter.ValueButton = -2;
      
      let InfoPageListTmp1 : InfoPageDto[] = InfoPageListFinal.filter( e => e.ValueButton < (ResponsePage.Page + 3));
      InfoPageListTmp1.push(ButtonCenter);
      let InfoPageListTmp2 : InfoPageDto[] = InfoPageListFinal.filter( e => e.ValueButton >= (ResponsePage.Page + 3));

      InfoPageListFinal = [...InfoPageListTmp1, ...InfoPageListTmp2];
    }

    let ButtonPreview : InfoPageDto = new InfoPageDto().buildPrev(ResponsePage.Page);
    InfoPageListFinal.unshift(ButtonPreview);

    let ButtonNext : InfoPageDto = new InfoPageDto().buildNext(ResponsePage.Page);
    if( ButtonNext.ValueButton === ResponsePage.TotalPages + 1)
    {
      ButtonNext.ValueButton = 0;
    }
    InfoPageListFinal.push(ButtonNext);

    return InfoPageListFinal;
  }

  LoadingPagesMin13():InfoPageDto[]
  {
    let ResponsePage : ResponsePageSearch<T> = this.dataSource.DataTable.data;

    const InfoPageList : InfoPageDto[] = [];

    let ButtonPreview : InfoPageDto = new InfoPageDto().buildPrev(ResponsePage.Page);
    InfoPageList.push(ButtonPreview);

    for (let i = 0; i < ResponsePage.TotalPages; i++) {
      
      let Page = i + 1;
      let Button : InfoPageDto = new InfoPageDto();

      Button.IsActive = true;
      Button.NameButton = Page.toString();
      Button.ValueButton = Page;

      if( ResponsePage.Page === Button.ValueButton )
      {
        Button.IsCurrent = true;
      }

      InfoPageList.push(Button);   
    }

    let ButtonNext : InfoPageDto = new InfoPageDto().buildNext(ResponsePage.Page);

    if( ButtonNext.ValueButton === ResponsePage.TotalPages + 1)
    {
      ButtonNext.ValueButton = 0;
    }

    InfoPageList.push(ButtonNext);

    return InfoPageList;
  }

  findResultPage(ValueButton : number)
  {
    console.log(ValueButton);
    this.actionTableService?.filter(ValueButton);
  }

  getDataRow(item : any):void
  {
    this.actionTableService?.getDataRow(item);
  }

  public getValue(item : any, key : string, FunctionKey? : (...args: any[]) => any)
  {
    if( FunctionKey && key ){
      return FunctionKey(item);
    }
    else if(key){
      return String(item[key]);
    }
    return "";
  }

  generateUrl(optionTableGeneric? : OptionTableGenericDto, item? : any, Headers? :HeaderTableGenericDto):string | any
  {

    if(optionTableGeneric?.FunctionUrl){
      return optionTableGeneric?.FunctionUrl(item);
    }

    let url : string = (optionTableGeneric?.Url) ? optionTableGeneric.Url : "";

    if(Headers?.Id)
    {
      for (let i = 0; i < Headers.Id.length; i++) {   

        url = url.replace("{"+Headers.Id[i]+"}",item[Headers.Id[i]]);  
  
      }
    }
    return url;
  }

  public showOption(OptionTableGeneric :OptionTableGenericDto,item : T):boolean
  {
    if(!OptionTableGeneric.Function){
      return true;
    }

    return OptionTableGeneric.Function(item);

  }

  private existSpot(text : string) : boolean{
    const regex = /\./;
    return regex.test(text);
  }

}
