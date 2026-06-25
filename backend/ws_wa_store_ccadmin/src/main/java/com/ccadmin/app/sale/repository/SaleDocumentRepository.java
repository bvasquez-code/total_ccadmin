package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.SaleDocumentEntity;
import com.ccadmin.app.sale.model.entity.id.SaleDocumentID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SaleDocumentRepository extends JpaRepository<SaleDocumentEntity, SaleDocumentID> {

    @Query( value = """
            select sd.* from sale_document sd where sd.SaleCod = :SaleCod
            """, nativeQuery = true)
    public SaleDocumentEntity findBySaleCod(@Param("SaleCod") String SaleCod);


    @Query( value = """
            select sd.* from sale_document sd where sd.DocumentCod = :DocumentCod
            """, nativeQuery = true)
    public SaleDocumentEntity findByDocumentCod(@Param("DocumentCod") String DocumentCod);
}
