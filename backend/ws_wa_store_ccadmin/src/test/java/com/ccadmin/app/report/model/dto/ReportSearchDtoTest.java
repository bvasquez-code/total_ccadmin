package com.ccadmin.app.report.model.dto;

import org.junit.jupiter.api.Test;
import java.sql.Date;
import static org.junit.jupiter.api.Assertions.*;

class ReportSearchDtoTest {
    @Test
    void includesTheEntireFinalDayAndEscapesLiteralWildcards() {
        var filters = filters();
        filters.Query = "  50%_!  ";
        var search = ReportSearchDto.forPeriod(filters, "U001");
        assertEquals(Date.valueOf("2026-09-01"), search.DateFrom);
        assertEquals(Date.valueOf("2026-10-01"), search.DateTo);
        assertEquals("%50!%!_!!%", search.Query);
        assertEquals("U001", search.UserCod);
    }

    @Test
    void stockDoesNotRequireADateRange() {
        var search = ReportSearchDto.forStock(new ReportFilterDto(), "U001");
        assertNull(search.DateFrom);
        assertNull(search.DateTo);
        assertEquals("", search.StoreCod);
        assertEquals("%", search.Query.substring(0, 1));
    }

    @Test
    void periodAcceptsOneStoreOrAllStores() {
        var filters = filters();
        assertEquals("", ReportSearchDto.forPeriod(filters, "U001").StoreCod);
        filters.StoreCod = "  T001  ";
        assertEquals("T001", ReportSearchDto.forPeriod(filters, "U001").StoreCod);
        filters.StoreCod = " ";
        assertEquals("", ReportSearchDto.forPeriod(filters, "U001").StoreCod);
    }

    @Test
    void rejectsInvalidDatesAndReversedPeriods() {
        var filters = filters();
        filters.DateTo = "2026-02-30";
        assertThrows(IllegalArgumentException.class, () -> ReportSearchDto.forPeriod(filters, "U001"));
        filters.DateTo = "2026-08-31";
        assertThrows(IllegalArgumentException.class, () -> ReportSearchDto.forPeriod(filters, "U001"));
    }

    @Test
    void rejectsExcessiveDateRange() {
        var filters = filters();
        filters.DateTo = "2027-10-01";
        assertThrows(IllegalArgumentException.class, () -> ReportSearchDto.forPeriod(filters, "U001"));
    }

    @Test
    void rejectsInvalidPageAndOversizedQuery() {
        var filters = filters();
        filters.Page = 0;
        assertThrows(IllegalArgumentException.class, () -> ReportSearchDto.forPeriod(filters, "U001"));
        filters.Page = 1;
        filters.Query = "x".repeat(129);
        assertThrows(IllegalArgumentException.class, () -> ReportSearchDto.forPeriod(filters, "U001"));
    }

    private ReportFilterDto filters() {
        return new ReportFilterDto("2026-09-01", "2026-09-30", "", "", "", "", 1);
    }
}
