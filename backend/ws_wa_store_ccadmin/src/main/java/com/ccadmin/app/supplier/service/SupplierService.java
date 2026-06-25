package com.ccadmin.app.supplier.service;

import com.ccadmin.app.person.model.entity.PersonEntity;
import com.ccadmin.app.person.shared.PersonShared;
import com.ccadmin.app.shared.model.dto.ResponsePageSearch;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.service.SearchService;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.supplier.model.entity.SupplierEntity;
import com.ccadmin.app.supplier.repository.SupplierRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SupplierService extends SessionService {

    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private PersonShared personShared;

    private SearchService searchService;

    @Transactional
    public SupplierEntity save(SupplierEntity Supplier)
    {
        this.personShared.save(Supplier.Person);
        Supplier.PersonCod = Supplier.Person.PersonCod;
        Supplier.SupplierCod = Supplier.Person.PersonCod;
        Supplier.addSession(getUserCod(),!this.supplierRepository.existsById(Supplier.PersonCod));
        this.supplierRepository.save(Supplier);
        return findById(Supplier.SupplierCod);
    }

    public SupplierEntity findByDocumentNum(String DocumentType,String DocumentNum)
    {
        PersonEntity Person = this.personShared.findByDocumentNum(DocumentType,DocumentNum);

        if( Person == null ) return null;

        SupplierEntity Supplier = this.supplierRepository.findByPersonCod(Person.PersonCod);

        if( Supplier != null )
        {
            Supplier.Person = Person;
            return Supplier;
        }

        Supplier = new SupplierEntity();
        Supplier.SupplierCod = Person.PersonCod;
        Supplier.PersonCod = Person.PersonCod;
        Supplier.addSession(getUserCod(),true);
        this.supplierRepository.save(Supplier);

        return findById(Supplier.SupplierCod);
    }

    public ResponsePageSearch findAll(String Query, int Page)
    {
        SearchDto search = new SearchDto(Query,Page);
        this.searchService = new SearchService(this.supplierRepository);
        ResponsePageSearch responsePage = this.searchService.findAll(search,10);

        if( responsePage.resultSearch !=null )
        {
            for(var Supplier : (List<SupplierEntity>)responsePage.resultSearch)
            {
                Supplier.Person = this.personShared.findById(Supplier.PersonCod);
            }
        }

        return responsePage;
    }

    public ResponseWsDto findDataForm(String supplierCod) {

        ResponseWsDto rpt = new ResponseWsDto();

        rpt.AddResponseAdditional("Supplier",findById(supplierCod));

        return rpt;
    }

    public SupplierEntity findById(String SupplierCod)
    {
        SupplierEntity Supplier = this.supplierRepository.findById(SupplierCod).get();
        Supplier.Person = this.personShared.findById(Supplier.PersonCod);
        return Supplier;
    }

    public List<SupplierEntity> findAllById(List<String> SupplierCodList){
        List<SupplierEntity> supplierList = this.supplierRepository.findAllById(SupplierCodList);
        List<PersonEntity> personList = this.personShared.findAllById( supplierList.stream().map( Supplier -> Supplier.PersonCod ).collect(Collectors.toList()) );

        for(var Supplier : supplierList){
            Supplier.Person = personList.stream()
                    .filter( Person -> Person.PersonCod.equals(Supplier.PersonCod) )
                    .findFirst()
                    .orElse(null);
        }
        return supplierList;
    }
}
