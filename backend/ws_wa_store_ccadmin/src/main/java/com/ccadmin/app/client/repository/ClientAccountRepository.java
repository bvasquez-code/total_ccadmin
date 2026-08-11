package com.ccadmin.app.client.repository;

import com.ccadmin.app.client.model.entity.ClientAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientAccountRepository extends JpaRepository<ClientAccountEntity, Long> {

    @Query(value = """
            select ca.*
            from client_account ca
            inner join client c on c.ClientCod = ca.ClientCod
            inner join person p on p.PersonCod = c.PersonCod
            where lower(ca.Email) = lower(:email)
              and ca.Status = 'A'
              and c.Status = 'A'
              and p.Status = 'A'
            limit 1
            """, nativeQuery = true)
    Optional<ClientAccountEntity> findActiveByEmail(@Param("email") String email);

    @Query(value = """
            select ca.*
            from client_account ca
            inner join client c on c.ClientCod = ca.ClientCod
            inner join person p on p.PersonCod = c.PersonCod
            where ca.ClientAccountID = :clientAccountID
              and ca.Status = 'A'
              and c.Status = 'A'
              and p.Status = 'A'
            limit 1
            """, nativeQuery = true)
    Optional<ClientAccountEntity> findActiveByClientAccountID(
            @Param("clientAccountID") Long clientAccountID
    );

    @Query(value = """
            select trim(concat(p.Names, ' ', coalesce(p.LastNames, '')))
            from client_account ca
            inner join client c on c.ClientCod = ca.ClientCod
            inner join person p on p.PersonCod = c.PersonCod
            where ca.ClientAccountID = :clientAccountID
              and ca.Status = 'A'
              and c.Status = 'A'
              and p.Status = 'A'
            limit 1
            """, nativeQuery = true)
    String findClientNames(@Param("clientAccountID") Long clientAccountID);

    @Query(value = """
            select count(1)
            from client_account ca
            where lower(ca.Email) = lower(:email)
            """, nativeQuery = true)
    int countByEmail(@Param("email") String email);

    @Query(value = """
            select count(1)
            from client_account ca
            where ca.ClientCod = :clientCod
            """, nativeQuery = true)
    int countByClientCod(@Param("clientCod") String clientCod);
}
