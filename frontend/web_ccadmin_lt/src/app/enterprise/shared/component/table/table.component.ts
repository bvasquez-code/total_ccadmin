import { Component, Inject, Input, OnInit } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { DataTablaGeneticDto } from '../../model/dto/DataTablaGeneticDto';
import { TableService } from '../../service/table.service';
import { HeaderTableGenericDto } from '../../model/dto/HeaderTableGenericDto';
import { OptionTableGenericDto } from '../../model/dto/OptionTableGenericDto';
import { InfoPageDto } from '../../model/dto/InfoPageDto';
import { ResponsePageSearch } from '../../model/dto/ResponsePageSearch';
import { ActionTableService } from '../../interface/ActionTableService';

@Component({
  selector: 'app-table',
  templateUrl: './table.component.html',
  styleUrls: ['./table.component.css']
})
export class TableComponent<T> implements OnInit{
  private static nextId = 0;
  readonly tableId = `responsive-table-${TableComponent.nextId++}`;
  readonly expandedRows = new Set<T>();

  isMobileColumn(header: HeaderTableGenericDto): boolean {
    if (header.VisibleMobile !== undefined) return header.VisibleMobile;
    if (header.VisibleDesktop === false) return false;
    return header.ColumnAction === true || this.dataSource.Headers
      .filter(column => !column.ColumnAction && column.VisibleDesktop !== false)
      .slice(0, 2).includes(header);
  }

  get displayColumns(): HeaderTableGenericDto[] {
    return this.dataSource.Headers.filter(header => header.VisibleDesktop !== false || this.isMobileColumn(header));
  }

  get mobileColumnCount(): number {
    return this.dataSource.Headers.filter(header => this.isMobileColumn(header)).length;
  }

  get detailColumns(): HeaderTableGenericDto[] {
    return this.dataSource.Headers.filter(header => !header.ColumnAction &&
      (header.VisibleInDetails ?? (header.VisibleDesktop !== false || this.isMobileColumn(header))));
  }

  toggleRow(item: T): void {
    if (this.expandedRows.has(item)) this.expandedRows.delete(item);
    else this.expandedRows.add(item);
  }

  onRowClick(event: MouseEvent, item: T): void {
    const target = event.target as HTMLElement | null;
    if (target?.closest('a, button, input, select, textarea, label, [role="button"]')) return;
    if (this.document.defaultView?.matchMedia('(max-width: 767.98px)').matches && this.detailColumns.length) {
      this.toggleRow(item);
    }
  }

  dataSource : DataTablaGeneticDto<T> = new DataTablaGeneticDto();
  actionTableService? : ActionTableService<T>;

  @Input() set data(data: DataTablaGeneticDto<T>) {
    this.dataSource = data;
    this.expandedRows.clear();
  }

  @Input() set action(actionTableService : ActionTableService<T>) {
    this.actionTableService = actionTableService;
  }


  constructor(@Inject(DOCUMENT) private readonly document: Document)
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
    const page = this.dataSource.DataTable.data;
    if (ValueButton < 1 || ValueButton > page.TotalPages || ValueButton === page.Page) return;
    this.expandedRows.clear();
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

  public getProgressValue(
    item: any,
    key: string,
    FunctionKey?: (...args: any[]) => any
  ): number {
    const value = Number(this.getValue(item, key, FunctionKey));
    if (!Number.isFinite(value)) return 0;
    return Math.min(100, Math.max(0, value));
  }

  public getProgressText(Headers: HeaderTableGenericDto, item: any): string {
    if (Headers.ProgressTextFunction) {
      return Headers.ProgressTextFunction(item);
    }
    return `${this.getProgressValue(item, Headers.key ?? '', Headers.FunctionKey)}%`;
  }

  public getProgressClass(Headers: HeaderTableGenericDto, item: any): string {
    return Headers.ProgressClassFunction
      ? Headers.ProgressClassFunction(item)
      : 'bgc-primary';
  }

  public getCellClass(Headers: HeaderTableGenericDto, item: any): string {
    return Headers.CellClassFunction
      ? Headers.CellClassFunction(item)
      : '';
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
