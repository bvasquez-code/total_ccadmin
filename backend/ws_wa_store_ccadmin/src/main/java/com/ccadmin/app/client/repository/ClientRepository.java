package com.ccadmin.app.client.repository;

import com.ccadmin.app.client.model.entity.ClientEntity;
import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClientRepository extends JpaRepository<ClientEntity,String>, CcAdminRepository<ClientEntity,String> {

    @Query(value = """
            select * from client c where c.PersonCod = :PersonCod and c.Status = 'A'
            """,nativeQuery = true)
    public ClientEntity findByPersonCod(@Param("PersonCod") String PersonCod);

    @Override
    @Query(value = """
            SELECT COUNT(1) FROM client c
            INNER JOIN person p ON p.PersonCod = c.PersonCod
            where
            c.PersonCod = :id or CONCAT(p.Names,' ',p.LastNames) like %:query%
            and p.Status = 'A'
            and c.Status = 'A'
            """,nativeQuery = true)
    public int countByQueryText(
              @Param("id") String id
            , @Param("query") String query
    );

    @Override
    @Query(value = """
            SELECT c.* FROM client c
            INNER JOIN person p ON p.PersonCod = c.PersonCod
            where
            c.PersonCod = :id or CONCAT(p.Names,' ',p.LastNames) like %:query%
            and p.Status = 'A'
            and c.Status = 'A'
            order by c.ModifyDate desc
            limit :init,:limit
            """,nativeQuery = true)
    public List<ClientEntity> findByQueryText(
              @Param("id") String id
            , @Param("query") String query
            , @Param("init") int init
            , @Param("limit") int limit
    );
}
