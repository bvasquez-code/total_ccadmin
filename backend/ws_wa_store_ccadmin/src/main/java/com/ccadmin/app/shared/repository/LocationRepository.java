package com.ccadmin.app.shared.repository;

import com.ccadmin.app.shared.model.entity.CountryEntity;
import com.ccadmin.app.shared.model.idto.IAddressLocationDto;
import com.ccadmin.app.shared.model.idto.ILocationOptionDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<CountryEntity, String> {

    @Query(value = """
            select c.CountryCod as Code,
                   c.CountryName as Name,
                   null as Latitude,
                   null as Longitude
            from country c
            where c.Status = 'A'
            order by c.CountryName
            """, nativeQuery = true)
    List<ILocationOptionDto> findCountries();

    @Query(value = """
            select cast(s.StateId as char) as Code,
                   s.StateName as Name,
                   null as Latitude,
                   null as Longitude
            from state s
            inner join country c
                on c.CountryCod = s.CountryCod
               and c.Status = 'A'
            where s.CountryCod = :countryCod
              and s.Status = 'A'
            order by s.StateName
            """, nativeQuery = true)
    List<ILocationOptionDto> findStates(@Param("countryCod") String countryCod);

    @Query(value = """
            select cast(c.CityId as char) as Code,
                   c.CityName as Name,
                   c.Latitude as Latitude,
                   c.Longitude as Longitude
            from city c
            inner join state s
                on s.StateId = c.StateId
               and s.Status = 'A'
            where c.StateId = :stateId
              and c.Status = 'A'
            order by c.CityName
            """, nativeQuery = true)
    List<ILocationOptionDto> findCities(@Param("stateId") Long stateId);

    @Query(value = """
            select cast(ci.CityId as char) as Code,
                   up.Name as Name,
                   ci.Latitude as Latitude,
                   ci.Longitude as Longitude
            from ubigeo_province up
            inner join ubigeo_department ud
                on ud.DepartmentCod = up.DepartmentCod
            inner join country co
                on co.CountryCod = 'PER'
               and co.Status = 'A'
            inner join state s
                on s.CountryCod = co.CountryCod
               and s.Status = 'A'
            inner join city ci
                on ci.StateId = s.StateId
               and ci.Status = 'A'
               and ci.Latitude is not null
               and ci.Longitude is not null
               and upper(trim(ci.CityName)) = upper(trim(up.Name))
            where up.ProvinceCod = :provinceCod
            order by case
                         when upper(trim(s.StateName)) = upper(trim(ud.Name)) then 0
                         else 1
                     end,
                     ci.CityId
            limit 1
            """, nativeQuery = true)
    Optional<ILocationOptionDto> findPeruProvinceLocation(
            @Param("provinceCod") String provinceCod
    );

    @Query(value = """
            select c.*
            from country c
            where c.CountryCod = :countryCod
              and c.Status = 'A'
            limit 1
            """, nativeQuery = true)
    Optional<CountryEntity> findActiveCountryByCode(
            @Param("countryCod") String countryCod
    );

    @Query(value = """
            select c.CountryCod as CountryCod,
                   c.CountryName as CountryName,
                   s.StateName as StateName,
                   ci.CityName as CityName
            from country c
            inner join state s
                on s.CountryCod = c.CountryCod
               and s.StateId = :stateId
               and s.Status = 'A'
            inner join city ci
                on ci.StateId = s.StateId
               and ci.CityId = :cityId
               and ci.Status = 'A'
            where c.CountryCod = :countryCod
              and c.Status = 'A'
            """, nativeQuery = true)
    Optional<IAddressLocationDto> findForeignLocation(
            @Param("countryCod") String countryCod,
            @Param("stateId") Long stateId,
            @Param("cityId") Long cityId
    );
}
