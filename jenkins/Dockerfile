# Imagen de Jenkins para el laboratorio de despliegue de Zoo Fantástico.
#
# Parte de la imagen oficial LTS con Java 21 (el mismo que usa el proyecto) y agrega:
#   - Maven, para que los jobs puedan ejecutar `mvn clean package` / `mvn test`.
#   - Docker CLI + plugin Compose, para que el pipeline construya y levante los
#     contenedores usando el Docker del host (se monta /var/run/docker.sock).
#   - curl y procps, usados por los pasos de verificación y despliegue.
#   - Los plugins listados en plugins.txt, preinstalados.
FROM jenkins/jenkins:lts-jdk21

USER root

RUN apt-get update \
 && apt-get install -y --no-install-recommends maven curl procps ca-certificates gnupg lsb-release \
 && install -m 0755 -d /etc/apt/keyrings \
 && curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc \
 && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
      > /etc/apt/sources.list.d/docker.list \
 && apt-get update \
 && apt-get install -y --no-install-recommends docker-ce-cli docker-compose-plugin \
 && rm -rf /var/lib/apt/lists/*

USER jenkins

# Sin asistente de instalación: el usuario admin lo crea init.groovy.d/01-admin.groovy
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"

COPY --chown=jenkins:jenkins plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

COPY --chown=jenkins:jenkins init.groovy.d/ /usr/share/jenkins/ref/init.groovy.d/
