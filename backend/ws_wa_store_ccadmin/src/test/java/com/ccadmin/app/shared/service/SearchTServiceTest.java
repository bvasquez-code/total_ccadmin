package com.ccadmin.app.shared.service;

import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.shared.model.dto.SearchDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SearchTServiceTest {
    @SuppressWarnings("unchecked")
    private final CcAdminRepository<String, String> repository =
            mock(CcAdminRepository.class, CALLS_REAL_METHODS);
    private final SearchTService<String> searchTService = new SearchTService<>(repository);

    @Test
    void preservesExistingTextSearchAndCalculatesOffsetWithSearchDto() {
        doReturn(List.of("P011")).when(repository).findByQueryText("producto", "producto", 10, 10);
        doReturn(11).when(repository).countByQueryText("producto", "producto");

        SearchDto search = new SearchDto("producto", 2);
        var response = searchTService.findAll(search, 10);

        assertEquals(10, search.Init);
        assertEquals(10, search.Limit);
        assertEquals(2, response.Page);
        assertEquals(2, response.TotalPages);
        assertEquals(11, response.TotalResult);
        assertEquals(List.of("P011"), response.resultSearch);
    }

    @Test
    void preservesExistingStoreSearch() {
        doReturn(List.of("P001")).when(repository).findByQueryTextStore("producto", "producto", "T001", 0, 10);
        doReturn(1).when(repository).countByQueryTextStore("producto", "producto", "T001");

        var response = searchTService.findAllStore(new SearchDto("producto", 1, "T001"), 10);

        assertEquals(1, response.TotalResult);
        assertEquals(List.of("P001"), response.resultSearch);
    }

    @Test
    void structuredSearchRetainsTheSpecializedDtoForBothQueries() {
        class FilteredSearch extends SearchDto {
            final String CurrencyCod = "PEN";
        }
        FilteredSearch search = new FilteredSearch();
        search.Page = 3;
        doReturn(List.of("S021")).when(repository).findByQueryText(search);
        doReturn(21).when(repository).countByQueryText(search);

        var response = searchTService.findAll(search, 10);

        verify(repository).findByQueryText(same(search));
        verify(repository).countByQueryText(same(search));
        assertEquals(20, search.Init);
        assertEquals(3, response.TotalPages);
        assertEquals(List.of("S021"), response.resultSearch);
    }
}
