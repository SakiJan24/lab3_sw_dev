# Despliegue automatizado con Jenkins – Zoo Fantástico

Parte 3 (configuración de Jenkins), Parte 4 (prueba del flujo automatizado), Parte 5 paso 2
(pipeline con Docker) y paso 7 (pipeline con pruebas) de la práctica de despliegue.
Complementa a [`docker.md`](docker.md), que cubre el Dockerfile y las pruebas.

| Elemento | Valor |
|---|---|
| Jenkins | `jenkins/jenkins:lts-jdk21` + Maven + Docker CLI, contenedor `jenkins-zoo`, **http://localhost:8080** |
| Repositorio | `https://github.com/SakiJan24/lab3_sw_dev.git` |
| Job Freestyle | `Despliegue-CRUD` → app con `java -jar` en **http://localhost:8090** |
| Job Pipeline | `Pipeline-Docker-CRUD` (lee el `Jenkinsfile`) → app en contenedor en **http://localhost:8081** |
| Imagen de la app | `zoo-fantastic-app:latest` (la misma de `docker.md`) |

## 1. Jenkins en un contenedor

La imagen oficial de Jenkins no trae Maven ni el cliente de Docker, y la versión por defecto
corre con Java 17, mientras que el proyecto compila para Java 21. Por eso se armó una imagen
propia en [`jenkins/Dockerfile`](../../jenkins/Dockerfile):

| Línea / bloque | Qué hace |
|---|---|
| `FROM jenkins/jenkins:lts-jdk21` | Jenkins LTS sobre Java 21, la misma versión del proyecto. |
| `apt-get install maven curl procps` | Maven para `mvn clean package` / `mvn test`; `curl` para verificar la API; `procps` (`pkill`) para detener el JAR anterior. |
| repositorio de Docker + `docker-ce-cli docker-compose-plugin` | Solo el **cliente** de Docker y Compose. El motor es el del host (ver abajo). |
| `JAVA_OPTS=-Djenkins.install.runSetupWizard=false` | Se salta el asistente inicial; el usuario admin lo crea `init.groovy.d/01-admin.groovy`. |
| `jenkins-plugin-cli --plugin-file plugins.txt` | Preinstala Git, Pipeline, Stage View, **Docker Plugin**, Docker Pipeline, JUnit, PostBuildScript y Timestamper. |

[`jenkins/docker-compose.yml`](../../jenkins/docker-compose.yml) levanta el contenedor:

| Configuración | Por qué |
|---|---|
| `8080:8080` | Interfaz web de Jenkins, como pide la guía. |
| `8090:8090` | Puerto de la app que el job Freestyle lanza **dentro** del contenedor de Jenkins (ahí el 8080 ya es de Jenkins). |
| `/var/run/docker.sock:/var/run/docker.sock` | Jenkins usa el Docker del host: las imágenes y contenedores que crea el pipeline quedan visibles con `docker ps` en el host. |
| `group_add: ${DOCKER_GID}` | El socket pertenece al grupo `docker` del host (GID 115 aquí). Agregar ese GID al usuario `jenkins` le permite usar Docker **sin correr Jenkins como root**. |
| `extra_hosts: host.docker.internal:host-gateway` | Desde el contenedor, `host.docker.internal` es el host: así Jenkins llega a MySQL (3306) y a la app publicada en el 8081. |
| volumen `jenkins_home` | Jobs, historial y caché de Maven (`~/.m2`) sobreviven si se recrea el contenedor. |

```bash
cd jenkins
cp .env.example .env            # poner JENKINS_ADMIN_PASSWORD y DOCKER_GID (getent group docker)
docker compose up -d --build
```

Montar `docker.sock` le da a Jenkins control total sobre el Docker del host. Para un
laboratorio local es aceptable y es la opción más simple; en un servidor compartido se usaría
Docker-in-Docker con TLS o agentes dedicados.

## 2. Job Freestyle `Despliegue-CRUD` (Partes 3 y 4)

| Sección del job | Configuración |
|---|---|
| Source Code Management | Git → `https://github.com/SakiJan24/lab3_sw_dev.git`, rama `*/feature/jenkins-pipeline` |
| Build → Execute shell | `mvn -B clean package` (compila, corre las 71 pruebas y genera el JAR) |
| Post-build Actions → Execute scripts (PostBuildScript), solo si el build fue SUCCESS | script de despliegue (abajo) |

```bash
docker compose up -d --wait zoo-mysql            # la app necesita MySQL
pkill -f zoo-fantastic-app.jar || true           # detener la versión anterior
cp target/zoo-fantastico-*-SNAPSHOT.jar "$JENKINS_HOME/deploy/zoo-fantastic-app.jar"
JENKINS_NODE_COOKIE=dontKillMe BUILD_ID=dontKillMe \
PORT=8090 DB_HOST=host.docker.internal \
nohup java -jar "$JENKINS_HOME/deploy/zoo-fantastic-app.jar" > "$JENKINS_HOME/deploy/app.log" 2>&1 &
# ...y espera hasta que GET /api/zones responda 200
```

La guía propone solo `java -jar target/crud-app.jar &`. Así tal cual no funciona en este
proyecto, por cuatro razones:

1. **Jenkins mata los procesos hijos al terminar el build** (*ProcessTreeKiller*). El `&` no
   basta: la app moriría apenas termina el job. `JENKINS_NODE_COOKIE=dontKillMe` (y
   `BUILD_ID` en jobs Freestyle) le dice a Jenkins que no lo toque.
2. **Puerto**: dentro del contenedor de Jenkins el 8080 ya está ocupado por Jenkins. La app lee
   `server.port=${PORT:8080}`, así que con `PORT=8090` arranca en otro puerto, que el compose
   de Jenkins publica en el host.
3. **Base de datos**: la app necesita MySQL. Se levanta solo el servicio `zoo-mysql` del
   `docker-compose.yml` del proyecto y la app se conecta por `host.docker.internal:3306`.
4. **Nombre del JAR**: Maven genera `zoo-fantastico-0.0.1-SNAPSHOT.jar`, no `crud-app.jar`. Se
   copia fuera del workspace para que el siguiente `mvn clean` no borre el JAR en uso, y se
   detiene la versión anterior antes de lanzar la nueva.

**Resultado (build #1, 64 s, SUCCESS)**, ver `evidencias/09-jenkins-freestyle-build1.txt`:

```
Checking out Revision 341b769... (refs/remotes/origin/feature/jenkins-pipeline)
[INFO] Tests run: 71, Failures: 0, Errors: 0, Skipped: 0
[INFO] Building jar: /var/jenkins_home/workspace/Despliegue-CRUD/target/zoo-fantastico-0.0.1-SNAPSHOT.jar
[INFO] BUILD SUCCESS
[PostBuildScript] - [INFO] Executing post build scripts.
 Container zoo-mysql Healthy
App desplegada: http://localhost:8090/api/zones -> 200
Finished: SUCCESS
```

Después de terminar el job, `curl http://localhost:8090/api/zones` desde el host sigue
respondiendo 200: el proceso sobrevivió al build.

## 3. Job Pipeline `Pipeline-Docker-CRUD` (Parte 5 paso 2 y paso 7)

El job es de tipo **Pipeline → Pipeline script from SCM**: Jenkins clona el repositorio y
ejecuta el [`Jenkinsfile`](../../Jenkinsfile) de la raíz. Así el pipeline queda versionado
junto con el código, en vez de vivir solo en la configuración de Jenkins.

### 3.1 Análisis del pipeline de la guía («¡Ojo!»)

```groovy
stage('Docker Run') { steps { sh 'docker run -d -p 8080:8080 crud-app:latest' } }
```

| Problema | Consecuencia | Solución en el `Jenkinsfile` |
|---|---|---|
| `-p 8080:8080` | El 8080 es de Jenkins: falla con *port is already allocated*. | La app se publica en el **8081** (`APP_PORT`). |
| `docker run` sin detener el contenedor anterior | La 1.ª ejecución funciona, pero la 2.ª falla porque el contenedor de la 1.ª sigue ocupando el puerto. Además se acumulan contenedores. | `docker compose down` antes de `docker compose up -d`. |
| Solo se corre la app | La app no arranca sin MySQL (se cae al no encontrar la base de datos). | Compose levanta `zoo-mysql` y `app` juntos; `app` espera a que MySQL esté *healthy*. |
| `FROM openjdk:11-jre-slim` | No ejecuta clases compiladas para Java 21. | Dockerfile multi-etapa con Java 21 (ver `docker.md`). |
| `mvn clean package -DskipTests=false` y luego `mvn test` | Las pruebas corren **dos veces**. | Build con `-DskipTests`; las pruebas corren solo en *Run Tests*. |
| `-d` y nada más | El pipeline sale en verde aunque la app se caiga 2 s después de arrancar. | Etapa **Smoke Test**: espera a que `GET /api/zones` responda 200. |

### 3.2 Etapas

| Etapa | Comando | Qué garantiza |
|---|---|---|
| Checkout SCM | (automático) | Clona la rama configurada del repositorio. |
| Build | `mvn -B clean package -DskipTests` + `archiveArtifacts target/*.jar` | El código compila; el JAR queda adjunto al build en Jenkins. |
| Run Tests | `mvn -B test` + `junit target/surefire-reports/*.xml` | Las 71 pruebas pasan. Jenkins publica el reporte (pestaña *Test Result* y gráfico de tendencia). Si una falla, el pipeline se detiene aquí y **no se despliega**. |
| Docker Build | `docker compose build app` | Construye `zoo-fantastic-app:latest` con el Dockerfile multi-etapa. |
| Docker Run | `docker compose down --remove-orphans` y `docker compose up -d` | Reemplaza los contenedores anteriores por los nuevos. El volumen `mysql_data` no se borra (no se usa `-v`), así que los datos se conservan. |
| Smoke Test | `curl` a `http://host.docker.internal:8081/api/zones` hasta recibir 200 (máx. ~2 min) | La app desplegada responde de verdad. Si no, muestra los logs de `zoo-app` y marca el build como fallido. |
| post | `always` / `success` / `failure` | Mensajes de la guía: «Pipeline terminado.», «¡Despliegue exitoso!», «Error en el pipeline…». |

Opciones del pipeline: `disableConcurrentBuilds()` (dos ejecuciones simultáneas pelearían por
los mismos contenedores), `timestamps()` y conservar solo los últimos 10 builds.

### 3.3 Resultados

Se ejecutó dos veces seguidas para comprobar que una segunda corrida no falla por contenedores
o puertos ocupados (`evidencias/10-…build1.txt` y `evidencias/11-…build2.txt`):

| Etapa | Build #1 | Build #2 |
|---|---|---|
| Checkout SCM | 1,0 s | 0,5 s |
| Build | 8,2 s | 5,1 s |
| Run Tests | 24,9 s | 24,3 s |
| Docker Build | 52,2 s | **0,9 s** |
| Docker Run | 10,8 s | 12,0 s |
| Smoke Test | 6,8 s | 6,8 s |
| **Resultado** | **SUCCESS** | **SUCCESS** (50,8 s en total) |

- Reporte de pruebas en Jenkins: **71 pasaron, 0 fallaron, 0 omitidas**.
- En el build #2 la etapa *Docker Build* tarda menos de 1 s porque el código no cambió y
  Docker reutilizó todas las capas en caché (incluida la de `mvn dependency:go-offline`).
- En el build #2, `docker compose down` detiene y elimina `zoo-app`, `zoo-mysql` y la red antes de
  volver a crearlos, sin errores de *container name already in use* ni de puerto ocupado.

```
$ docker ps
NAMES         IMAGE                      STATUS                    PORTS
zoo-app       zoo-fantastic-app:latest   Up 11 seconds             0.0.0.0:8081->8080/tcp
zoo-mysql     mysql:8.0                  Up 17 seconds (healthy)   0.0.0.0:3306->3306/tcp
jenkins-zoo   jenkins-zoo:lts-jdk21      Up 5 minutes              0.0.0.0:8080->8080/tcp, 0.0.0.0:8090->8090/tcp
```

### 3.4 Validación: una prueba fallida detiene el despliegue

Para comprobar el punto 2 de la *Validación del Pipeline* («si alguna prueba falla, el
pipeline se detendrá y no se ejecutará el despliegue») se creó una rama temporal
`demo/prueba-fallida` con una prueba que falla a propósito:

```java
@Test
void fallaIntencionalParaDemostrarElPipeline() {
    int capacidadEsperada = 50;
    int capacidadReal = 51;
    assertEquals(capacidadEsperada, capacidadReal,
            "Falla intencional: con una prueba en rojo el pipeline no debe desplegar");
}
```

Se apuntó el job a esa rama y se ejecutó el build #3 (`evidencias/12-…build3-prueba-fallida.txt`):

```
[ERROR] PipelineFallaDemoTest.fallaIntencionalParaDemostrarElPipeline:18 Falla intencional: con una
        prueba en rojo el pipeline no debe desplegar ==> expected: <50> but was: <51>
[ERROR] Tests run: 72, Failures: 1, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE
Stage "Docker Build" skipped due to earlier failure(s)
Stage "Docker Run" skipped due to earlier failure(s)
Stage "Smoke Test" skipped due to earlier failure(s)
Pipeline terminado.
Error en el pipeline. Revisar los logs.
Finished: FAILURE
```

| Etapa | Build #3 |
|---|---|
| Checkout SCM, Build | SUCCESS |
| Run Tests | **FAILED**: 71 pasaron, 1 falló (reporte JUnit en Jenkins) |
| Docker Build, Docker Run, Smoke Test | **No se ejecutaron** (*skipped due to earlier failure*) |
| post | `always` + `failure`: «Pipeline terminado.» / «Error en el pipeline. Revisar los logs.» |

El Stage View pinta esas tres etapas en rojo con la etiqueta *failed*, pero duraron entre 60 y
120 ms y la consola confirma que fueron omitidas: es la forma en que el plugin Stage View
muestra las etapas saltadas.

**La versión desplegada no se tocó.** El contenedor `zoo-app` tenía el mismo ID
(`da809ecec5a7…`) y la misma fecha de creación (18:32:11, del build #2) antes y después del
build #3, y `GET /api/zones` en el 8081 siguió respondiendo 200. Una prueba en rojo deja en
producción la última versión buena.

Al terminar, el job volvió a apuntar a `feature/jenkins-pipeline` y la rama temporal se eliminó.

## 4. Beneficios de usar contenedores en el pipeline

- **Mismo artefacto en todos lados**: la imagen que construye Jenkins es la misma que se
  probó a mano en `docker.md`. Lo que corre en el pipeline es lo que corre en el host.
- **Entorno reproducible**: Java 21, Maven y MySQL 8 vienen definidos en imágenes; no hay que
  instalarlos en el servidor ni depender de lo que tenga cada máquina.
- **Despliegue idempotente**: `down` + `up` deja siempre el mismo estado, sin procesos
  huérfanos como los del `java -jar &` del job Freestyle.
- **Aislamiento**: la app, la base de datos y Jenkins están en contenedores separados, con
  puertos y redes explícitos.
- **Caché de capas**: si el `pom.xml` no cambia, las dependencias no se vuelven a descargar
  (Docker Build pasó de 52 s a menos de 1 s).

## 5. Desafíos y soluciones

| Desafío | Solución |
|---|---|
| La imagen oficial de Jenkins no tiene Maven ni Docker, y usa Java 17. | Imagen propia basada en `lts-jdk21` con Maven y Docker CLI. |
| **Permisos de Docker**: el usuario `jenkins` (no root) no podía usar `/var/run/docker.sock`. | `group_add` con el GID del grupo `docker` del host. |
| Desde el contenedor de Jenkins, `localhost` es el propio contenedor, no el host. | `host.docker.internal` (vía `host-gateway`) para llegar a MySQL y a la app. |
| Jenkins y la app querían el puerto 8080. | Jenkins en 8080, app del pipeline en 8081, app del Freestyle en 8090. |
| Un Jenkins anterior (de otro curso) ocupaba el 8081 y su conexión a Docker estaba rota (apuntaba a un contenedor `docker:dind` que ya no existía). | Se detuvo (`docker stop`, sin borrar sus datos) y se creó uno nuevo, dedicado y reproducible desde el repositorio. |
| El `java -jar … &` moría al terminar el build. | `JENKINS_NODE_COOKIE=dontKillMe` / `BUILD_ID=dontKillMe` + `nohup`. |
| El pipeline de la guía falla en la segunda ejecución. | `docker compose down` antes de `up` (sección 3.1). |

## 6. Cambiar la rama al integrar

Los dos jobs apuntan a `*/feature/jenkins-pipeline`. Cuando el PR se fusione, cambiar
*Branch Specifier* a `*/develop` (o `*/main`) en **Configure → Source Code Management**.
