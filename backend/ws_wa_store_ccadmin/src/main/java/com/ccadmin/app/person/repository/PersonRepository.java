package com.ccadmin.app.person.repository;

import com.ccadmin.app.person.model.entity.PersonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

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

    @Query(value = """
            select p.*
            from person p
            where p.PersonCod = :personCod
              and p.Status = 'A'
            limit 1
            """, nativeQuery = true)
    Optional<PersonEntity> findActiveByPersonCod(@Param("personCod") String personCod);

    @Query(value = """
            select count(1)
            from person p
            where p.PersonCod = :personCod
            """, nativeQuery = true)
    int countByPersonCod(@Param("personCod") String personCod);
}
