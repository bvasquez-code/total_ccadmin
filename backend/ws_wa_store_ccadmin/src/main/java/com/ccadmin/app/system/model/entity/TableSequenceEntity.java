package com.ccadmin.app.system.model.entity;


import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "table_sequence")
public class TableSequenceEntity implements Serializable {

    @Id
    public Long SequenceTrx;          // secuencia actual (PK)
    public String Prefix;             // prefijo (2)
    public String SequenceTableType;  // tipo/tabla
    public Integer length;            // longitud numérica
    public String UsePrefix;          // 'S'/'N'

}
