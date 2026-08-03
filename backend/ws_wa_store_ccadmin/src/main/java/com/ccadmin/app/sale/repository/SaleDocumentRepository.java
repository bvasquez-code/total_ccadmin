package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.SaleDocumentEntity;
import com.ccadmin.app.sale.model.entity.id.SaleDocumentID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleDocumentRepository extends JpaRepository<SaleDocumentEntity, SaleDocumentID> {

    @Query( value = """
            select sd.*
            from sale_document sd
            where sd.SaleCod = :SaleCod
              and sd.Status = 'A'
            order by
              case sd.DocumentRole when 'F' then 1 when 'I' then 2 else 3 end,
              sd.IssueDate desc,
              sd.CreationDate desc
            """, nativeQuery = true)
    List<SaleDocumentEntity> findBySaleCod(@Param("SaleCod") String SaleCod);

    @Query( value = """
            select sd.*
            from sale_document sd
            where sd.SaleCod = :SaleCod
              and sd.DocumentRole = 'F'
              and sd.Status = 'A'
            order by sd.IssueDate desc, sd.CreationDate desc
            limit 1
            """, nativeQuery = true)
    SaleDocumentEntity findFiscalBySaleCod(@Param("SaleCod") String SaleCod);

    @Query( value = """
            select sd.*
            from sale_document sd
            where sd.SaleCod = :SaleCod
              and sd.DocumentRole = 'I'
              and sd.DocumentType = '99'
              and sd.Status = 'A'
            order by sd.IssueDate desc, sd.CreationDate desc
            limit 1
            """, nativeQuery = true)
    SaleDocumentEntity findProformaBySaleCod(@Param("SaleCod") String SaleCod);

    @Query(value = """
            select count(1)
            from sale_document sd
            where sd.SaleCod = :SaleCod
              and sd.DocumentRole = :DocumentRole
              and sd.Status = :Status
            """, nativeQuery = true)
    long countBySaleCodAndDocumentRoleAndStatus(
            @Param("SaleCod") String saleCod,
            @Param("DocumentRole") String documentRole,
            @Param("Status") String status
    );


    @Query( value = """
            select sd.* from sale_document sd where sd.DocumentCod = :DocumentCod
            """, nativeQuery = true)
    public SaleDocumentEntity findByDocumentCod(@Param("DocumentCod") String DocumentCod);

    @Query( value = """
            select sd.*
            from sale_document sd
            where sd.DocumentCod = :DocumentCod
              and sd.SaleCod = :SaleCod
              and sd.Status = 'A'
            limit 1
            """, nativeQuery = true)
    SaleDocumentEntity findByDocumentCodAndSaleCod(
            @Param("DocumentCod") String DocumentCod,
            @Param("SaleCod") String SaleCod
    );
}
