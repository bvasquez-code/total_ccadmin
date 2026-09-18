import { Injectable } from '@angular/core';
import * as XLSX from 'xlsx';
import { DataTablaGeneticDto } from '../../shared/model/dto/DataTablaGeneticDto';
import { ReportRowDto } from '../model/dto/ReportRowDto';
import { FinancialReportOverviewDto } from '../model/dto/FinancialReportOverviewDto';
import { ReportFilterDto } from '../model/dto/ReportFilterDto';

@Injectable({ providedIn: 'root' })
export class ReportExcelService {
    download(table: DataTablaGeneticDto<ReportRowDto>, filters: ReportFilterDto, description: string, overview?: FinancialReportOverviewDto): void {
        const columns = table.Headers.filter(column => !!column.key && !column.ColumnAction);
        const values = table.DataTable.data.resultSearch.map(row => columns.map(column => {
            const value = row[column.key!];
            return column.Mask && value != null ? column.Mask[String(value)] ?? value : value;
        }));
        const sheet = XLSX.utils.aoa_to_sheet([columns.map(column => column.Name), ...values]);
        sheet['!cols'] = columns.map(column => ({ wch: Math.min(40, Math.max(16, (column.Name || '').length + 2)) }));
        if (sheet['!ref']) sheet['!autofilter'] = { ref: sheet['!ref'] };
        const workbook = XLSX.utils.book_new();
        XLSX.utils.book_append_sheet(workbook, sheet, 'Reporte');
        XLSX.utils.book_append_sheet(workbook, XLSX.utils.aoa_to_sheet([
            ['Reporte', table.NameTable],
            ['Criterio', description],
            ['Desde', filters.DateFrom || 'Stock actual'],
            ['Hasta', filters.DateTo || 'Stock actual'],
            ['Local', filters.StoreCod || 'Todas las tiendas'],
            ['Moneda', filters.CurrencyCod || 'Todas (sin conversión)'],
            ['Búsqueda', filters.Query],
            ['Estado / tipo', filters.State || 'Todos'],
            ['Filas', table.DataTable.data.TotalResult]
        ]), 'Filtros');
        if (overview) {
            const includesCost = columns.some(column => column.key === 'Cost');
            const salesLabel = columns.find(column => column.key === 'Sales')?.Name || 'Ventas netas';
            const purchasesLabel = columns.find(column => column.key === 'Purchases')?.Name || 'Compras';
            const currencyLabel = (currency: string) => currency === 'UNKNOWN' ? 'Sin moneda' : currency;
            const summaryRows = includesCost ? [
                ['Moneda', salesLabel, purchasesLabel, 'Costo verificado', 'Resultado completo', 'Resultado verificado',
                    'Margen %', 'Filas', 'Filas con costo pendiente'],
                ...overview.Summary.map(item => [currencyLabel(item.CurrencyCod), item.Sales, item.Purchases, item.Cost,
                    item.Result, item.KnownResult, item.Margin, item.RowCount, item.MissingCostRows])
            ] : [
                ['Moneda', salesLabel, purchasesLabel, 'Saldo del período', 'Filas'],
                ...overview.Summary.map(item => [currencyLabel(item.CurrencyCod), item.Sales, item.Purchases,
                    item.Result, item.RowCount])
            ];
            const dailyRows = includesCost ? [
                ['Fecha', 'Moneda', salesLabel, purchasesLabel, 'Costo verificado', 'Resultado verificado',
                    'Filas con costo pendiente', 'Filas'],
                ...overview.Daily.map(item => [item.ReportDay, currencyLabel(item.CurrencyCod), item.Sales, item.Purchases,
                    item.Cost, item.KnownResult, item.MissingCostRows, item.RowCount])
            ] : [
                ['Fecha', 'Moneda', salesLabel, purchasesLabel, 'Saldo del día', 'Filas'],
                ...overview.Daily.map(item => [item.ReportDay, currencyLabel(item.CurrencyCod), item.Sales, item.Purchases,
                    item.KnownResult, item.RowCount])
            ];
            XLSX.utils.book_append_sheet(workbook, XLSX.utils.aoa_to_sheet(summaryRows), 'Resumen');
            XLSX.utils.book_append_sheet(workbook, XLSX.utils.aoa_to_sheet(dailyRows), 'Evolución diaria');
        }
        const filename = table.NameTable.normalize('NFD').replace(/[\u0300-\u036f]/g, '')
            .toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/-$/, '');
        XLSX.writeFile(workbook, `${filename}.xlsx`);
    }
}
