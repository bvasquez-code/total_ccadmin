package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.ChannelDeliveryTypeEntity;
import com.ccadmin.app.sale.model.entity.id.ChannelDeliveryTypeID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelDeliveryTypeRepository extends JpaRepository<ChannelDeliveryTypeEntity, ChannelDeliveryTypeID> {

    @Query(value = """
            select cdt.*
            from channel_delivery_type cdt
            inner join commercial_channel cc on cc.ChannelCod = cdt.ChannelCod
            inner join delivery_type dt on dt.DeliveryTypeCod = cdt.DeliveryTypeCod
            where cdt.ChannelCod = :channelCod
              and cdt.Status = 'A'
              and cc.Status = 'A'
              and dt.Status = 'A'
            order by cdt.IsDefault desc, dt.Name, cdt.DeliveryTypeCod
            """, nativeQuery = true)
    List<ChannelDeliveryTypeEntity> findActiveByChannelCod(@Param("channelCod") String channelCod);

    @Query(value = """
            select cdt.*
            from channel_delivery_type cdt
            inner join commercial_channel cc on cc.ChannelCod = cdt.ChannelCod
            inner join delivery_type dt on dt.DeliveryTypeCod = cdt.DeliveryTypeCod
            where cdt.ChannelCod = :channelCod
              and cdt.IsDefault = 'S'
              and cdt.Status = 'A'
              and cc.Status = 'A'
              and dt.Status = 'A'
            order by cdt.ModifyDate desc
            limit 1
            """, nativeQuery = true)
    Optional<ChannelDeliveryTypeEntity> findDefaultByChannelCod(@Param("channelCod") String channelCod);

    @Query(value = """
            select cdt.*
            from channel_delivery_type cdt
            inner join commercial_channel cc on cc.ChannelCod = cdt.ChannelCod
            inner join delivery_type dt on dt.DeliveryTypeCod = cdt.DeliveryTypeCod
            where cdt.ChannelCod = :channelCod
              and cdt.DeliveryTypeCod = :deliveryTypeCod
              and cdt.Status = 'A'
              and cc.Status = 'A'
              and dt.Status = 'A'
            limit 1
            """, nativeQuery = true)
    Optional<ChannelDeliveryTypeEntity> findActiveByChannelAndDeliveryType(
            @Param("channelCod") String channelCod,
            @Param("deliveryTypeCod") String deliveryTypeCod
    );
}
