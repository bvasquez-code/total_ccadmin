package com.ccadmin.app.person.repository;

import com.ccadmin.app.person.model.entity.PersonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PersonRepository extends JpaRepository<PersonEntity,String> {


    @Query( value = """
            select * from person p
            where
            p.DocumentType = :DocumentType
            and p.DocumentNum = :DocumentNum
            and p.Status = 'A'
            """,nativeQuery = true)
    public PersonEntity findByDocumentNum(
             @Param("DocumentType") String DocumentType
            ,@Param("DocumentNum") String DocumentNum
    );
}
