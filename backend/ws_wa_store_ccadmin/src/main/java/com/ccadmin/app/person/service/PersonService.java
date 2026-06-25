package com.ccadmin.app.person.service;

import com.ccadmin.app.person.model.entity.PersonEntity;
import com.ccadmin.app.person.repository.PersonRepository;
import com.ccadmin.app.shared.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PersonService extends SessionService {

    @Autowired
    private PersonRepository personRepository;

    public PersonEntity save(PersonEntity person)
    {
        person.PersonCod = person.DocumentNum;
        person.addSession(getUserCod(),!this.personRepository.existsById(person.PersonCod));
        return this.personRepository.save(person);
    }

    public PersonEntity findById(String PersonCod)
    {
        return this.personRepository.findById(PersonCod).get();
    }

    public PersonEntity findByDocumentNum(String DocumentType,String DocumentNum)
    {
        return this.personRepository.findByDocumentNum(DocumentType,DocumentNum);
    }

    public List<PersonEntity> findAllById(List<String> PersonCodList){
        return this.personRepository.findAllById(PersonCodList);
    }
}
