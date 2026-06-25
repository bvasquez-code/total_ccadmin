package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.PresaleDetEntity;
import com.ccadmin.app.sale.model.entity.id.PresaleDetID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PresaleDetRepository extends JpaRepository<PresaleDetEntity, PresaleDetID> {

    @Modifying
    @Query( value = """
            update presale_det set Status = :Status where PresaleCod = :PresaleCod
            """,nativeQuery = true)
    public void updateStatusAll(
            @Param("PresaleCod") String PresaleCod
            ,@Param("Status") String Status
    );

    @Query( value = """
            select * from presale_det where PresaleCod = :PresaleCod and Status = 'A'
            """,nativeQuery = true)
    public List<PresaleDetEntity> findByPresaleCod(@Param("PresaleCod") String PresaleCod);
}
