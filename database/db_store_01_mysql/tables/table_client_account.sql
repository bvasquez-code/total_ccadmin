DROP PROCEDURE IF EXISTS `p_manage_client_account`;

DELIMITER $$

CREATE PROCEDURE `p_manage_client_account`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'client_account';

    IF v_table_exists = 0 THEN
        CREATE TABLE `client_account` (
          `ClientAccountID` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador interno de la cuenta de acceso del cliente',
          `ClientCod` varchar(16) NOT NULL COMMENT 'Codigo del cliente propietario de la cuenta; la estructura permite varias cuentas por cliente',
          `Email` varchar(254) NOT NULL COMMENT 'Correo utilizado para iniciar sesion en la tienda virtual',
          `PasswordHash` varchar(256) NOT NULL COMMENT 'Hash seguro de la contrasena; nunca debe almacenar la contrasena en texto plano',
          `IsEmailVerified` char(1) NOT NULL DEFAULT 'N' COMMENT 'Indica si el correo fue verificado (S:Si, N:No)',
          `EmailVerificationTokenHash` varchar(256) DEFAULT NULL COMMENT 'Hash del token temporal utilizado para verificar el correo',
          `EmailVerificationExpireDate` datetime DEFAULT NULL COMMENT 'Fecha de expiracion del token de verificacion del correo',
          `PasswordRecoveryTokenHash` varchar(256) DEFAULT NULL COMMENT 'Hash del token temporal utilizado para recuperar la contrasena',
          `PasswordRecoveryExpireDate` datetime DEFAULT NULL COMMENT 'Fecha de expiracion del token de recuperacion de contrasena',
          `FailedLoginAttempts` int NOT NULL DEFAULT 0 COMMENT 'Cantidad de intentos consecutivos de inicio de sesion fallidos',
          `LockUntilDate` datetime DEFAULT NULL COMMENT 'Fecha hasta la cual la cuenta permanece bloqueada temporalmente',
          `LastLoginDate` datetime DEFAULT NULL COMMENT 'Fecha del ultimo inicio de sesion exitoso',
          `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario o actor tecnico que creo el registro; para autorregistro web se utilizara USER_WEB',
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creacion del registro',
          `ModifyUser` varchar(16) DEFAULT NULL COMMENT 'Ultimo usuario o actor tecnico que modifico el registro',
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de ultima modificacion del registro',
          `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado del registro (A:Activo, I:Inactivo)',
          PRIMARY KEY (`ClientAccountID`),
          UNIQUE KEY `uq_client_account_email` (`Email`),
          KEY `idx_client_account_client` (`ClientCod`,`Status`),
          CONSTRAINT `fk_client_account_client` FOREIGN KEY (`ClientCod`) REFERENCES `client` (`ClientCod`),
          CONSTRAINT `chk_client_account_email_verified` CHECK (`IsEmailVerified` IN ('S','N')),
          CONSTRAINT `chk_client_account_failed_attempts` CHECK (`FailedLoginAttempts` >= 0),
          CONSTRAINT `chk_client_account_status` CHECK (`Status` IN ('A','I'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

        SELECT 'Tabla client_account creada desde cero.' AS Mensaje;
    ELSE
        SELECT 'Tabla client_account ya existe. No se realizaron cambios estructurales.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_client_account`();
DROP PROCEDURE `p_manage_client_account`;
