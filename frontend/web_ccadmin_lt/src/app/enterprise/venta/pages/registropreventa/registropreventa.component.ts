import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { InfoPaginaDto } from 'src/app/enterprise/compartido/entity/InfoPaginaDto';
import { RespuestaPaginacionDto } from 'src/app/enterprise/compartido/entity/RespuestaPaginacionDto';
import { RespuestaWsDto } from 'src/app/enterprise/compartido/entity/RespuestaWsDto';
import { DataSesionService } from 'src/app/enterprise/compartido/service/datasesion.service';
import { BusquedaProductoDto } from '../../entity/BusquedaProductoDto';
import { ProductoBusquedaEntity } from '../../entity/ProductoBusquedaEntity';
import { ProductSearchDto } from '../../../product/model/dto/ProductSearchDto';
import { PreventaService } from '../../service/preventa.service';

@Component({
  selector: 'app-registropreventa',
  templateUrl: './registropreventa.component.html',
  styleUrls: ['./registropreventa.component.css']
})
export class RegistropreventaComponent implements OnInit {
  
  ngOnInit(): void {
    throw new Error('Method not implemented.');
  }

  
  // @ViewChild('txt_filtro_busqueda',{static: false}) txt_filtro_busqueda!: ElementRef<HTMLInputElement>;

  // g_busquedaProductoDto : BusquedaProductoDto = new BusquedaProductoDto();
  // g_RptBusquedaProducto : RespuestaPaginacionDto = new RespuestaPaginacionDto();
  // g_ListaProductoBusqueda : ProductoBusquedaEntity[] = [];
  // g_ListaProductoBusquedaOrganizado : ProductoBusquedaEntity[][] = [];
  // g_ListaBoton : InfoPaginaDto[] = [];

  // productSearch : ProductSearchDto = new ProductSearchDto();

  // constructor(
  //    private g_preventaService : PreventaService
  //   ,private g_DataSesionService : DataSesionService
  // ) 
  // { 
  //   this.g_busquedaProductoDto.cod_local = g_DataSesionService.getSessionStorageDto().cod_local;
  //   this.g_busquedaProductoDto.des_consulta = "";
  //   this.g_busquedaProductoDto.num_pagina_busqueda = 1;
  //   this.BuscarProducto();
  // }

  // ngOnInit(): void {
  // }

  // async BuscarProducto() 
  // {
  //     this.g_ListaProductoBusquedaOrganizado = [];
      
  //     const response : RespuestaWsDto = await this.g_preventaService.BuscarProducto(this.productSearch);

  //     if( response.flg_error === false )
  //     {
  //       this.g_RptBusquedaProducto = response.obj_respuesta;
  //       this.g_ListaProductoBusqueda = this.g_RptBusquedaProducto.listaResultado;
  //       this.OrganizarProductoBusquedaHtml();
  //       this.g_ListaBoton = [];
  //       this.g_ListaBoton = this.CalcularPaginacion(this.g_RptBusquedaProducto);
  //     }

  //     console.log( response );
  // }

  // FiltrarProducto( p_num_pagina_busqueda : number  = 1)
  // {
  //   if( p_num_pagina_busqueda <= 0 ) return;

  //   this.g_busquedaProductoDto.cod_local = this.g_DataSesionService.getSessionStorageDto().cod_local;
  //   this.g_busquedaProductoDto.des_consulta = this.txt_filtro_busqueda.nativeElement.value;
  //   this.g_busquedaProductoDto.num_pagina_busqueda = p_num_pagina_busqueda;
  //   this.BuscarProducto();
  // }

  // OrganizarProductoBusquedaHtml()
  // {
  //   let l_num_Producto_fila : number = 4;
  //   let l_num_contador : number = 0;
  //   let l_SubListaProducto : ProductoBusquedaEntity[] = [];

  //   for (let index = 0; index < this.g_ListaProductoBusqueda.length; index++) 
  //   {
  //     const l_Producto :ProductoBusquedaEntity = this.g_ListaProductoBusqueda[index];

  //     if( l_num_Producto_fila > l_num_contador )
  //     {
  //       l_SubListaProducto.push(l_Producto);
  //       l_num_contador++;
  //     }
  //     else
  //     {
  //       this.g_ListaProductoBusquedaOrganizado.push(l_SubListaProducto);
  //       l_num_contador = 0;
  //       l_SubListaProducto = [];

  //       l_SubListaProducto.push(l_Producto);
  //       l_num_contador++;
  //     }
      
  //   }

  //   if( l_SubListaProducto.length > 0 )
  //   {
  //     this.g_ListaProductoBusquedaOrganizado.push(l_SubListaProducto);
  //   }
  //   console.log(this.g_ListaProductoBusquedaOrganizado);
  // }


  // CalcularPaginacion(P_InfoPaginacion : RespuestaPaginacionDto):InfoPaginaDto[]
  // {
  //     let l_numero_paginas : number[] = [];
  //     let l_ListaBoton : InfoPaginaDto[] = [];

  //     for (let index = 0; index < P_InfoPaginacion.num_total_paginas; index++) {
        
  //       l_numero_paginas.push(index+1);
        
  //     }

  //     let BotonPrev : InfoPaginaDto = new InfoPaginaDto();

  //     BotonPrev.flg_boton_activo = true;
  //     BotonPrev.nom_boton = "Prev";
  //     BotonPrev.valor_boton = P_InfoPaginacion.num_pagina_actual - 1;

  //     l_ListaBoton.push(BotonPrev);

  //     if( l_numero_paginas.length < 13 )
  //     {
  //       for (let i = 0; i < l_numero_paginas.length; i++) {
        
  //         let Boton : InfoPaginaDto = new InfoPaginaDto();

  //         Boton.flg_boton_activo = true;
  //         Boton.nom_boton = l_numero_paginas[i].toString();
  //         Boton.valor_boton = l_numero_paginas[i];

  //         l_ListaBoton.push(Boton);   
  //       }

  //     }
  //     else
  //     {
  //       let Botones = [1,2,3
  //         ,P_InfoPaginacion.num_pagina_actual - 2
  //         ,P_InfoPaginacion.num_pagina_actual - 1
  //         ,P_InfoPaginacion.num_pagina_actual
  //         ,P_InfoPaginacion.num_pagina_actual + 1
  //         ,P_InfoPaginacion.num_pagina_actual + 2
  //         ,l_numero_paginas.length-1
  //         ,l_numero_paginas.length
  //         ,l_numero_paginas.length+1
  //       ]

  //       let valor_boton_anterior = -2;

  //       for (let i = 0; i < Botones.length; i++) {
        
  //         if( Botones[i] > 0 &&  Botones[i] <= P_InfoPaginacion.num_total_paginas )
  //         {

  //           let Existe : boolean = (l_ListaBoton.filter( 
  //             e => e.valor_boton === Botones[i] 
  //             && e.nom_boton !== "Prev" 
  //             && e.nom_boton !== "Next" 
  //           ).length > 0);

  //           if( Existe ) continue;

  //           if( Botones[i] - 1 !== valor_boton_anterior && valor_boton_anterior !== -2 )
  //           {
  //             let BotonCentro : InfoPaginaDto = new InfoPaginaDto();
  //             BotonCentro.flg_boton_activo = true;
  //             BotonCentro.nom_boton = "...";
  //             BotonCentro.valor_boton = -2;
  //             l_ListaBoton.push(BotonCentro);
  //           }

  //           let BotonCentro : InfoPaginaDto = new InfoPaginaDto();
  //           BotonCentro.flg_boton_activo = true;
  //           BotonCentro.nom_boton = Botones[i].toString();
  //           BotonCentro.valor_boton = Botones[i];
  //           l_ListaBoton.push(BotonCentro);

  //           valor_boton_anterior = Botones[i];
  //         }
          
  //       }

  //     }

  //     let BotonNext : InfoPaginaDto = new InfoPaginaDto();

  //     BotonNext.flg_boton_activo = true;
  //     BotonNext.nom_boton = "Next";
  //     BotonNext.valor_boton = P_InfoPaginacion.num_pagina_actual + 1;

  //     if( BotonNext.valor_boton === P_InfoPaginacion.num_total_paginas + 1)
  //     {
  //       BotonNext.valor_boton = 0;
  //     }

  //     l_ListaBoton.push(BotonNext);

  //     console.log(l_ListaBoton);

  //     for (let i = 0; i < l_ListaBoton.length; i++) {

  //       let element = l_ListaBoton[i];

  //       if( P_InfoPaginacion.num_pagina_actual === element.valor_boton )
  //       {
  //         element.flg_boton_actual = true;
  //       }
        
  //     }
  //     return l_ListaBoton;
  // }

  // async ObtenerProducto(p_cod_producto : string) 
  // {
  //   const response : RespuestaWsDto = await this.g_preventaService.ObtenerProducto(
  //     {
  //       cod_producto : p_cod_producto
  //     }
  //   );

  //   if( response.flg_error === false )
  //   {
      
  //   }
  // }

}
