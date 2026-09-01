package com.ccadmin.app.store.repository;

import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.store.model.entity.StoreEntity;
import com.ccadmin.app.store.model.idto.IStoreVirtualCandidateDto;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreRepository extends JpaRepository<StoreEntity,String>, CcAdminRepository<StoreEntity,String> {

    @Query(value = """
            select s.*
            from store s
            where s.StoreCod = :storeCod
            limit 1
            """, nativeQuery = true)
    Optional<StoreEntity> findByStoreCod(@Param("storeCod") String storeCod);

    @Query(value = """
            select s.*
            from store s
            where s.Name = :name
            limit 1
            """, nativeQuery = true)
    Optional<StoreEntity> findByName(@Param("name") String name);

    @Query(value = """
            select
                s.StoreCod as StoreCod,
                s.Name as Name,
                s.Description as Description,
                s.Address as Address,
                s.UbigeoCod as UbigeoCod,
                s.Latitude as Latitude,
                s.Longitude as Longitude,
                svc.AllowsAutomaticDelivery as AllowsAutomaticDelivery,
                svc.AutomaticDeliveryRadiusKm as AutomaticDeliveryRadiusKm,
                svc.AllowsScheduledDelivery as AllowsScheduledDelivery,
                svc.ScheduledDeliveryMaxRadiusKm as ScheduledDeliveryMaxRadiusKm,
                svc.AllowsStorePickup as AllowsStorePickup,
                svc.PreparationTimeMinutes as PreparationTimeMinutes
            from store s
            inner join store_virtual_config svc on svc.StoreCod = s.StoreCod
            where s.Status = 'A'
              and s.IsVirtualStoreEnabled = 'S'
              and svc.Status = 'A'
              and s.Latitude is not null
              and s.Longitude is not null
            order by s.StoreCod
            """, nativeQuery = true)
    List<IStoreVirtualCandidateDto> findAllActiveVirtualCandidates();

    @Query(value = """
            select s.* from store s where s.Status = 'A' order by s.StoreCod
            """, nativeQuery = true)
    public List<StoreEntity> findAllActive();

    @Query(value = """
            select count(1)  from warehouse w where w.StoreCod = :StoreCod and Status = 'A'
            """,nativeQuery = true)
    public int countNumberWarehouse(@Param("StoreCod") String StoreCod);

    @Query(value = """
            select get_ubigeo_full_name(:UbigeoCod)
            """, nativeQuery = true)
    public String findUbigeo(@Param("UbigeoCod") String UbigeoCod);

    @Modifying
    @Query(value = """
            CALL sp_initalize_store_automation(:StoreCod, :Name, :Description)
            """, nativeQuery = true)
    public void initializeStoreAutomation(
            @Param("StoreCod") String StoreCod,
            @Param("Name") String Name,
            @Param("Description") String Description
    );

    @Override
    @Query(value = """
            select count(1) from store s 
            where s.StoreCod = :id 
            or (s.Name like %:query% or s.Description like %:query%)
            """, nativeQuery = true)
    int countByQueryText(
        @Param("id") String id, 
        @Param("query") String query
    );

    @Override
    @Query(value = """
            select s.* from store s 
            where s.StoreCod = :id 
            or (s.Name like %:query% or s.Description like %:query%)
            order by s.ModifyDate desc
            limit :init,:limit
            """, nativeQuery = true)
    List<StoreEntity> findByQueryText(
        @Param("id") String id, 
        @Param("query") String query, 
        @Param("init") int init, 
        @Param("limit") int limit
    );

    
}
