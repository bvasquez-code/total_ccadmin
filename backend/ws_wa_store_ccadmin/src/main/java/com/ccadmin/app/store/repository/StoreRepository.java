package com.ccadmin.app.store.repository;

import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.store.model.entity.StoreEntity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreRepository extends JpaRepository<StoreEntity,String>, CcAdminRepository<StoreEntity,String> {

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
            CALL db_store_01.sp_initalize_store_automation(:StoreCod, :Name, :Description)
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
