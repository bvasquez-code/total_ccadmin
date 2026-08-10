package com.ccadmin.app.store.service;

import com.ccadmin.app.sale.model.entity.StoreVirtualConfigEntity;
import com.ccadmin.app.sale.repository.StoreVirtualConfigRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.store.model.dto.StoreInfoDto;
import com.ccadmin.app.store.model.dto.StoreVirtualConfigRegisterDto;
import com.ccadmin.app.store.model.entity.StoreEntity;
import com.ccadmin.app.store.repository.CompanyRepository;
import com.ccadmin.app.store.repository.StoreRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class StoreService extends SessionService {

    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private StoreVirtualConfigRepository storeVirtualConfigRepository;

    private SearchTService<StoreEntity> searchService;

    public StoreEntity findById(String StoreCod)
    {
        return this.storeRepository.findById(StoreCod).get();
    }

    public List<StoreEntity> findAll()
    {
        return this.storeRepository.findAll();
    }

    public String findUbigeo(String UbigeoCod){
        return this.storeRepository.findUbigeo(UbigeoCod);
    }

    public StoreInfoDto findStoreInfo(String StoreCod){
        StoreInfoDto storeInfo = new StoreInfoDto();
        storeInfo.Company = this.companyRepository.findMyCompany();
        storeInfo.CompanyUbigeo = this.storeRepository.findUbigeo(storeInfo.Company.UbigeoCod);
        storeInfo.Store = this.storeRepository.findById(StoreCod).get();
        storeInfo.StoreUbigeo = this.storeRepository.findUbigeo(storeInfo.Store.UbigeoCod);
        return storeInfo;
    }

    public StoreVirtualConfigEntity findVirtualConfig(String StoreCod) {
        this.storeRepository.findByStoreCod(StoreCod)
                .orElseThrow(() -> new IllegalArgumentException("La tienda indicada no existe"));

        return this.storeVirtualConfigRepository.findByStoreCod(StoreCod)
                .orElseGet(() -> {
                    StoreVirtualConfigEntity config = new StoreVirtualConfigEntity();
                    config.StoreCod = StoreCod;
                    return config;
                });
    }


    public void initializeStoreAutomation(StoreEntity store){
        log.info("INI_AUTOCOMPLETADO_TIENDA -->>  {}", store.StoreCod);
        this.storeRepository.initializeStoreAutomation(store.StoreCod, store.Name, store.Description);
        log.info("FIN_AUTOCOMPLETADO_TIENDA -->>  {}", store.StoreCod);
    }

    @Transactional
    public StoreEntity save(StoreEntity store)
    {

        boolean exists = this.storeRepository.existsById(store.StoreCod);

        store.SunatAddressTypeCode = normalizeSunatAddressTypeCode(store.SunatAddressTypeCode);
        store.addSession(getUserCod());        
        StoreEntity savedStore = this.storeRepository.save(store);

        if(!exists){
             this.initializeStoreAutomation(savedStore);
        }
       
        return savedStore;
    }

    @Transactional
    public StoreVirtualConfigEntity saveVirtualConfig(StoreVirtualConfigRegisterDto register) {
        if (register == null || register.Store == null
                || register.Store.StoreCod == null || register.Store.StoreCod.isBlank()) {
            throw new IllegalArgumentException("Debe indicar la tienda que se configurara");
        }
        StoreEntity store = this.storeRepository.findByStoreCod(register.Store.StoreCod)
                .orElseThrow(() -> new IllegalArgumentException("La tienda indicada no existe"));
        updateVirtualStoreLocation(store, register.Store);

        if ("S".equals(store.IsVirtualStoreEnabled)) {
            if (register.Config == null) {
                throw new IllegalArgumentException("Debe ingresar la configuracion de delivery de la tienda");
            }
            validateVirtualConfig(register.Config);
        }

        this.storeRepository.save(store);

        if ("N".equals(store.IsVirtualStoreEnabled)) {
            return this.storeVirtualConfigRepository.findByStoreCod(store.StoreCod)
                    .orElseGet(() -> {
                        StoreVirtualConfigEntity config = new StoreVirtualConfigEntity();
                        config.StoreCod = store.StoreCod;
                        return config;
                    });
        }
        StoreVirtualConfigEntity config = this.storeVirtualConfigRepository
                .findByStoreCod(store.StoreCod)
                .orElseGet(StoreVirtualConfigEntity::new);
        config.StoreCod = store.StoreCod;
        config.AllowsAutomaticDelivery = register.Config.AllowsAutomaticDelivery;
        config.AutomaticDeliveryRadiusKm = register.Config.AutomaticDeliveryRadiusKm;
        config.AllowsScheduledDelivery = register.Config.AllowsScheduledDelivery;
        config.ScheduledDeliveryMaxRadiusKm = register.Config.ScheduledDeliveryMaxRadiusKm;
        config.AllowsStorePickup = register.Config.AllowsStorePickup;
        config.PreparationTimeMinutes = register.Config.PreparationTimeMinutes;
        config.Status = "A";
        config.addSession(getUserCod());

        return this.storeVirtualConfigRepository.save(config);
    }

    public ResponsePageSearchT<StoreEntity> findAll(String Query,int Page)
    {
        SearchDto search = new SearchDto(Query,Page);
        this.searchService = new SearchTService<StoreEntity>(this.storeRepository);
        return this.searchService.findAll(search,10);
    }

    private String normalizeSunatAddressTypeCode(String value) {
        if (value == null || value.isBlank()) {
            return "0000";
        }
        String code = value.trim();
        if (!code.matches("^\\d{4}$")) {
            throw new IllegalArgumentException("SunatAddressTypeCode debe tener 4 digitos");
        }
        return code;
    }

    private void validateVirtualConfig(StoreVirtualConfigEntity config) {
        validateIndicator(config.AllowsAutomaticDelivery, "delivery automatico");
        validateIndicator(config.AllowsScheduledDelivery, "entrega programada");
        validateIndicator(config.AllowsStorePickup, "recojo en tienda");

        if ("S".equals(config.AllowsAutomaticDelivery)
                && !isPositive(config.AutomaticDeliveryRadiusKm)) {
            throw new IllegalArgumentException("Debe ingresar un radio mayor que cero para el delivery automatico");
        }
        if ("S".equals(config.AllowsScheduledDelivery)
                && !isPositive(config.ScheduledDeliveryMaxRadiusKm)) {
            throw new IllegalArgumentException("Debe ingresar un alcance mayor que cero para la entrega programada");
        }
        if (config.AutomaticDeliveryRadiusKm != null
                && config.ScheduledDeliveryMaxRadiusKm != null
                && config.ScheduledDeliveryMaxRadiusKm.compareTo(config.AutomaticDeliveryRadiusKm) < 0) {
            throw new IllegalArgumentException("El alcance programado no puede ser menor al radio automatico");
        }
        if (config.PreparationTimeMinutes < 0) {
            throw new IllegalArgumentException("El tiempo de preparacion no puede ser negativo");
        }
    }

    private void updateVirtualStoreLocation(StoreEntity store, StoreEntity requestedStore) {
        validateIndicator(requestedStore.IsVirtualStoreEnabled, "uso en tienda virtual");

        store.IsVirtualStoreEnabled = requestedStore.IsVirtualStoreEnabled;
        store.Address = requestedStore.Address;
        store.UbigeoCod = requestedStore.UbigeoCod;
        store.Latitude = requestedStore.Latitude;
        store.Longitude = requestedStore.Longitude;

        if (store.Address != null && store.Address.length() > 128) {
            throw new IllegalArgumentException("La direccion no puede superar los 128 caracteres");
        }
        if (store.UbigeoCod != null && store.UbigeoCod.length() > 12) {
            throw new IllegalArgumentException("El ubigeo no puede superar los 12 caracteres");
        }
        if (store.Latitude != null
                && (store.Latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || store.Latitude.compareTo(BigDecimal.valueOf(90)) > 0)) {
            throw new IllegalArgumentException("La latitud debe encontrarse entre -90 y 90");
        }
        if (store.Longitude != null
                && (store.Longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || store.Longitude.compareTo(BigDecimal.valueOf(180)) > 0)) {
            throw new IllegalArgumentException("La longitud debe encontrarse entre -180 y 180");
        }
        if ("S".equals(store.IsVirtualStoreEnabled)) {
            if (store.Address == null || store.Address.isBlank()) {
                throw new IllegalArgumentException("Debe ingresar la direccion de la tienda virtual");
            }
            if (store.UbigeoCod == null || store.UbigeoCod.isBlank()) {
                throw new IllegalArgumentException("Debe ingresar el ubigeo de la tienda virtual");
            }
            if (store.Latitude == null || store.Longitude == null) {
                throw new IllegalArgumentException("Debe ingresar la latitud y longitud de la tienda virtual");
            }
        }
        store.addSession(getUserCod());
    }

    private void validateIndicator(String value, String fieldName) {
        if (!"S".equals(value) && !"N".equals(value)) {
            throw new IllegalArgumentException("El indicador de " + fieldName + " debe ser S o N");
        }
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
}
