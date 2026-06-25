package com.ccadmin.app.product.model.entity;

import com.ccadmin.app.product.model.entity.id.ProductSearchID;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import com.ccadmin.app.system.model.entity.AppFileEntity;
import com.ccadmin.app.system.model.entity.CurrencyEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "product_search")
@IdClass(ProductSearchID.class)
public class ProductSearchEntity extends AuditTableEntity implements Serializable {

    @Id
    public String ProductCod;
    @Id
    public String StoreCod;
    public String ProductName;
    public String ProductDesc;
    public int NumDigitalStock;
    public int NumPhysicalStock;
    public BigDecimal NumPrice;
    public int NumMaxStock;
    public int NumMinStock;
    public String IsDiscontable;
    public String DiscountType;
    public BigDecimal NumDiscountMax;
    public String BrandCod;
    public String BrandName;
    public String CategoryCod;
    public String CategoryName;
    public String CategoryDadCod;
    public String CategoryDadName;
    public String CurrencyCod;
    public String CurrencySymbol;
    public String FileCod;
    public String FileRoute;
    public int NumTrend;

    public ProductSearchEntity() {

    }

    public ProductSearchEntity(
            ProductEntity product, ProductConfigEntity productConfig, List<ProductInfoEntity> productInfoList,
            CategoryEntity category, CategoryEntity categoryDad, BrandEntity brand, CurrencyEntity currencySys,
            ProductPictureEntity productPicture, AppFileEntity appFile, ProductRankingEntity productRanking,
            String StoreCod) {
        this.ProductCod = product.ProductCod;
        this.StoreCod = StoreCod;
        this.ProductName = product.ProductName;
        this.ProductDesc = product.ProductDesc;
        this.NumDigitalStock = productInfoList.stream().mapToInt(e -> e.NumDigitalStock).sum();
        this.NumPhysicalStock = productInfoList.stream().mapToInt(e -> e.NumPhysicalStock).sum();
        this.NumPrice = productConfig.NumPrice;
        this.NumMaxStock = productConfig.NumMaxStock;
        this.NumMinStock = productConfig.NumMinStock;
        this.IsDiscontable = productConfig.IsDiscontable;
        this.DiscountType = productConfig.DiscountType;
        this.NumDiscountMax = productConfig.NumDiscountMax;
        this.BrandCod = brand.BrandCod;
        this.BrandName = brand.BrandName;
        this.CategoryCod = category.CategoryCod;
        this.CategoryName = category.CategoryName;
        this.CategoryDadCod = categoryDad.CategoryCod;
        this.CategoryDadName = categoryDad.CategoryName;
        this.CurrencyCod = currencySys.CurrencyCod;
        this.CurrencySymbol = currencySys.CurrencySymbol;
        this.NumTrend = productRanking.RankingPoints;
        if (appFile != null) {
            this.FileCod = appFile.FileCod;
            this.FileRoute = appFile.Route;
        }

    }

    @Override
    public String toString() {
        return "ProductSearchEntity{" +
                "ProductCod='" + ProductCod + '\'' +
                ", StoreCod='" + StoreCod + '\'' +
                ", ProductName='" + ProductName + '\'' +
                ", ProductDesc='" + ProductDesc + '\'' +
                ", NumDigitalStock=" + NumDigitalStock +
                ", NumPhysicalStock=" + NumPhysicalStock +
                ", NumPrice=" + NumPrice +
                ", NumMaxStock=" + NumMaxStock +
                ", NumMinStock=" + NumMinStock +
                ", IsDiscontable='" + IsDiscontable + '\'' +
                ", DiscountType='" + DiscountType + '\'' +
                ", NumDiscountMax=" + NumDiscountMax +
                ", BrandCod='" + BrandCod + '\'' +
                ", BrandName='" + BrandName + '\'' +
                ", CategoryCod='" + CategoryCod + '\'' +
                ", CategoryName='" + CategoryName + '\'' +
                ", CategoryDadCod='" + CategoryDadCod + '\'' +
                ", CategoryDadName='" + CategoryDadName + '\'' +
                ", CurrencyCod='" + CurrencyCod + '\'' +
                ", CurrencySymbol='" + CurrencySymbol + '\'' +
                ", FileCod='" + FileCod + '\'' +
                ", FileRoute='" + FileRoute + '\'' +
                ", NumTrend=" + NumTrend +
                '}';
    }
}
