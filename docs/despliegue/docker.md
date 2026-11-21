# Despliegue con Docker – Zoo Fantástico

Parte 5 (paso 1: Dockerfile, construir y correr la imagen) y Parte 6 (pruebas dentro del
pipeline) de la práctica de despliegue con Docker y Jenkins.

| Elemento | Valor |
|---|---|
| Aplicación | Spring Boot 3.2.4, Java 21 |
| Imagen | `zoo-fantastic-app:latest` |
| Base de datos | MySQL 8.0 (contenedor `zoo-mysql`, volumen `zoo-fantastico_mysql_data`) |
| Puerto publicado | `8081` en el host → `8080` dentro del contenedor |
| Jenkins | puerto `8080` (por eso la app usa `8081`) |

## 1. Dockerfile explicado línea por línea

```dockerfile
# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Etapa 1: compilación (`build`)**

| Línea | Qué hace |
|---|---|
| `FROM maven:3.9.6-eclipse-temurin-21-alpine AS build` | Parte de una imagen que ya trae Maven 3.9.6 y el JDK 21 (Temurin) sobre Alpine. `AS build` le da nombre a la etapa para poder copiar desde ella después. |
| `WORKDIR /app` | Crea y se ubica en `/app`; todas las rutas siguientes son relativas a esa carpeta. |
| `COPY pom.xml .` | Copia **solo** el `pom.xml`. Así la capa de dependencias depende únicamente del pom. |
| `RUN mvn dependency:go-offline -B` | Descarga todas las dependencias y plugins. Esta capa queda en caché: mientras el `pom.xml` no cambie, los builds siguientes no vuelven a descargar nada. `-B` = modo batch (sin colores ni barras de progreso, ideal para logs de CI). |
| `COPY src ./src` | Copia el código fuente. Cambiar código solo invalida desde esta capa en adelante. |
| `RUN mvn clean package -DskipTests` | Compila y empaqueta el JAR ejecutable (spring-boot-maven-plugin lo reempaqueta con sus dependencias). Las pruebas se omiten aquí porque se ejecutan en una etapa propia del pipeline (`mvn test`); así no se corren dos veces ni se necesita base de datos durante el build. |

**Etapa 2: ejecución (runtime)**

| Línea | Qué hace |
|---|---|
| `FROM eclipse-temurin:21-jre-alpine` | Nueva imagen base, mucho más liviana: solo el **JRE** 21, sin JDK, sin Maven, sin código fuente. Todo lo de la etapa 1 se descarta salvo lo que se copie explícitamente. |
| `WORKDIR /app` | Carpeta de trabajo de la imagen final. |
| `RUN addgroup -S spring && adduser -S spring -G spring` | Crea un grupo y un usuario de sistema (`-S`) llamados `spring`. |
| `USER spring:spring` | A partir de aquí todo corre con ese usuario, no como `root`. Si alguien explotara una vulnerabilidad de la app, no tendría privilegios de root dentro del contenedor. |
| `COPY --from=build /app/target/*.jar app.jar` | Trae de la etapa `build` únicamente el JAR generado y lo renombra a `app.jar` (el `.jar.original` no coincide con el patrón, así que solo se copia uno). |
| `EXPOSE 8080` | Documenta que la app escucha en el 8080 dentro del contenedor (`server.port=${PORT:8080}`). No publica el puerto; eso lo hace `-p` o `ports:` en Compose. |
| `ENTRYPOINT ["java", "-jar", "app.jar"]` | Comando que se ejecuta al iniciar el contenedor. Se usa la forma JSON (exec) para que `java` sea el PID 1 y reciba las señales de `docker stop` y se apague limpiamente. |

**`.dockerignore`**: excluye `.git`, `target`, `zoo-fantastico-frontend`, `performance`,
`docs`, los `*.txt` de evidencias, `.env` y archivos de IDE. El contexto que se envía a Docker
pasó a ser de ~90 kB, y no se copian archivos sensibles (`.env`) ni innecesarios.

## 2. ¿Por qué multi-etapa con Java 21 y no `openjdk:11` como en la guía?

- **Java 21 es obligatorio, no opcional.** El proyecto usa Spring Boot 3.2.4, que exige Java 17
  como mínimo, y el `pom.xml` declara `<java.version>21</java.version>`. Un JAR compilado para
  Java 21 no arranca en un JRE 11 (`UnsupportedClassVersionError`).
- **La imagen `openjdk` está descontinuada.** Docker Hub marcó `openjdk` como deprecada y dejó
  de publicar actualizaciones; `eclipse-temurin` (Adoptium) es el reemplazo mantenido y con
  parches de seguridad.
- **Build reproducible.** La guía asume que el JAR ya se compiló en la máquina (`COPY target/*.jar`).
  Con multi-etapa la imagen compila el código ella misma con una versión fija de Maven y JDK:
  da igual qué Java tenga instalado el desarrollador o el agente de Jenkins.
- **Imagen final más pequeña y segura.** La etapa final solo lleva JRE + JAR (sin JDK, Maven,
  código fuente ni caché de `.m2`). La imagen pesa ~387 MB en disco (~120 MB comprimida) y
  corre con un usuario sin privilegios.
- **Caché de dependencias.** Separar `pom.xml` de `src` hace que, si solo cambia código, el
  build reutilice la capa de dependencias y tarde segundos.

## 3. Cambios hechos a `docker-compose.yml`

| Cambio | Por qué |
|---|---|
| `version: '3.8'` → `name: zoo-fantastico` | `version` está obsoleto en Compose v2 (solo genera un warning). `name` fija el nombre del proyecto: sin él, Compose usa el nombre de la carpeta (en Jenkins, el del workspace) y, como los servicios tienen `container_name` fijo, una copia en otra carpeta choca con *"container name already in use"*. |
| `image: zoo-fantastic-app:latest` en el servicio `app` | Con `image` junto a `build`, Compose construye y etiqueta la misma imagen que `docker build -t zoo-fantastic-app:latest .`, en lugar de inventar `zoo-fantastico-app`. |
| `${APP_PORT:-8080}` → `${APP_PORT:-8081}` (y `.env.example`) | El 8080 del host lo usa Jenkins; si alguien olvida definir `APP_PORT`, la app igual queda en 8081 y no choca. |

## 4. Comandos usados (Windows, PowerShell)

```powershell
# Verificar que Docker esté corriendo
docker info

# Construir la imagen
docker build -t zoo-fantastic-app:latest .
docker images | findstr zoo

# Levantar app + MySQL en el puerto 8081
$env:APP_PORT=8081; docker compose up -d --build
docker ps
docker logs zoo-app            # buscar "Started ZooFantasticoApplication"
Invoke-WebRequest http://localhost:8081/api/zones
# Navegador: http://localhost:8081/api/zones

# Ciclo que hará Jenkins (se probó dos veces seguidas)
docker compose build
docker compose down
$env:APP_PORT=8081; docker compose up -d

# Pruebas (H2 en memoria, no requieren los contenedores)
.\mvnw.cmd test

# Apagar todo (los datos quedan en el volumen)
docker compose down
```

## 5. Resultados

| Paso | Resultado | Evidencia |
|---|---|---|
| `docker build` | BUILD SUCCESS, sin problemas de certificados SSL | `evidencias/01-docker-build.txt` |
| `docker images` | `zoo-fantastic-app:latest`, 387 MB | `evidencias/02-docker-images.txt` |
| `docker compose up -d --build` | `zoo-mysql` healthy → `zoo-app` iniciado | `evidencias/03-compose-up.txt` |
| `docker ps` | `zoo-app` en `0.0.0.0:8081->8080/tcp` | `evidencias/04-docker-ps.txt` |
| `docker logs zoo-app` | `Started ZooFantasticoApplication in ~4.5 seconds` | `evidencias/05-logs-zoo-app.txt` |
| `Invoke-WebRequest .../api/zones` | HTTP 200, JSON con las zonas | `evidencias/06-invoke-webrequest.txt` |
| Ciclo build → down → up ×2 | Ambas ejecuciones OK, HTTP 200 | `evidencias/07-ciclo-down-up.txt` |
| `.\mvnw.cmd test` | **71 pruebas, 0 fallos, 0 errores** | `evidencias/08-mvnw-test.txt` |

### Pruebas y versión de Surefire

`spring-boot-starter-parent` 3.2.4 ya gestiona `maven-surefire-plugin` **3.1.2** (visible en el
log como `surefire:3.1.2:test`), que soporta JUnit 5 de forma nativa, incluidas las pruebas
parametrizadas, y Java 21. Por eso no se agrega la versión 2.22.2 de la guía: sería un
downgrade sin beneficio. Los 51 métodos `@Test`/`@ParameterizedTest` se expanden a 71 casos.

## 6. Etapas sugeridas para el Jenkinsfile

```groovy
stage('Test') {
    steps {
        sh './mvnw test'                 // en agente Windows: bat 'mvnw.cmd test'
    }
    post {
        always { junit 'target/surefire-reports/*.xml' }
    }
}

stage('Docker Build') {
    steps {
        sh 'docker compose build'
    }
}

stage('Docker Run') {
    steps {
        sh 'docker compose down'
        sh 'APP_PORT=8081 docker compose up -d'
    }
}
```

En un agente Windows, la etapa *Docker Run* queda:

```groovy
bat 'docker compose down'
bat 'set APP_PORT=8081&& docker compose up -d'
```

- `docker compose down` antes del `up` garantiza que una segunda ejecución del pipeline no
  falle por contenedores existentes. Si no hay nada corriendo, `down` termina sin error.
- **No** usar `docker compose down -v` en el pipeline: borraría el volumen con los datos de MySQL.
- Gracias a `name: zoo-fantastico`, el nombre del workspace de Jenkins no importa.

### Nota: Jenkins necesita acceso a Docker

El usuario con el que corre Jenkins debe poder ejecutar `docker`:

- **Jenkins instalado en Linux:** agregar el usuario al grupo `docker`
  (`sudo usermod -aG docker jenkins`) y reiniciar Jenkins.
- **Jenkins en un contenedor:** montar el socket del host
  (`-v /var/run/docker.sock:/var/run/docker.sock`) e instalar el cliente de Docker (con el
  plugin compose) dentro de la imagen de Jenkins.
- **Jenkins en Windows con Docker Desktop:** Docker Desktop debe estar abierto y el servicio de
  Jenkins debe correr con un usuario que pertenezca al grupo `docker-users` (no como
  `Local System`).

Se puede verificar con una etapa que ejecute `docker version` y `docker compose version`.
