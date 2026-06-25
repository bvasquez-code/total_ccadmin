package com.ccadmin.app.product.repository;

import com.ccadmin.app.product.model.entity.ProductPictureEntity;
import com.ccadmin.app.product.model.entity.id.ProductPictureID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductPictureRepository extends JpaRepository<ProductPictureEntity, ProductPictureID> {

    @Query( value = """
            select * from product_picture pp
            where pp.ProductCod = :ProductCod
            order by pp.IsPrincipal desc
            limit 1
            """, nativeQuery = true)
    public ProductPictureEntity findPrincipal(@Param("ProductCod") String ProductCod);

    @Modifying
    @Query( value = """
            update product_picture set Status = :Status where ProductCod = :ProductCod
            """, nativeQuery = true)
    public void updateAllStatus(
            @Param("ProductCod") String ProductCod,
            @Param("Status") String Status
    );

    @Query( value = """
            select pp.* from product_picture pp where pp.ProductCod = :ProductCod and pp.Status = 'A' order by pp.CreationDate desc
            """, nativeQuery = true)
    public List<ProductPictureEntity> findAllByProductCod(@Param("ProductCod") String ProductCod);
}
