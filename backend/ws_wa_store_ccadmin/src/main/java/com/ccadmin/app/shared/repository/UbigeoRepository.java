package com.ccadmin.app.shared.repository;

import com.ccadmin.app.delivery.model.idto.IUbigeoOptionDto;
import com.ccadmin.app.shared.model.entity.UbigeoDepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UbigeoRepository extends JpaRepository<UbigeoDepartmentEntity, String> {

    @Query(value = """
            select ud.DepartmentCod as Code,
                   ud.Name as Name
            from ubigeo_department ud
            order by ud.Name
            """, nativeQuery = true)
    List<IUbigeoOptionDto> findDepartments();

    @Query(value = """
            select up.ProvinceCod as Code,
                   up.Name as Name
            from ubigeo_province up
            where up.DepartmentCod = :departmentCod
            order by up.Name
            """, nativeQuery = true)
    List<IUbigeoOptionDto> findProvinces(@Param("departmentCod") String departmentCod);

    @Query(value = """
            select ud.DistrictCod as Code,
                   ud.Name as Name
            from ubigeo_district ud
            where ud.ProvinceCod = :provinceCod
            order by ud.Name
            """, nativeQuery = true)
    List<IUbigeoOptionDto> findDistricts(@Param("provinceCod") String provinceCod);

    @Query(value = """
            select count(1)
            from ubigeo_district ud
            inner join ubigeo_province up
                on up.ProvinceCod = ud.ProvinceCod
               and up.DepartmentCod = ud.DepartmentCod
            inner join ubigeo_department udp
                on udp.DepartmentCod = ud.DepartmentCod
            where ud.DistrictCod = :ubigeoCod
            """, nativeQuery = true)
    int countDistrictByCode(@Param("ubigeoCod") String ubigeoCod);
}
