package com.ccadmin.app.transfer.repository;

import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.transfer.model.entity.CarrierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CarrierRepository extends JpaRepository<CarrierEntity, String>,
        CcAdminRepository<CarrierEntity, String> {

    @Override
    @Query(value = """
        select count(1)
        from carrier c
        where c.CarrierCod = :id
           or c.CarrierRuc = :query
           or c.CarrierName like %:query%
           or c.VehiclePlate like %:query%
           or c.DriverDocNumber = :query
           or c.DriverLicenseNumber = :query
        """, nativeQuery = true)
    int countByQueryText(@Param("id") String id, @Param("query") String query);

    @Override
    @Query(value = """
        select c.*
        from carrier c
        where c.CarrierCod = :id
           or c.CarrierRuc = :query
           or c.CarrierName like %:query%
           or c.VehiclePlate like %:query%
           or c.DriverDocNumber = :query
           or c.DriverLicenseNumber = :query
        order by c.CreationDate desc
        limit :init, :limit
        """, nativeQuery = true)
    List<CarrierEntity> findByQueryText(
            @Param("id") String id,
            @Param("query") String query,
            @Param("init") int init,
            @Param("limit") int limit
    );

    @Query(value = """
        select c.*
        from carrier c
        where c.Status = 'A'
        order by c.CarrierName asc
        """, nativeQuery = true)
    List<CarrierEntity> findActives();
}
