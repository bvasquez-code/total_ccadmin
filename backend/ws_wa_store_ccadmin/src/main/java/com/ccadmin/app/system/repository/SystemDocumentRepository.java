package com.ccadmin.app.system.repository;

import com.ccadmin.app.system.model.entity.SystemDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemDocumentRepository  extends JpaRepository<SystemDocumentEntity,String> {
}
