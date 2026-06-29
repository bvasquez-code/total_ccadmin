package com.ccadmin.app.product.repository;

import com.ccadmin.app.product.model.entity.ProductSearchEntity;
import com.ccadmin.app.product.model.entity.id.ProductSearchID;
import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductSearchRepository extends JpaRepository<ProductSearchEntity, ProductSearchID>, CcAdminRepository<ProductSearchEntity, ProductSearchID> {

    @Query(value = """
            select ps.* from product_search ps
            where (ps.ProductCod = :id or ps.ProductName like %:query%)
            and ps.StoreCod = :storeCod 
            and ps.Status = 'A'
            order by ps.NumTrend desc
            limit :init,:limit
            """, nativeQuery = true)
    @Override
    public List<ProductSearchEntity> findByQueryTextStore(
            @Param("id") String id,
            @Param("query") String query,
            @Param("storeCod") String storeCod,
            @Param("init") int init,
            @Param("limit") int limit
    );
    @Override
    @Query(value = """
            select count(1) from product_search ps
            where (ps.ProductCod = :id or ps.ProductName like %:query%)
            and ps.StoreCod = :storeCod 
            and ps.Status = 'A'
            """, nativeQuery = true)
    public int countByQueryTextStore(
            @Param("id") String id,
            @Param("query") String query,
            @Param("storeCod") String storeCod
    );

    @Query(value = """
            select
              p.ProductCod
             ,pi2.StoreCod
             ,p.ProductName
             ,p.ProductDesc
             ,pi2.NumDigitalStock
             ,pi2.NumPhysicalStock
             ,pc.NumPrice
             ,pc.NumMaxStock
             ,pc.NumMinStock
             ,pc.IsDiscontable
             ,pc.DiscountType
             ,pc.NumDiscountMax
             ,pc.ProductUnitName
             ,pc.ProductUnitFactor
             ,b.BrandCod
             ,b.BrandName
             ,c.CategoryCod
             ,c.CategoryName
             ,cp.CategoryCod as CategoryDadCod
             ,cp.CategoryName as CategoryDadName
             ,cu.CurrencyCod
             ,cu.CurrencySymbol
             ,pp.FileCod
             ,af.Route as FileRoute
             ,0 as NumTrend
             ,p.CreationUser
             ,p.CreationDate
             ,p.ModifyUser
             ,now() as ModifyDate
             ,p.Status
            from product p
            inner join product_info pi2 on pi2.ProductCod = p.ProductCod
            inner join product_config pc on pc.ProductCod = p.ProductCod and pc.StoreCod = pi2.StoreCod
            inner join brand b on b.BrandCod = p.BrandCod
            inner join category c on c.CategoryCod = p.CategoryCod
            inner join category cp on cp.CategoryCod = c.CategoryDadCod
            inner join ( select c2.* from currency c2 where c2.IsMonedaSystem = 'S'  ) cu
            left join product_picture pp on pp.ProductCod = p.ProductCod and pp.IsPrincipal = 'S'
            left join app_file af on af.FileCod = pp.FileCod
            where
            p.ProductCod = :ProductCod
            and  pi2.StoreCod = :StoreCod
            """, nativeQuery = true)
    public ProductSearchEntity findResumeProduct(
             @Param("ProductCod") String ProductCod
            ,@Param("StoreCod") String StoreCod
    );


    @Query(value = """
        select ps.* 
        from product_search ps
        where (ps.ProductCod = :id or ps.ProductName like %:query%) 
          and ps.StoreCod = :storeCod 
          and ps.Status = 'A'
          and ps.NumPhysicalStock >= :stockMin
        order by
            CASE WHEN :orderBy = 'trend' THEN ps.NumTrend END
                * IF(:direction = 'desc', -1, 1),
            CASE WHEN :orderBy = 'price' THEN ps.NumPrice END
                * IF(:direction = 'desc', -1, 1),
            CASE WHEN :orderBy = 'date'  THEN UNIX_TIMESTAMP(ps.CreationDate) END
                * IF(:direction = 'desc', -1, 1)
        limit :init, :limit
        """, nativeQuery = true)
    List<ProductSearchEntity> findByQueryTextStorePersonalized(
            @Param("id") String id,
            @Param("query") String query,
            @Param("storeCod") String storeCod,
            @Param("stockMin") int stockMin,
            @Param("orderBy") String orderBy,
            @Param("direction") String direction,
            @Param("init") int init,
            @Param("limit") int limit
    );

    @Query(value = """
        select count(1)
        from product_search ps
        where (ps.ProductCod = :id or ps.ProductName like %:query%) 
          and ps.StoreCod = :storeCod 
          and ps.Status = 'A'
          and ps.NumPhysicalStock >= :stockMin
        """, nativeQuery = true)
    public int countByQueryTextStorePersonalized(
            @Param("id") String id,
            @Param("query") String query,
            @Param("storeCod") String storeCod,
            @Param("stockMin") int stockMin
    );

}
