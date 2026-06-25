package com.ccadmin.app.pucharse.repository;

import com.ccadmin.app.pucharse.model.entity.PucharseRequestDetEntity;
import com.ccadmin.app.pucharse.model.entity.id.PucharseRequestDetId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PucharseRequestDetRepository extends JpaRepository<PucharseRequestDetEntity, PucharseRequestDetId> {

    @Modifying
    @Query( value = """
            update pucharse_request_det set Status = :Status where PucharseReqCod = :PucharseReqCod
            """,nativeQuery = true)
    public void updateStatusAll(
             @Param("PucharseReqCod") String PucharseReqCod
            ,@Param("Status") String Status
    );

    @Query( value = """
            select * from pucharse_request_det where PucharseReqCod = :PucharseReqCod and Status = 'A'
            """,nativeQuery = true)
    public List<PucharseRequestDetEntity> findAllActive(@Param("PucharseReqCod") String PucharseReqCod);
}
