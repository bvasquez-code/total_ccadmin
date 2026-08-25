package com.ccadmin.app.product.controller;

import com.ccadmin.app.product.model.dto.ProductRegisterDto;
import com.ccadmin.app.product.model.dto.ProductRegisterMassiveDto;
import com.ccadmin.app.product.model.dto.ProductConfigStoreUpdateDto;
import com.ccadmin.app.product.model.entity.ProductPictureEntity;
import com.ccadmin.app.product.service.ProductCreateService;
import com.ccadmin.app.product.service.ProductConfigCreateService;
import com.ccadmin.app.product.service.ProductConfigSearchService;
import com.ccadmin.app.product.service.ProductImageAnalysisService;
import com.ccadmin.app.product.service.ProductSearchService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("api/v1/product")
public class ProductController {

    @Autowired
    private ProductCreateService productCreateService;
    @Autowired
    private ProductConfigCreateService productConfigCreateService;
    @Autowired
    private ProductConfigSearchService productConfigSearchService;
    @Autowired
    private ProductSearchService productSearchService;
    @Autowired
    private ProductImageAnalysisService productImageAnalysisService;

    @GetMapping("findById")
    public ResponseEntity<ResponseWsDto> findById(@RequestParam String ProductCod) {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.productSearchService.findById(ProductCod)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll(@RequestParam String Query, int Page) {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.productSearchService.findAll(Query, Page)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findDetailById")
    public ResponseEntity<ResponseWsDto> findDetailById(@RequestParam String ProductCod, String StoreCod) {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.productSearchService.findDetailById(ProductCod, StoreCod)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findDataForm")
    public ResponseEntity<ResponseWsDto> findDataForm(@RequestParam String ProductCod) {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    this.productSearchService.findDataForm(ProductCod), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findDataConfigForm")
    public ResponseEntity<ResponseWsDto> findDataConfigForm(@RequestParam String ProductCod, @RequestParam String StoreCod) {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    this.productConfigSearchService.findDataConfigForm(ProductCod, StoreCod), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findByBarCode")
    public ResponseEntity<ResponseWsDto> findByBarCode(@RequestParam String BarCode) {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.productSearchService.findByBarCode(BarCode)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findRegisteredByBarCode")
    public ResponseEntity<ResponseWsDto> findRegisteredByBarCode(
            @RequestParam String BarCode
    ) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(
                            this.productSearchService.findRegisteredByBarCode(BarCode)
                    ),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(
                    new ResponseWsDto(ex), HttpStatus.BAD_REQUEST
            );
        }
    }

    @PostMapping(
            value = "analyzeQuickCreateImage",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ResponseWsDto> analyzeQuickCreateImage(
            @RequestPart("frontImage") MultipartFile frontImage,
            @RequestPart("sideImage") MultipartFile sideImage,
            @RequestPart("barcodeImage") MultipartFile barcodeImage
    ) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(
                            this.productImageAnalysisService.analyze(
                                    frontImage, sideImage, barcodeImage
                            )
                    ),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(
                    new ResponseWsDto(ex), HttpStatus.BAD_REQUEST
            );
        }
    }

    @GetMapping("generateProductCode")
    public ResponseEntity<ResponseWsDto> generateProductCode() {
        try {
            ResponseWsDto rpt = new ResponseWsDto();
            return new ResponseEntity<ResponseWsDto>(
                    rpt.okResponse(this.productCreateService.generateProductCode()), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("saveAll")
    public ResponseEntity<ResponseWsDto> saveAll(@RequestBody ProductRegisterMassiveDto productRegisterMassive) {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    this.productCreateService.saveAll(productRegisterMassive), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("save")
    public ResponseEntity<ResponseWsDto> save(@RequestBody ProductRegisterDto product) {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.productCreateService.save(product)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("saveConfigByStores")
    public ResponseEntity<ResponseWsDto> saveConfigByStores(@RequestBody ProductConfigStoreUpdateDto request) {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.productConfigCreateService.saveConfigByStores(request)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("deletePicture")
    public ResponseEntity<ResponseWsDto> deletePicture(@RequestBody ProductPictureEntity productPicture) {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.productCreateService.deletePicture(productPicture)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findDataFormMassive")
    public ResponseEntity<ResponseWsDto> findDataFormMassive() {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    this.productSearchService.findDataFormMassive(), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

}
