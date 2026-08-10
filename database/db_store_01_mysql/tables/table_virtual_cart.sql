DROP PROCEDURE IF EXISTS `p_manage_virtual_cart`;

DELIMITER $$

CREATE PROCEDURE `p_manage_virtual_cart`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;
    DECLARE v_constraint_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'virtual_cart';

    IF v_table_exists = 0 THEN
        CREATE TABLE `virtual_cart` (
          `CartCod` char(36) NOT NULL COMMENT 'Identificador UUID no predecible del carrito virtual',
          `ClientCod` varchar(16) DEFAULT NULL COMMENT 'Codigo del cliente propietario; puede ser nulo para un invitado',
          `StoreCod` varchar(4) NOT NULL COMMENT 'Unica tienda seleccionada para consultar stock y convertir el carrito',
          `PresaleCod` varchar(16) DEFAULT NULL COMMENT 'Codigo de preventa generado desde el carrito',
          `SaleCod` varchar(16) DEFAULT NULL COMMENT 'Codigo de venta generado desde el carrito',
          `CartData` json NOT NULL COMMENT 'Contenido temporal del carrito expresado como JSON',
          `CartStatus` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado del carrito (A:Activo, C:Convertido, B:Abandonado, E:Expirado)',
          `ExpiresDate` datetime NOT NULL COMMENT 'Fecha a partir de la cual el carrito puede marcarse expirado y eliminarse',
          `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario o actor tecnico que creo el registro',
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creacion del registro',
          `ModifyUser` varchar(16) DEFAULT NULL COMMENT 'Ultimo usuario o actor tecnico que modifico el registro',
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de ultima modificacion del registro',
          `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado del registro (A:Activo, I:Inactivo)',
          PRIMARY KEY (`CartCod`),
          UNIQUE KEY `uq_virtual_cart_presale` (`PresaleCod`),
          UNIQUE KEY `uq_virtual_cart_sale` (`SaleCod`),
          KEY `idx_virtual_cart_client` (`ClientCod`,`CartStatus`,`Status`),
          KEY `idx_virtual_cart_store` (`StoreCod`,`CartStatus`,`Status`),
          KEY `idx_virtual_cart_expiration` (`CartStatus`,`ExpiresDate`),
          CONSTRAINT `fk_virtual_cart_client` FOREIGN KEY (`ClientCod`) REFERENCES `client` (`ClientCod`),
          CONSTRAINT `fk_virtual_cart_store` FOREIGN KEY (`StoreCod`) REFERENCES `store` (`StoreCod`),
          CONSTRAINT `fk_virtual_cart_presale` FOREIGN KEY (`PresaleCod`) REFERENCES `presale_head` (`PresaleCod`),
          CONSTRAINT `fk_virtual_cart_sale` FOREIGN KEY (`SaleCod`) REFERENCES `sale_head` (`SaleCod`),
          CONSTRAINT `chk_virtual_cart_status_value` CHECK (`CartStatus` IN ('A','C','B','E')),
          CONSTRAINT `chk_virtual_cart_status` CHECK (`Status` IN ('A','I'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

        SELECT 'Tabla virtual_cart creada desde cero.' AS Mensaje;
    ELSE
        SELECT COUNT(*) INTO v_constraint_exists
        FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'virtual_cart'
          AND constraint_name = 'chk_virtual_cart_status_value';

        IF v_constraint_exists > 0 THEN
            ALTER TABLE `virtual_cart`
                DROP CHECK `chk_virtual_cart_status_value`;
        END IF;

        UPDATE `virtual_cart`
        SET `CartStatus` = CASE `CartStatus`
            WHEN 'ACTIVE' THEN 'A'
            WHEN 'CONVERTED' THEN 'C'
            WHEN 'ABANDONED' THEN 'B'
            WHEN 'EXPIRED' THEN 'E'
            ELSE `CartStatus`
        END;

        ALTER TABLE `virtual_cart`
            MODIFY COLUMN `CartStatus` char(1) NOT NULL DEFAULT 'A'
                COMMENT 'Estado del carrito (A:Activo, C:Convertido, B:Abandonado, E:Expirado)',
            ADD CONSTRAINT `chk_virtual_cart_status_value`
                CHECK (`CartStatus` IN ('A','C','B','E'));

        SELECT 'Tabla virtual_cart actualizada con estados operativos de un caracter.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_virtual_cart`();
DROP PROCEDURE `p_manage_virtual_cart`;
