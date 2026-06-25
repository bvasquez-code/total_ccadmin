package com.ccadmin.app.system.repository;

import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.system.model.entity.PaymentMethodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethodEntity,String>,
        CcAdminRepository<PaymentMethodEntity, String> {

    @Query( value = """
            select pm.* from payment_method pm where pm.Status = 'A'
            """, nativeQuery = true)
    public List<PaymentMethodEntity> findAllActive();

    @Override
    @Query(value = """
            select count(1)
            from payment_method pm
            where pm.PaymentMethodCod = :id
               or pm.Name like %:query%
               or pm.Description like %:query%
               or pm.PaymentMethodType like %:query%
            """, nativeQuery = true)
    int countByQueryText(@Param("id") String id, @Param("query") String query);

    @Override
    @Query(value = """
            select pm.*
            from payment_method pm
            where pm.PaymentMethodCod = :id
               or pm.Name like %:query%
               or pm.Description like %:query%
               or pm.PaymentMethodType like %:query%
            order by pm.CreationDate desc
            limit :init, :limit
            """, nativeQuery = true)
    List<PaymentMethodEntity> findByQueryText(
            @Param("id") String id,
            @Param("query") String query,
            @Param("init") int init,
            @Param("limit") int limit
    );
}
