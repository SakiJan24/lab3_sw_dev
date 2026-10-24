# Evidencias – Cuarta Práctica (Pruebas de CRUD)

Responsable: Juan Camilo Delgado (Parte 1: pruebas unitarias · Parte 4: JMeter).
Las imágenes se guardan en `docs/pruebas/img/`.
Los dashboards HTML y los `.jtl` de JMeter no se versionan (están en `.gitignore`); se regeneran con los comandos de `jmeter-resultados.md` (*Cómo reproducir*).

## Parte 1 – Pruebas unitarias

| N | Qué muestra | Archivo sugerido | Estado |
|---|---|---|---|
| 1 | Estructura de `src/test` y `CreatureService` en el IDE | `01_estructura_src_test.png` | Manual (la toma Juan Camilo) |
| 2 | El plan aprobado (conversación) | `02_plan_aprobado.png` | Manual (la toma Juan Camilo) |
| 3 | Test representativo de create: `CreatureServiceTest.testCreateCreature_ShouldReturnSavedCreature` (líneas 112–141): Arrange-Act-Assert, `ArgumentCaptor` y `verify` sobre los repositorios | `03_test_create_creature.png` | Tomada |
| 4 | Test de `ZoneServiceTest`: `testDeleteZone_WhenHasCreatures_ShouldThrowConflict` (líneas 240–270): borrar una zona con criaturas → `BusinessRuleException` (409) y `verify(zoneRepository, never()).delete(any())` | `04_test_zone_delete_con_criaturas.png` | Tomada |
| 5 | Test de "no eliminar criatura en estado crítico": `CreatureServiceTest.testDeleteCreature_WhenCritical_ShouldThrowConflict` (líneas 362–376), parametrizado con `critical`/`CRITICAL`/`Critical` → 409 y `verify(creatureRepository, never()).delete(any())` | `05_test_delete_critical.png` | Tomada |
| 6a | Tests de parámetros inválidos – `RequestValidationTest` (líneas 21–51): Javadoc que justifica el Validator de Jakarta, `Validation.buildDefaultValidatorFactory()` en `@BeforeAll` y helper `invalidFields` | `06a_validator_setup.png` | Tomada |
| 6b | Tests de parámetros inválidos – `RequestValidationTest` (líneas 94–136): `size` negativo (mensaje exacto de la anotación) y `dangerLevel` fuera de rango parametrizado (0, 11, -1, 100) | `06b_test_parametros_invalidos.png` | Tomada |
| 7 | Primera ejecución de `.\mvnw.cmd -q -B test`: falla al resolver `jacoco-maven-plugin:0.8.12` con `(certificate_unknown) PKIX path building failed` (el escaneo SSL de Norton intercepta el certificado de Maven Central) → `EXIT=1`. **Tomada de la sesión de Claude Code (evidencia real).** | `07_mvn_test_error_norton_ssl.png` | Tomada |
| 8a | Corrección (parte 1): diagnóstico de Norton y ejecución con `$env:MAVEN_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'` (Java usa el almacén de certificados de Windows, donde Norton registra su CA) → `Downloaded from central: ... jacoco-maven-plugin-0.8.12`. **Tomada de la sesión de Claude Code (evidencia real).** | `08a_correccion_windows_root.png` | Tomada |
| 8b | Corrección (parte 2): final de la misma ejecución → 64 tests, `jacoco:0.8.12:report`, `BUILD SUCCESS`, `EXIT=0`. **Tomada de la sesión de Claude Code (evidencia real).** | `08b_correccion_build_success.png` | Tomada |
| 9 | Ejecución final en la PowerShell de Juan Camilo: `Tests run: 64, Failures: 0, Errors: 0, Skipped: 0` y `BUILD SUCCESS` | `09_mvn_test_final.png` | Tomada |
| 10a | Reporte de Surefire: carpeta `target/surefire-reports` (5 `.xml` + 5 `.txt`) y el resumen `CreatureServiceTest.txt` (24 tests, 0 fallos) | `10a_surefire_reports.png` | Tomada |
| 10b | Reporte HTML de JaCoCo, paquete `service`: `CreatureService` 94 % instrucciones / 13 de 14 ramas; `ZoneService` 89 % / 4 de 4 ramas | `10b_jacoco_service.png` | Tomada |
| 10c | Reporte HTML de JaCoCo global (`index.html`): todos los paquetes, `service` 92 % y total del proyecto 77 % instrucciones / 72 % ramas (incluye los tests de integración de los compañeros) | `10c_jacoco_global.png` | Tomada |

> Nota: al intentar repetir el error en la PowerShell de Juan Camilo (sin `MAVEN_OPTS` y con la caché de JaCoCo borrada), la descarga funcionó sin error. El bloqueo de Norton es **intermitente**; por eso las capturas 7 y 8 se toman de la sesión donde ocurrió realmente y no se simularon.

## Parte 4 – JMeter

| N | Qué muestra | Archivo sugerido | Estado |
|---|---|---|---|
| 11 | JMeter 5.6.3 instalado (`C:\tools\apache-jmeter-5.6.3`): salida de `jmeter -v` en PowerShell y diálogo *Help → About* con la versión 5.6.3. **Misma imagen que la 12.** | `11_12_jmeter_instalado_y_plan.png` | Tomada |
| 12 | Árbol del plan expandido: HTTP Request Defaults, Headers JSON, *setUp – Crear zona de carga* (POST /api/zones, JSON Extractor `$.id`, JSR223 que publica `zoneId`, Assert 201), *Usuarios – Crear criaturas* (POST /api/creatures, Assert 201) y Listeners (Summary Report, Aggregate Report, Response Time Graph, View Results Tree deshabilitado). **Misma imagen que la 11.** | `11_12_jmeter_instalado_y_plan.png` | Tomada |
| 13 | Configuración del Thread Group *Usuarios – Crear criaturas*: `${__P(users,10)}` hilos, ramp-up `${__P(rampup,10)}` s, loops `${__P(loops,10)}`; *Continue* ante error. En las corridas solo cambia `-Jusers` (10/50/100); `rampup=10` y `loops=10` fijos | `13_jmeter_thread_group.png` | Tomada |
| 14a | Sampler *POST /api/creatures*: método POST, ruta `/api/creatures`, *Body Data* JSON con nombre único `Criatura-${__threadNum}-${__UUID()}` y `"zoneId": ${__P(zoneId)}` (id publicado por el setUp) | `14a_jmeter_sampler_body.png` | Tomada |
| 14b | Headers del plan (*Headers JSON*: `Content-Type` y `Accept` = `application/json`), compartidos por setUp y carga | `14b_jmeter_headers.png` | Tomada |
| 14c | *HTTP Request Defaults*: protocolo `http`, servidor `${__P(host,localhost)}`, puerto `${__P(port,8081)}` (8080 lo ocupa CVAT) | `14c_jmeter_http_defaults.png` | Tomada |
| 15a | Dashboard HTML de la corrida de **10 usuarios**, iteración 2 (`rampup=1`, `loops=50`; dashboard `report_10`): 500 muestras, 0 % error, media 23,6 ms, p95 30 ms, throughput 246,8 req/s; máx. 10 hilos activos | `15a_jmeter_10_usuarios.png` | Tomada |
| 15a2 | *Active Threads Over Time* de la corrida de 10 usuarios, granularidad 1 s (`-q report.properties`) | `15a2_jmeter_10_hilos_activos.png` | Tomada |
| 15b | Dashboard HTML de la corrida de **50 usuarios**, iteración 2 (dashboard `report_50`): 2 500 muestras, 0 % error, media 113,8 ms, p95 166 ms, throughput 373,2 req/s; máx. 50 hilos activos | `15b_jmeter_50_usuarios.png` | Tomada |
| 15b2 | *Active Threads Over Time* de la corrida de 50 usuarios, granularidad 1 s: meseta en 50 hilos | `15b2_jmeter_50_hilos_activos.png` | Tomada |
| 15c | Dashboard HTML de la corrida de **100 usuarios**, iteración 2 (dashboard `report_100`): 5 000 muestras, 0 % error, media 235,6 ms, p95 293 ms, máx. 811 ms, throughput 390,4 req/s; máx. 100 hilos activos | `15c_jmeter_100_usuarios.png` | Tomada |
| 15c2 | *Active Threads Over Time* de la corrida de 100 usuarios, granularidad 1 s: meseta de ~9 s en 100 hilos | `15c2_jmeter_100_hilos_activos.png` | Tomada |
| 16 | Dashboard de 100 usuarios, *Response Time Percentiles Over Time* (granularidad 1 s): subida de ~55 ms a ~270 ms en los primeros 4 s (ramp-up + cola), meseta de 230–255 ms con 100 hilos activos, pico máx. 811 ms en el segundo 5 | `16_jmeter_100_tiempos_respuesta.png` | Tomada |
| 16b | Listener *Response Time Graph* del plan en la GUI de JMeter (nodo dentro de *Usuarios – Crear criaturas*), leyendo `res_100.jtl` (iteración 2) con *Sampler label selection* = `POST /api/creatures` e intervalo 1000 ms: una sola serie, subida a ~280 ms, meseta 230–255 ms y caída final | `16b_jmeter_response_time_graph_gui.png` | Tomada |
| 17 | Errores bajo carga, corrida oficial de 100 usuarios (dashboard `report_100`, zoom 100 %): tabla *Statistics* con 5 000 muestras y 0,00 % de error. Causa del 0 % y cruce con la BD (5 000 criaturas, 5 000 nombres distintos) en `jmeter-resultados.md` | `17_jmeter_errores_0pct.png` | Tomada |
| 17_2 | Misma corrida: secciones *Errors* (cabeceras visibles, sin filas) y *Top 5 Errors by sampler* (Total: 5 000 muestras, 0 errores) | `17_2_jmeter_errores_tablas_vacias.png` | Tomada |
| 17b | Corrida **exploratoria** (fuera de la tabla oficial): 100 usuarios contra zona de **capacidad 50** (dashboard `report_100_cap50`): 5 000 muestras, 4 944 × 409 (98,88 %), tabla *Errors* con la fila `409`. Cruce con BD: 56 criaturas en una zona de capacidad 50 → confirma H2 | `17b_jmeter_explor_capacidad_409.png` | Tomada |
| 18 | Tabla comparativa final y análisis en `docs/pruebas/jmeter-resultados.md` (vista previa de Markdown): usuarios, hilos activos, muestras, promedio, mediana, p90/p95/p99, máximo, % de error y throughput de las corridas oficiales 10/50/100 | `18_tabla_comparativa_jmeter.png` | Pendiente |

> Iteración 1 (`rampup=10`, `loops=10`) descartada por falta de concurrencia real (ver `jmeter-resultados.md`, *Problemas encontrados durante la prueba*).
