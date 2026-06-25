package com.ccadmin.app.promotion.repository;

import com.ccadmin.app.promotion.model.entity.PromotionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionRepository extends JpaRepository<PromotionEntity,String> {

    @Query(value = """
            CALL db_store_01.get_cod_seq(:SequenceTableType)
            """,nativeQuery = true)
    public String getCodSeq(@Param("SequenceTableType") String SequenceTableType);
}
