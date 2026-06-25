package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.CreditNoteHeadEntity;
import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditNoteHeadRepository extends JpaRepository<CreditNoteHeadEntity,String>, CcAdminRepository<CreditNoteHeadEntity,String> {

    /*@Query(value = """
            SELECT
            """, nativeQuery = true)
    public CreditNoteHeadEntity findBySaleCod(@Param("SaleCod") String SaleCod);*/

    @Query(value = """
            CALL db_store_01.get_cod_trx(:storeCod, 'credit_note_head')
            """,nativeQuery = true)
    public String getCreditNoteCod(@Param("storeCod") String storeCod);

    @Override
    @Query(value = """
            select count(1) from credit_note_head cnh
            left join client c on c.ClientCod = cnh.ClientCod
            left join person p on p.PersonCod = c.PersonCod
            where
            cnh.CreditNoteCod = :id or ( ('' != :query and concat(cnh.SaleCod,p.DocumentNum,p.Names,p.LastNames) like concat('%',:query,'%') ) or ( '' = :query ) )
            and cnh.StoreCod = :storeCod
            """, nativeQuery = true)
    public int countByQueryTextStore(String id, String query, String storeCod);

    @Override
    @Query(value = """
            select cnh.* from credit_note_head cnh
            left join client c on c.ClientCod = cnh.ClientCod
            left join person p on p.PersonCod = c.PersonCod
            where
            cnh.CreditNoteCod = :id or ( ('' != :query and concat(cnh.SaleCod,p.DocumentNum,p.Names,p.LastNames) like concat('%',:query,'%') ) or ( '' = :query ) )
            and cnh.StoreCod = :storeCod
            order by cnh.CreditNoteCod desc
            limit :init,:limit
            """,nativeQuery = true)
    public List<CreditNoteHeadEntity> findByQueryTextStore(String id, String query, String storeCod, int init, int limit);

    @Query(value = """
            select c.* from credit_note_head c where c.SaleCod = :SaleCod and c.Status = 'A'
            """, nativeQuery = true)
    CreditNoteHeadEntity findBySaleCod(@Param("SaleCod") String SaleCod);

}
