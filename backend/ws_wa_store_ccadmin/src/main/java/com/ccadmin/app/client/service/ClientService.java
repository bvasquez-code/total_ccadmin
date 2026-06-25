package com.ccadmin.app.client.service;

import com.ccadmin.app.client.model.entity.ClientEntity;
import com.ccadmin.app.client.repository.ClientRepository;
import com.ccadmin.app.person.model.entity.PersonEntity;
import com.ccadmin.app.person.shared.PersonShared;
import com.ccadmin.app.shared.model.dto.ResponsePageSearch;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.service.SearchService;
import com.ccadmin.app.shared.service.SessionService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClientService extends SessionService {

    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private PersonShared personShared;

    private SearchService searchService;

    @Transactional
    public ClientEntity save(ClientEntity Client)
    {
        this.personShared.save(Client.Person);
        Client.PersonCod = Client.Person.PersonCod;
        Client.ClientCod = Client.Person.PersonCod;
        Client.addSession(getUserCod(),!this.clientRepository.existsById(Client.PersonCod));
        this.clientRepository.save(Client);
        return findById(Client.ClientCod);
    }

    public ClientEntity findByDocumentNum(String DocumentType,String DocumentNum)
    {
        PersonEntity Person = this.personShared.findByDocumentNum(DocumentType,DocumentNum);

        if( Person == null ) return null;

        ClientEntity Client = this.clientRepository.findByPersonCod(Person.PersonCod);

        if( Client != null )
        {
            Client.Person = Person;
            return Client;
        }

        Client = new ClientEntity();
        Client.ClientCod = Person.PersonCod;
        Client.PersonCod = Person.PersonCod;
        Client.addSession(getUserCod(),true);
        this.clientRepository.save(Client);

        return findById(Client.ClientCod);
    }

    public ResponsePageSearch findAll(String Query, int Page)
    {
        SearchDto search = new SearchDto(Query,Page);
        this.searchService = new SearchService(this.clientRepository);
        ResponsePageSearch responsePage = this.searchService.findAll(search,10);

        if( responsePage.resultSearch !=null )
        {
            for(var Client : (List<ClientEntity>)responsePage.resultSearch)
            {
                Client.Person = this.personShared.findById(Client.PersonCod);
            }
        }

        return responsePage;
    }

    public ResponseWsDto findDataForm(String clientCod) {

        ResponseWsDto rpt = new ResponseWsDto();

        rpt.AddResponseAdditional("Client",findById(clientCod));

        return rpt;
    }

    public ClientEntity findById(String ClientCod)
    {
        ClientEntity Client = this.clientRepository.findById(ClientCod).get();
        Client.Person = this.personShared.findById(Client.PersonCod);
        return Client;
    }

    public List<ClientEntity> findAllById(List<String> ClientCodList){
        List<ClientEntity> clientList = this.clientRepository.findAllById(ClientCodList);
        List<PersonEntity> personList = this.personShared.findAllById( clientList.stream().map( Client -> Client.PersonCod ).collect(Collectors.toList()) );

        for(var Client : clientList){
            Client.Person = personList.stream()
                            .filter( Person -> Person.PersonCod.equals(Client.PersonCod) )
                            .findFirst()
                            .orElse(null);
        }
        return clientList;
    }
}
