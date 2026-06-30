package com.ccadmin.app.sunat.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "sunat_document_file")
public class SunatDocumentFileEntity extends AuditTableEntity implements Serializable {

    @Id
    public String SunatFileCod;
    public String SunatDocumentCod;
    public String FileType;
    public String FileName;
    public String FilePath;
    public String ContentType;
    public Long SizeBytes;
    public String Sha256Hash;

    public SunatDocumentFileEntity validate() {
        if (SunatFileCod == null || SunatFileCod.isBlank())
            throw new IllegalArgumentException("SunatFileCod requerido");
        if (SunatDocumentCod == null || SunatDocumentCod.isBlank())
            throw new IllegalArgumentException("SunatDocumentCod requerido");
        if (FileType == null || FileType.isBlank())
            throw new IllegalArgumentException("FileType requerido");
        if (FileName == null || FileName.isBlank())
            throw new IllegalArgumentException("FileName requerido");
        if (FilePath == null || FilePath.isBlank())
            throw new IllegalArgumentException("FilePath requerido");
        return this;
    }

    @Override
    public SunatDocumentFileEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }
}
