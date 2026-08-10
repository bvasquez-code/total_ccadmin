package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.PresaleChannelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PresaleChannelRepository extends JpaRepository<PresaleChannelEntity, String> {

    @Query(value = """
            select pc.*
            from presale_channel pc
            where pc.PresaleCod = :presaleCod
            limit 1
            """, nativeQuery = true)
    Optional<PresaleChannelEntity> findByPresaleCod(@Param("presaleCod") String presaleCod);

    @Query(value = """
            select pc.*
            from presale_channel pc
            inner join commercial_channel cc on cc.ChannelCod = pc.ChannelCod
            where pc.PresaleCod = :presaleCod
              and pc.Status = 'A'
              and cc.Status = 'A'
            limit 1
            """, nativeQuery = true)
    Optional<PresaleChannelEntity> findActiveByPresaleCod(@Param("presaleCod") String presaleCod);
}
