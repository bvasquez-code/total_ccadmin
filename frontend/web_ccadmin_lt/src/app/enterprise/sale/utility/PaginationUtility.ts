import { InfoPaginaDto } from 'src/app/enterprise/compartido/entity/InfoPaginaDto';
import { RespuestaPaginacionDto } from 'src/app/enterprise/compartido/entity/RespuestaPaginacionDto';

export class PaginationUtil {

    static buildButtons<T>(info: RespuestaPaginacionDto<T>): InfoPaginaDto[] {
        const total = info.num_total_paginas ?? 0;
        const current = info.num_pagina_actual ?? 1;

        if (total <= 0) return [];

        const buttons: InfoPaginaDto[] = [];

        const prev = new InfoPaginaDto();
        prev.flg_boton_activo = true;
        prev.nom_boton = 'Prev';
        prev.valor_boton = current - 1;
        buttons.push(prev);

        const addPage = (p: number) => {
            const b = new InfoPaginaDto();
            b.flg_boton_activo = true;
            b.nom_boton = String(p);
            b.valor_boton = p;
            buttons.push(b);
        };

        const addDots = () => {
            const b = new InfoPaginaDto();
            b.flg_boton_activo = true;
            b.nom_boton = '...';
            b.valor_boton = -2;
            buttons.push(b);
        };

        if (total < 13) {
            for (let p = 1; p <= total; p++) addPage(p);
        } else {
            const wanted = [
                1, 2, 3,
                current - 2, current - 1, current, current + 1, current + 2,
                total - 1, total
            ].filter(p => p >= 1 && p <= total);

            const uniqueSorted = Array.from(new Set(wanted)).sort((a, b) => a - b);

            let last = 0;
            for (const p of uniqueSorted) {
                if (last !== 0 && p !== last + 1) addDots();
                addPage(p);
                last = p;
            }
        }

        const next = new InfoPaginaDto();
        next.flg_boton_activo = true;
        next.nom_boton = 'Next';
        next.valor_boton = current + 1;
        if (next.valor_boton === total + 1) next.valor_boton = 0;
        buttons.push(next);

        // marcar actual
        for (const b of buttons) {
            if (b.valor_boton === current) b.flg_boton_actual = true;
        }

        // deshabilitar prev/next fuera de rango
        if (prev.valor_boton <= 0) prev.valor_boton = 0;

        return buttons;
    }

    static organizeElement<T>(elementList: T[], numElementRow: number) {
        let numCount: number = 0;
        let subElementList: T[] = [];
        let elementListHtml: T[][] = [];

        for (let index = 0; index < elementList.length; index++) {
            const element: T = elementList[index];

            if (numElementRow > numCount) {
                subElementList.push(element);
                numCount++;
            }
            else {
                elementListHtml.push(subElementList);
                numCount = 0;
                subElementList = [];

                subElementList.push(element);
                numCount++;
            }

        }

        if (subElementList.length > 0) {
            elementListHtml.push(subElementList);
        }

        return elementListHtml;
    }
}
