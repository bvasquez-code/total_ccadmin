package com.ccadmin.app.pucharse.repository;

import com.ccadmin.app.pucharse.model.entity.PucharseDetEntity;
import com.ccadmin.app.pucharse.model.entity.id.PucharseDetId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PucharseDetRepository extends JpaRepository<PucharseDetEntity, PucharseDetId> {

    @Query( value = """
            select * from pucharse_det where PucharseCod = :PucharseCod and Status = 'A'
            """,nativeQuery = true)
    public List<PucharseDetEntity> findAllActive(@Param("PucharseCod") String PucharseCod);
}
