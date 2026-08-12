package com.ccadmin.app.shared.repository;

import com.ccadmin.app.shared.model.idto.IAddressLocationDto;
import com.ccadmin.app.shared.model.idto.ILocationOptionDto;
import com.ccadmin.app.shared.model.entity.UbigeoDepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UbigeoRepository extends JpaRepository<UbigeoDepartmentEntity, String> {

    @Query(value = """
            select ud.DepartmentCod as Code,
                   ud.Name as Name,
                   null as Latitude,
                   null as Longitude
            from ubigeo_department ud
            order by ud.Name
            """, nativeQuery = true)
    List<ILocationOptionDto> findDepartments();

    @Query(value = """
            select up.ProvinceCod as Code,
                   up.Name as Name,
                   null as Latitude,
                   null as Longitude
            from ubigeo_province up
            where up.DepartmentCod = :departmentCod
            order by up.Name
            """, nativeQuery = true)
    List<ILocationOptionDto> findProvinces(@Param("departmentCod") String departmentCod);

    @Query(value = """
            select ud.DistrictCod as Code,
                   ud.Name as Name,
                   null as Latitude,
                   null as Longitude
            from ubigeo_district ud
            where ud.ProvinceCod = :provinceCod
            order by ud.Name
            """, nativeQuery = true)
    List<ILocationOptionDto> findDistricts(@Param("provinceCod") String provinceCod);

    @Query(value = """
            select c.CountryCod as CountryCod,
                   c.CountryName as CountryName,
                   udp.Name as StateName,
                   ud.Name as CityName
            from ubigeo_district ud
            inner join ubigeo_province up
                on up.ProvinceCod = ud.ProvinceCod
               and up.DepartmentCod = ud.DepartmentCod
            inner join ubigeo_department udp
                on udp.DepartmentCod = ud.DepartmentCod
            inner join country c
                on c.CountryCod = 'PER'
               and c.Status = 'A'
            where ud.DistrictCod = :ubigeoCod
            """, nativeQuery = true)
    Optional<IAddressLocationDto> findPeruLocation(@Param("ubigeoCod") String ubigeoCod);
}
