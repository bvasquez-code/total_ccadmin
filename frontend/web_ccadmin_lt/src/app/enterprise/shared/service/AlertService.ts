
import {Injectable} from '@angular/core';
import Swal, { SweetAlertResult } from 'sweetalert2';

@Injectable({
    providedIn: 'root'
})
export class AlertService
{


    selectStore(stores: { StoreCod: string; Name: string }[], confirm: (storeCod: string) => Promise<boolean>) {
        const options: Record<string, string> = {};
        stores.forEach(store => options[store.StoreCod] = store.Name || store.StoreCod);
        return Swal.fire({
            title: 'Selecciona una tienda',
            text: 'Elige la tienda en la que deseas trabajar.',
            input: 'select', inputOptions: options, inputPlaceholder: 'Seleccionar tienda',
            confirmButtonText: 'Ingresar', showCancelButton: true, cancelButtonText: 'Cancelar',
            allowOutsideClick: false, allowEscapeKey: false, showLoaderOnConfirm: true,
            inputValidator: value => value ? null : 'Debes seleccionar una tienda',
            preConfirm: async value => {
                if (!await confirm(value)) {
                    Swal.showValidationMessage('No se pudo seleccionar la tienda. Intenta nuevamente.');
                    return false;
                }
                return value;
            }
        });
    }

    waring(text : string = "Esta acciÃ³n no podra ser revertida",title : string = "Â¿EstÃ¡s seguro?") : Promise<SweetAlertResult<any>>
    {
        return Swal.fire({
            title: title,
            text: text,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#3085d6',
            cancelButtonColor: '#d33',
            confirmButtonText: 'SÃ­, confirmar',
            cancelButtonText: 'No, cancelar'
        });
    }

    waringHtml(html : string,title : string = "Confirmar") : Promise<SweetAlertResult<any>>
    {
        return Swal.fire({
            title: title,
            html: html,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#3085d6',
            cancelButtonColor: '#d33',
            confirmButtonText: 'Si, confirmar',
            cancelButtonText: 'No, cancelar'
        });
    }

    warning(text: string, title: string = "Advertencia"): Promise<SweetAlertResult<any>>
    {
        return Swal.fire({
            title: title,
            text: text,
            icon: 'warning',
            confirmButtonColor: '#3085d6',
            confirmButtonText: 'Aceptar'
        });
    }

}
