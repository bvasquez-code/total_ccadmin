package com.ccadmin.app.product.service;

import com.ccadmin.app.product.model.dto.KardexZoneDto;
import com.ccadmin.app.product.model.dto.KardexZoneSearchDto;
import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.model.entity.ProductEntity;
import com.ccadmin.app.product.repository.KardexZoneRepository;
import com.ccadmin.app.product.shared.ProductShared;
import com.ccadmin.app.shared.model.dto.ResponsePageSearch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KardexZoneSearchServiceTest {

    @Mock
    private KardexZoneRepository kardexZoneRepository;
    @Mock
    private ProductShared productShared;
    @InjectMocks
    private KardexZoneSearchService searchService;

    @Test
    void shouldSearchMovementsWithZoneAndOperationFilters() {
        KardexZoneSearchDto search = new KardexZoneSearchDto();
        search.Query = "  SL001  ";
        search.Page = 2;
        search.StoreCod = " S001 ";
        search.ZoneStockMoved = " PHYSICAL ";
        search.TypeOperation = " R ";
        KardexZoneEntity movement = movement();
        ProductEntity product = new ProductEntity();
        product.ProductCod = "P001";
        product.ProductName = "Producto de prueba";

        when(this.kardexZoneRepository.countSearch("SL001", "S001", "PHYSICAL", "R"))
                .thenReturn(11);
        when(this.kardexZoneRepository.search("SL001", "S001", "PHYSICAL", "R", 10, 10))
                .thenReturn(List.of(movement));
        when(this.productShared.findAllById(List.of("P001"))).thenReturn(List.of(product));

        ResponsePageSearch response = this.searchService.findAll(search);

        assertThat(response.TotalResult).isEqualTo(11);
        assertThat(response.TotalPages).isEqualTo(2);
        assertThat(response.Page).isEqualTo(2);
        List<KardexZoneDto> result = (List<KardexZoneDto>) response.resultSearch;
        assertThat(result).hasSize(1);
        assertThat(result.get(0).kardexZone).isSameAs(movement);
        assertThat(result.get(0).product).isSameAs(product);
        verify(this.kardexZoneRepository).search("SL001", "S001", "PHYSICAL", "R", 10, 10);
    }

    private KardexZoneEntity movement() {
        KardexZoneEntity movement = new KardexZoneEntity();
        movement.KardexZoneID = 1L;
        movement.ProductCod = "P001";
        movement.Variant = "0000";
        movement.StoreCod = "S001";
        movement.WarehouseCod = "W001";
        movement.ZoneStockMoved = "PHYSICAL";
        movement.TypeOperation = "R";
        movement.NumStockMoved = 2;
        return movement;
    }
}
