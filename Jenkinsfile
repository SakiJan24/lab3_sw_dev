// Pipeline de CI/CD de Zoo Fantástico (Quinta práctica, Parte 5 paso 2 y paso 7).
//
// Build -> Run Tests -> Docker Build -> Docker Run -> Smoke Test
// Si una etapa falla, las siguientes no se ejecutan: con pruebas en rojo no se despliega.
pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()          // dos builds a la vez pelearían por los mismos contenedores
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        IMAGE    = 'zoo-fantastic-app:latest'
        APP_PORT = '8081'                   // 8080 lo usa Jenkins
        // Desde el contenedor de Jenkins, el host (donde Compose publica la app) es host.docker.internal
        APP_URL  = "http://host.docker.internal:${APP_PORT}"
    }

    stages {
        stage('Build') {
            steps {
                // Compila y empaqueta el JAR. Las pruebas corren en su propia etapa.
                sh 'mvn -B clean package -DskipTests'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('Run Tests') {
            steps {
                // Unitarias + integración (perfil test con H2 en memoria: no necesita MySQL)
                sh 'mvn -B test'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: false
                }
            }
        }

        stage('Docker Build') {
            steps {
                // Construye zoo-fantastic-app:latest con el Dockerfile multi-etapa (Java 21)
                sh 'docker compose build app'
                sh 'docker image ls zoo-fantastic-app'
            }
        }

        stage('Docker Run') {
            steps {
                // down antes de up: cada ejecución parte limpia y no choca con contenedores/puertos
                // de la corrida anterior. El volumen de MySQL se conserva (no se usa -v).
                sh 'docker compose down --remove-orphans'
                sh 'docker compose up -d'
                sh 'docker compose ps'
            }
        }

        stage('Smoke Test') {
            steps {
                // Espera (máx. ~2 min) a que la API responda 200 en el puerto publicado
                sh '''
                    for i in $(seq 1 40); do
                        code=$(curl -s -o /dev/null -w '%{http_code}' "$APP_URL/api/zones" || true)
                        if [ "$code" = "200" ]; then
                            echo "API arriba: GET /api/zones -> $code"
                            curl -s "$APP_URL/api/zones"; echo
                            exit 0
                        fi
                        echo "Esperando la app (intento $i, HTTP $code)..."
                        sleep 3
                    done
                    echo "La app no respondió a tiempo"
                    docker compose logs --tail=80 app
                    exit 1
                '''
            }
        }
    }

    post {
        always {
            echo 'Pipeline terminado.'
        }
        success {
            echo "¡Despliegue exitoso! App en http://localhost:${APP_PORT}/api/zones"
        }
        failure {
            echo 'Error en el pipeline. Revisar los logs.'
        }
    }
}
