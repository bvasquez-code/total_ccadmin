package com.ccadmin.app.product.controller;

import com.ccadmin.app.product.model.dto.CategoryRegisterMassiveDto;
import com.ccadmin.app.product.model.entity.CategoryEntity;
import com.ccadmin.app.product.service.CategoryService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll(@RequestParam String Query, int Page) {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.categoryService.findAll(Query, Page)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findDataForm")
    public ResponseEntity<ResponseWsDto> findDataForm(@RequestParam String CategoryCod) {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    this.categoryService.findDataForm(CategoryCod), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("save")
    public ResponseEntity<ResponseWsDto> save(@RequestBody CategoryEntity category) {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.categoryService.save(category)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("saveAll")
    public ResponseEntity<ResponseWsDto> saveAll(@RequestBody CategoryRegisterMassiveDto categoryRegisterMassive) {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    this.categoryService.saveAll(categoryRegisterMassive), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findDataFormMassive")
    public ResponseEntity<ResponseWsDto> findDataFormMassive() {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    this.categoryService.findDataFormMassive(), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

}
