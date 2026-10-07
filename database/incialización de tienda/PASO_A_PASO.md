# Copiar y limpiar db_store_01

Abre **PowerShell** y ejecuta estos pasos en orden, en la misma ventana. Cuando MySQL pida la contraseña, escribe la del usuario `root`. Si aparece un error, detente antes de continuar.

## 1. Preparar PowerShell

```powershell
$env:Path = 'C:\Program Files\MySQL\MySQL Server 9.5\bin;' + $env:Path
New-Item -ItemType Directory -Path 'C:\respaldos_ccadmin' -Force
Set-Location 'C:\proyectos\multiple\total_ccadmin\database\incialización de tienda'
```

Si tienes otra versión de MySQL instalada, cambia `MySQL Server 9.5` por el nombre de su carpeta.

## 2. Exportar db_store_01

Detén las aplicaciones que escriben en la base mientras haces el respaldo. Ejecuta:

```powershell
mysqldump --no-defaults -h 127.0.0.1 -P 3306 -u root -p --default-character-set=utf8mb4 --single-transaction --routines --triggers --events --hex-blob --no-tablespaces --set-gtid-purged=OFF --result-file=C:\respaldos_ccadmin\script_copia_bd_dev.sql db_store_01
```

El respaldo queda en `C:\respaldos_ccadmin\script_copia_bd_dev.sql`.

## 3. Crear db_store_lts

```powershell
mysql --no-defaults -h 127.0.0.1 -P 3306 -u root -p --execute="CREATE DATABASE db_store_lts CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
```

Si `db_store_lts` ya existe, el comando falla: no continúes sobre esa base sin revisar qué contiene.

## 4. Restaurar la copia en db_store_lts

Si el respaldo contiene eventos o referencias explícitas a `db_store_01` dentro de rutinas, vistas o triggers, aplica la revisión previa del [README.md](README.md) antes de importarlo.

```powershell
cmd /c 'mysql --no-defaults -h 127.0.0.1 -P 3306 -u root -p --default-character-set=utf8mb4 db_store_lts < C:\respaldos_ccadmin\script_copia_bd_dev.sql'
```

`cmd /c` permite leer el archivo SQL desde PowerShell. La copia se restaura en **db_store_lts**.

## 5. Limpiar db_store_lts

```powershell
cmd /c 'mysql --no-defaults -h 127.0.0.1 -P 3306 -u root -p --default-character-set=utf8mb4 db_store_lts < 02_limpiar_db_store_lts.sql'
```

Espera a que termine y comprueba que muestre `Result = OK`, `InitializedDatabase = db_store_lts` y `ForeignKeyChecks = 1`. Solo entonces continúa.

Carga las dos plantillas de SUNAT antes de exportar. Quedan inactivas; producción espera las credenciales y el certificado que se cargarán en el asistente:

```powershell
cmd /c 'mysql --no-defaults -h 127.0.0.1 -P 3306 -u root -p --default-character-set=utf8mb4 db_store_lts < "..\db_store_01_mysql\insert_sunat_config_test.sql"'
cmd /c 'mysql --no-defaults -h 127.0.0.1 -P 3306 -u root -p --default-character-set=utf8mb4 db_store_lts < "..\db_store_01_mysql\insert_sunat_config_prod.sql"'
```

## 6. Exportar la base limpia

```powershell
mysqldump --no-defaults -h 127.0.0.1 -P 3306 -u root -p --default-character-set=utf8mb4 --single-transaction --routines --triggers --events --hex-blob --no-tablespaces --set-gtid-purged=OFF --result-file=C:\respaldos_ccadmin\script_db_incial_store.sql db_store_lts
```

**El archivo final es `C:\respaldos_ccadmin\script_db_incial_store.sql`.** Ese es el que importarás en una base vacía para las nuevas instalaciones. Incluye también los archivos físicos de las imágenes conservadas; el SQL guarda sus referencias.
