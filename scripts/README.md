# Variables de entorno locales

`.env.example` es la plantilla versionada. Copiala a `.env` y rellena tus
valores locales reales.

`.env` contiene secretos locales y no se versiona.

En cada terminal de PowerShell, carga las variables con dot-sourcing:

```powershell
. .\scripts\load-env.ps1
```

El punto inicial es obligatorio para que las variables queden disponibles en la
sesion actual.

`CONFIG_REPO_URI` debe apuntar al repositorio Git local externo de configuracion.
Ejemplo:

```text
CONFIG_REPO_URI=file:///C:/Users/Usuario/Desktop/config-repo
```

Despues arranca los servicios desde la raiz del proyecto:

```powershell
mvn -pl eureka-server spring-boot:run
mvn -pl config-server spring-boot:run
mvn -pl auth-server spring-boot:run
mvn -pl cliente-service spring-boot:run
mvn -pl cuenta-service spring-boot:run
mvn -pl operacion-service spring-boot:run
mvn -pl api-gateway spring-boot:run
```
