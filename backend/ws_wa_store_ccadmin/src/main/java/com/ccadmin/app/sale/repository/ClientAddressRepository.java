package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.ClientAddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientAddressRepository extends JpaRepository<ClientAddressEntity, Long> {

    @Query(value = """
            select ca.*
            from client_address ca
            where ca.ClientCod = :clientCod
              and ca.Status = 'A'
            order by ca.IsDefault desc, ca.ModifyDate desc, ca.ClientAddressID desc
            """, nativeQuery = true)
    List<ClientAddressEntity> findActiveByClientCod(@Param("clientCod") String clientCod);

    @Query(value = """
            select ca.*
            from client_address ca
            where ca.ClientAddressID = :clientAddressId
              and ca.ClientCod = :clientCod
              and ca.Status = 'A'
            limit 1
            """, nativeQuery = true)
    Optional<ClientAddressEntity> findActiveByClientAddressIdAndClientCod(
            @Param("clientAddressId") Long clientAddressId,
            @Param("clientCod") String clientCod
    );
}
