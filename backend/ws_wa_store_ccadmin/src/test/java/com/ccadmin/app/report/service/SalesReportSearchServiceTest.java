package com.ccadmin.app.report.service;

import com.ccadmin.app.report.model.dto.ReportFilterDto;
import com.ccadmin.app.report.model.dto.ReportSearchDto;
import com.ccadmin.app.report.model.idto.ISalesReportDto;
import com.ccadmin.app.report.repository.SalesReportRepository;
import com.ccadmin.app.shared.model.dto.SearchDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SalesReportSearchServiceTest {
    private final SalesReportRepository salesReportRepository = mock(SalesReportRepository.class);
    private final SalesReportSearchService salesReportSearchService = new SalesReportSearchService(salesReportRepository);

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("U001", null));
    }

    @AfterEach
    void clearSession() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void delegatesPaginationAndPreservesTypedAmountsAndAllFilters() {
        when(salesReportRepository.countByQueryText(any(SearchDto.class))).thenReturn(11);
        ISalesReportDto row = mock(ISalesReportDto.class);
        when(row.getSaleCod()).thenReturn("S011");
        when(row.getAmount()).thenReturn(new BigDecimal("123.45"));
        when(salesReportRepository.findByQueryText(any(SearchDto.class))).thenAnswer(invocation -> {
            ReportSearchDto search = invocation.getArgument(0);
            assertEquals(10, search.Init);
            assertEquals(10, search.Limit);
            assertFilters(search);
            return List.of(row);
        });

        var response = salesReportSearchService.findAll(filters(2));

        assertEquals(2, response.Page);
        assertEquals(2, response.TotalPages);
        assertEquals(11, response.TotalResult);
        assertEquals(new BigDecimal("123.45"), response.resultSearch.getFirst().Amount);
    }

    @Test
    void emptyResultsUseTheSharedPaginatorContract() {
        when(salesReportRepository.findByQueryText(any(SearchDto.class))).thenReturn(List.of());
        var response = salesReportSearchService.findAll(filters(1));
        assertEquals(0, response.TotalPages);
        assertEquals(0, response.TotalResult);
        assertTrue(response.resultSearch.isEmpty());
    }

    @Test
    void exportRejectsOversizedResultsBeforeLoadingRows() {
        when(salesReportRepository.countByQueryText(any(SearchDto.class))).thenReturn(10001);
        assertThrows(IllegalArgumentException.class, () -> salesReportSearchService.export(filters(1)));
        verify(salesReportRepository, never()).findByQueryText(any(SearchDto.class));
    }

    @Test
    void exportUsesSameFiltersAndStartsAtFirstPage() {
        when(salesReportRepository.countByQueryText(any(SearchDto.class))).thenReturn(1);
        when(salesReportRepository.findByQueryText(any(SearchDto.class))).thenAnswer(invocation -> {
            ReportSearchDto search = invocation.getArgument(0);
            assertEquals(0, search.Init);
            assertEquals(10000, search.Limit);
            assertFilters(search);
            return List.of(mock(ISalesReportDto.class));
        });
        var response = salesReportSearchService.export(filters(8));
        assertEquals(1, response.Page);
        assertEquals(1, response.TotalResult);
    }

    private ReportFilterDto filters(int page) {
        return new ReportFilterDto("2026-09-01", "2026-09-30", "T001", "PEN", "venta", "C", page);
    }

    private void assertFilters(ReportSearchDto search) {
        assertEquals(Date.valueOf("2026-09-01"), search.DateFrom);
        assertEquals(Date.valueOf("2026-10-01"), search.DateTo);
        assertEquals("T001", search.StoreCod);
        assertEquals("PEN", search.CurrencyCod);
        assertEquals("%venta%", search.Query);
        assertEquals("C", search.State);
        assertEquals("U001", search.UserCod);
    }
}
