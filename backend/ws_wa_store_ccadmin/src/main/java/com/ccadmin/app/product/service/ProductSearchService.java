package com.ccadmin.app.product.service;

import com.ccadmin.app.product.model.dto.ProductInfoDto;
import com.ccadmin.app.product.model.dto.ProductRegisterDto;
import com.ccadmin.app.product.model.entity.ProductBarcodeEntity;
import com.ccadmin.app.product.model.entity.ProductEntity;
import com.ccadmin.app.product.repository.*;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.system.shared.AppFileShared;
import com.ccadmin.app.system.shared.SystemDocumentShared;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ProductSearchService {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductBarcodeRepository productBarcodeRepository;
    @Autowired
    private ProductConfigRepository productConfigRepository;
    @Autowired
    private ProductVariantRepository productVariantRepository;
    @Autowired
    private ProductInfoRepository productInfoRepository;
    @Autowired
    private ProductInfoWarehouseRepository productInfoWarehouseRepository;
    @Autowired
    private ProductPictureRepository productPictureRepository;
    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private AppFileShared appFileShared;
    @Autowired
    private SystemDocumentShared systemDocumentShared;
    private SearchTService<ProductEntity> searchTService;

    public ProductEntity findById(String ProductCod)
    {
        return this.productRepository.findById(ProductCod).get();
    }

    public ResponsePageSearchT<ProductEntity> findAll(String Query, int Page){

        if(Query!= null && !Query.isEmpty()){
            Optional<ResponsePageSearchT<ProductEntity>> rptOne = findAllOneResult(Query,Page);
            if(rptOne.isPresent()){
                return rptOne.get();
            }
        }
        this.searchTService = new SearchTService<>(this.productRepository);
        SearchDto search = new SearchDto(Query,Page);
        return this.searchTService.findAll(search,10);
    }

    private Optional<ResponsePageSearchT<ProductEntity>> findAllOneResult(String ProductCod, int Page){
        return this.productBarcodeRepository.findById(ProductCod)
                .flatMap( productBarcode -> this.productRepository.findById(productBarcode.ProductCod)
                        .map( product -> new ResponsePageSearchT<>(product,Page,10) )
                        .or(Optional::empty))
                .or(Optional::empty);
    }

    public ProductInfoDto findDetailById(String ProductCod, String StoreCod)
    {
        ProductInfoDto productInfoDto = new ProductInfoDto();
        productInfoDto.Product = productRepository.findById(ProductCod).orElse(null);
        productInfoDto.Config = productConfigRepository.findById(ProductCod).orElse(null);
        productInfoDto.VariantList = productVariantRepository.findAllVariantProduct(ProductCod);
        productInfoDto.InfoList = productInfoRepository.findInfoStore(ProductCod,StoreCod);
        productInfoDto.InfoWarehouseList = productInfoWarehouseRepository.findInfoWarehouse(StoreCod,ProductCod);
        productInfoDto.Picture = productPictureRepository.findPrincipal(ProductCod);
        return productInfoDto;
    }

    public ResponseWsDto findDataForm(String ProductCod) {

        ResponseWsDto rpt = new ResponseWsDto();

        if( ProductCod != null && !ProductCod.isEmpty())
        {
            ProductRegisterDto productRegister = new ProductRegisterDto();
            productRegister.product = this.findById(ProductCod);
            productRegister.config = this.productConfigRepository.findById(ProductCod).orElse(null);
            productRegister.pictureList = this.productPictureRepository.findAllByProductCod(ProductCod);
            productRegister.productBarcode = this.productBarcodeRepository.findByProductCod(ProductCod);

            for(var image : productRegister.pictureList){
                image.appFile = this.appFileShared.findById(image.FileCod);
            }
            rpt.AddResponseAdditional("product",productRegister);
        }
        rpt.AddResponseAdditional("brandList",this.brandRepository.findAllActive());
        rpt.AddResponseAdditional("categoryList",this.categoryRepository.findAllActiveNoDad());

        return rpt;
    }

    public ProductEntity findByBarCode(String BarCode){

        Optional<ProductBarcodeEntity> productBarcodeOp = this.productBarcodeRepository.findById(BarCode);

        String ProductCod = (productBarcodeOp.isPresent()) ? productBarcodeOp.get().ProductCod : BarCode;

        return findById(ProductCod);
    }

    public List<ProductEntity> findAll(){
        return this.productRepository.findAll();
    }

    public List<ProductEntity> findAllById(List<String> ProductCodList){
        return this.productRepository.findAllById(ProductCodList);
    }

    public List<ProductEntity> findByBrandCod(String BrandCod){
        return this.productRepository.findByBrandCod(BrandCod);
    }
    public List<ProductEntity> findByCategoryCod(String CategoryCod){
        return this.productRepository.findByCategoryCod(CategoryCod);
    }

    public ResponseWsDto findDataFormMassive() {

        ResponseWsDto rpt = new ResponseWsDto();
        rpt.AddResponseAdditional("brandList",this.brandRepository.findAllActive());
        rpt.AddResponseAdditional("categoryList",this.categoryRepository.findAllActiveNoDad());
        rpt.AddResponseAdditional("bulkUploadFormatProducts",this.systemDocumentShared.findById("EXCEL_REG_MASIVO_PROD"));
        return rpt;
    }
}
