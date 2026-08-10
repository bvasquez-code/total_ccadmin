package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.CommercialChannelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommercialChannelRepository extends JpaRepository<CommercialChannelEntity, String> {

    @Query(value = """
            select cc.*
            from commercial_channel cc
            where cc.Status = 'A'
            order by cc.Name, cc.ChannelCod
            """, nativeQuery = true)
    List<CommercialChannelEntity> findAllActive();

    @Query(value = """
            select cc.*
            from commercial_channel cc
            where cc.ChannelCod = :channelCod
              and cc.Status = 'A'
            limit 1
            """, nativeQuery = true)
    Optional<CommercialChannelEntity> findActiveByChannelCod(@Param("channelCod") String channelCod);
}
