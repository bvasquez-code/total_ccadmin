DROP PROCEDURE IF EXISTS `p_manage_sale_billing`;

DELIMITER $$

CREATE PROCEDURE `p_manage_sale_billing`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'sale_billing';

    IF v_table_exists = 0 THEN
        CREATE TABLE `sale_billing` (
          `SaleCod` varchar(16) NOT NULL COMMENT 'Codigo de la venta relacionada',
          `PersonCod` varchar(16) DEFAULT NULL COMMENT 'Codigo de la persona utilizada para facturacion',
          `DocumentTypeRequest` char(2) DEFAULT NULL COMMENT 'Tipo de comprobante solicitado (01:Factura, 03:Boleta)',
          `DocumentType` char(2) DEFAULT NULL COMMENT 'Tipo de documento de identidad de la persona facturada',
          `DocumentNum` varchar(16) DEFAULT NULL COMMENT 'Numero de documento de identidad de la persona facturada',
          `LegalName` varchar(256) DEFAULT NULL COMMENT 'Nombre completo o razon social impreso y enviado a facturacion electronica',
          `CommercialName` varchar(128) DEFAULT NULL COMMENT 'Nombre comercial enviado a facturacion electronica',
          `Address` varchar(256) DEFAULT NULL COMMENT 'Domicilio fiscal enviado a facturacion electronica',
          `UbigeoCod` varchar(12) DEFAULT NULL COMMENT 'Codigo de ubigeo enviado a facturacion electronica',
          `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario que creo el registro',
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creacion del registro',
          `ModifyUser` varchar(16) DEFAULT NULL COMMENT 'Ultimo usuario que modifico el registro',
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de ultima modificacion del registro',
          `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado del registro (A:Activo, I:Inactivo)',
          PRIMARY KEY (`SaleCod`),
          KEY `idx_sale_billing_person` (`PersonCod`),
          KEY `idx_sale_billing_document` (`DocumentType`,`DocumentNum`),
          CONSTRAINT `fk_sale_billing_sale` FOREIGN KEY (`SaleCod`) REFERENCES `sale_head` (`SaleCod`),
          CONSTRAINT `fk_sale_billing_person` FOREIGN KEY (`PersonCod`) REFERENCES `person` (`PersonCod`),
          CONSTRAINT `chk_sale_billing_document_request` CHECK (`DocumentTypeRequest` IS NULL OR `DocumentTypeRequest` IN ('01','03')),
          CONSTRAINT `chk_sale_billing_status` CHECK (`Status` IN ('A','I'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

        SELECT 'Tabla sale_billing creada desde cero.' AS Mensaje;
    ELSE
        SELECT 'Tabla sale_billing ya existe. No se realizaron cambios estructurales.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_sale_billing`();

INSERT INTO `sale_billing`
(`SaleCod`, `PersonCod`, `DocumentTypeRequest`, `DocumentType`, `DocumentNum`,
 `LegalName`, `CommercialName`, `Address`, `UbigeoCod`,
 `CreationUser`, `CreationDate`, `ModifyUser`, `ModifyDate`, `Status`)
SELECT
    sh.`SaleCod`,
    p.`PersonCod`,
    CASE WHEN fiscal_document.`DocumentType` IN ('01','03')
         THEN fiscal_document.`DocumentType` ELSE NULL END,
    p.`DocumentType`,
    p.`DocumentNum`,
    COALESCE(
        NULLIF(TRIM(p.`BusinessName`), ''),
        NULLIF(TRIM(CONCAT_WS(' ', p.`Names`, p.`LastNames`)), ''),
        NULLIF(TRIM(p.`CommercialName`), '')
    ),
    p.`CommercialName`,
    p.`Address`,
    p.`UbigeoCod`,
    sh.`CreationUser`,
    sh.`CreationDate`,
    sh.`ModifyUser`,
    sh.`ModifyDate`,
    'A'
FROM `sale_head` sh
LEFT JOIN (
    SELECT sd.`SaleCod`, MAX(sd.`DocumentType`) AS `DocumentType`, MAX(sd.`ClientCod`) AS `ClientCod`
    FROM `sale_document` sd
    WHERE sd.`Status` = 'A'
      AND sd.`DocumentRole` = 'F'
      AND sd.`DocumentType` IN ('01','03')
    GROUP BY sd.`SaleCod`
) fiscal_document ON fiscal_document.`SaleCod` = sh.`SaleCod`
LEFT JOIN `client` billing_client
       ON billing_client.`ClientCod` = COALESCE(fiscal_document.`ClientCod`, sh.`ClientCod`)
LEFT JOIN `person` p ON p.`PersonCod` = billing_client.`PersonCod`
WHERE NOT EXISTS (
    SELECT 1
    FROM `sale_billing` sb
    WHERE sb.`SaleCod` = sh.`SaleCod`
);

DROP PROCEDURE `p_manage_sale_billing`;
