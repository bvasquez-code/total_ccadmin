# Almacenamiento de archivos

`uploads/` es el directorio de datos administrado por el backend. No forma parte
del código compilado y debe montarse como volumen persistente en cada despliegue.

Variables de entorno:

- `APP_FILE_STORAGE_ROOT`: ruta física dentro del servidor o contenedor.
- `APP_FILE_PUBLIC_BASE_URL`: URL pública del backend usada para construir las rutas.
- `APP_FILE_MAXIMUM_SIZE_BYTES`: tamaño máximo aceptado por archivo.

Ejemplo para Docker:

```text
APP_FILE_STORAGE_ROOT=/app/data/uploads
APP_FILE_PUBLIC_BASE_URL=https://api.empresa.com
```

La columna `app_file.Route` conserva únicamente una clave relativa, por ejemplo:

```text
image/IMG260716764c821bb41.jpg
```
