package com.ccadmin.app.system.repository;


import com.ccadmin.app.system.model.entity.TableSequenceEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;


public interface TableSequenceRepository extends JpaRepository<TableSequenceEntity, Long> {


    @Query(value = "call db_store_01.get_cod_seq(:type)", nativeQuery = true)
    String getNextCode(@Param("type") String sequenceTableType);
}
