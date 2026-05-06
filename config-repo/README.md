# NovaBank Config Repo Template

Esta carpeta es una plantilla versionada para crear el repositorio Git local
que consume `config-server`.

No ejecutes `git init` dentro de esta carpeta del monorepo.

## Crear el repositorio local externo

1. Crear una carpeta fuera del proyecto:

```powershell
mkdir ~/novabank-config-repo
```

2. Copiar el contenido de `config-repo/` a esa carpeta externa.

3. Inicializar Git en la carpeta externa:

```powershell
cd ~/novabank-config-repo
git init
git add .
git commit -m "Configuracion inicial de microservicios NovaBank"
```

4. Si se usa una ruta distinta, definir la variable de entorno:

```powershell
$env:CONFIG_REPO_URI="file:///ruta/al/novabank-config-repo"
```

5. Arrancar `config-server`. Por defecto buscara:

```text
file://${user.home}/novabank-config-repo
```

## Archivos esperados

- `application.yml`
- `cliente-service.yml`
- `cuenta-service.yml`
- `operacion-service.yml`
- `auth-server.yml`
- `api-gateway.yml`
