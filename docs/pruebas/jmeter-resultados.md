# Resultados de la prueba de carga con JMeter – POST /api/creatures

Responsable: Juan Camilo Delgado (Parte 4). Fecha de ejecución: 2026-10-22.

## Diseño de la prueba

- **Plan:** `performance/jmeter/zoo-creatures.jmx` (JMeter 5.6.3, modo no-GUI).
- **setUp Thread Group:** `POST /api/zones` crea una zona `Zona de carga JMeter <fecha>` con capacidad 1 000 000 y publica su id en la propiedad `zoneId`, para que la regla de capacidad no genere 409 y sesgue los resultados.
- **Thread Group:** `${__P(users)}` hilos, `POST /api/creatures` con nombre único `Criatura-${__threadNum}-${__UUID()}` y datos válidos; aserción de código **201**.
- **Parámetros fijos en las tres corridas:** `rampup=1` s, `loops=50`; solo cambia `-Jusers` (10, 50, 100).
- **Dashboard:** `-q report.properties` → granularidad 1 s y solo la etiqueta `POST /api/creatures` (excluye la petición del setUp).
- **Entre corridas:** borrado selectivo de las zonas `Zona de carga JMeter%` y sus criaturas (verificado con `SELECT` previo); la BD vuelve siempre a las zonas N1/N2/N3 con 0/2/1 criaturas. Respaldos con `mysqldump` fuera del repo.
- **Entorno:** una sola máquina Windows 11 (JMeter, backend Spring Boot en `localhost:8081` y MySQL 8.0 en Docker). El puerto 8080 lo ocupa CVAT, que siguió en ejecución durante las pruebas; los valores absolutos dependen de este equipo y sirven sobre todo para comparar las tres cargas entre sí.

## Tabla comparativa (corridas oficiales, iteración 2)

| Usuarios | Hilos activos (prom. / máx.) | Muestras | Promedio | Mediana | p90 | p95 | p99 | Máximo | % error | Throughput |
|---|---|---|---|---|---|---|---|---|---|---|
| 10 | 6,9 / 10 | 500 | 23,6 ms | 24 ms | 28 ms | 30 ms | 37 ms | 41 ms | 0,00 % | 246,8 req/s |
| 50 | 43,7 / 50 | 2 500 | 113,8 ms | 118 ms | 145 ms | 166 ms | 195 ms | 277 ms | 0,00 % | 373,2 req/s |
| 100 | 93,5 / 100 | 5 000 | 235,6 ms | 241 ms | 278 ms | 293 ms | 451 ms | 811 ms | 0,00 % | 390,4 req/s |

Fuente: `statistics.json` del dashboard de cada corrida y columna `allThreads` de su `.jtl` (generados con los comandos de *Cómo reproducir*; no se versionan).

## Análisis del comportamiento bajo carga

1. **Saturación entre 50 y 100 usuarios.** De 10 a 50 usuarios el throughput sube un 51 % (246,8 → 373,2 req/s); de 50 a 100 solo un 4,6 % (373,2 → 390,4 req/s). El backend alcanza su techo en torno a **~390 req/s** de creación de criaturas en este equipo: duplicar los usuarios ya no aumenta el trabajo realizado.
2. **La latencia crece de forma casi lineal con los usuarios una vez saturado.** La mediana pasa de 24 a 118 ms (×4,9) y luego a 241 ms (×2,0, el mismo factor que los usuarios). Es el patrón de una cola: las peticiones adicionales esperan turno en lugar de procesarse en paralelo. Los números cumplen la ley de Little (usuarios ≈ throughput × tiempo de respuesta): 373,2 × 0,114 ≈ 42,5 y 390,4 × 0,236 ≈ 92, frente a 43,7 y 93,5 hilos activos medidos.
3. **Colas largas al inicio de la carga.** Con 100 usuarios, p99 (451 ms) y el máximo (811 ms) se separan mucho de la mediana; en *Response Time Percentiles Over Time* el pico está en el segundo 5, cuando los 100 hilos arrancan casi a la vez (ramp-up de 1 s) y se acumula la cola. En régimen (segundos 6–12) la mediana se estabiliza en 230–255 ms y el p95 en 240–285 ms.
4. **Sin errores.** Ninguna de las 8 000 peticiones oficiales falló (ver *Errores bajo carga*): el sistema degrada la latencia pero no rechaza peticiones ni pierde escrituras (cruce con la BD: 5 000 filas para 5 000 respuestas 201).
5. **Posible cuello de botella (hipótesis, no medida).** Cada creación hace `SELECT` de la zona, `COUNT` de sus criaturas e `INSERT`, dentro de una transacción. El pool de conexiones por defecto de Spring Boot (HikariCP, 10 conexiones) frente a 50–100 hilos concurrentes es un candidato natural a explicar la cola; confirmarlo requeriría métricas del pool o de MySQL, que no se recogieron en esta práctica.
6. **Para un uso interactivo**, con hasta 10 usuarios simultáneos la respuesta es inmediata (p95 = 30 ms); con 100 usuarios simultáneos creando criaturas sin pausa, el p95 de ~0,3 s sigue siendo aceptable, pero el sistema ya está al límite de su capacidad en este equipo.

## Iteración 1 descartada (`rampup=10`, `loops=10`)

La primera tanda no generaba concurrencia real (máximo 1, 2 y 3 hilos activos para 10, 50 y 100 usuarios), así que medía solo la tasa de llegada. Se explica en `hallazgos.md` (H3).

## Errores bajo carga

### Corridas oficiales (iteración 2: `rampup=1`, `loops=50`)

| Usuarios | Muestras | Respuestas 201 | Errores | % de error |
|---|---|---|---|---|
| 10 | 500 | 500 | 0 | 0,00 % |
| 50 | 2 500 | 2 500 | 0 | 0,00 % |
| 100 | 5 000 | 5 000 | 0 | 0,00 % |

La sección *Errors* y *Top 5 Errors by sampler* del dashboard de 100 usuarios aparecen vacías.

**Por qué no hubo errores.** El plan elimina deliberadamente las causas de error funcional para medir solo el rendimiento del endpoint:

- **Capacidad de zona:** el setUp crea una zona con capacidad 1 000 000, así que la regla "zona llena → 409" nunca se activa (como máximo se crean 5 000 criaturas).
- **Datos válidos:** `dangerLevel = 5` (rango 1–10), `size = 2.5` (≥ 0), todos los campos obligatorios presentes → ni 400 de Bean Validation ni 409 de `validateCreatureData`.
- **Nombres únicos:** `Criatura-${__threadNum}-${__UUID()}`; aunque el modelo no exige nombres únicos, así cada petición es trazable.
- **Sin saturación con fallo:** al llegar a su capacidad (~390 req/s) el backend encola peticiones y sube la latencia (media 236 ms, máx. 811 ms con 100 hilos) pero no rechaza conexiones ni devuelve 5xx; ninguna petición superó el *timeout* de respuesta de 30 s del plan.

**Cruce con la base de datos** (corrida de 100 usuarios, antes de la limpieza):

```sql
SELECT z.id, z.name, COUNT(c.id) AS criaturas, COUNT(DISTINCT c.name) AS nombres_distintos
FROM zones z LEFT JOIN creatures c ON c.zone_id = z.id
WHERE z.name LIKE 'Zona de carga JMeter%' GROUP BY z.id, z.name;
-- 12 | Zona de carga JMeter 20261022-093602 | 5000 | 5000
```

5 000 respuestas 201 en el `.jtl` = 5 000 filas persistidas con 5 000 nombres distintos: no hubo escrituras perdidas ni duplicadas.

### Corrida exploratoria: zona de capacidad 50 (fuera de la tabla oficial)

Objetivo: observar los errores de regla de negocio bajo carga y comprobar el hallazgo H2. Mismo plan, con la propiedad `capacity` (por defecto 1 000 000):

```
jmeter -n -t zoo-creatures.jmx -Jusers=100 -Jrampup=1 -Jloops=50 -Jcapacity=50 -q report.properties \
  -l results/explor_capacidad/res_100_cap50.jtl -e -o results/explor_capacidad/report_100_cap50
```

| Usuarios | Muestras | 201 | 409 | % de error | Media | Mediana | p95 | Máx. | Throughput |
|---|---|---|---|---|---|---|---|---|---|
| 100 (capacidad 50) | 5 000 | 56 | 4 944 | 98,88 % | 165,6 ms | 169 ms | 217 ms | 522 ms | 544,6 req/s |

- **Causa del error:** el 100 % de los errores son `409 Conflict` (tabla *Errors* del dashboard), producidos por `BusinessRuleException` cuando la zona alcanza su capacidad. Los datos enviados son válidos, así que no hay otra fuente de 409 en `createCreature`.
- **Cruce con la BD:** la zona (capacidad 50) terminó con **56 criaturas** → 6 por encima del máximo. Confirma la condición de carrera descrita en `hallazgos.md` (H2).
- El throughput es mayor y la latencia menor que en la corrida oficial de 100 usuarios porque las respuestas 409 no hacen `INSERT`; no es comparable con la tabla oficial.

## Cómo reproducir

Desde `performance/jmeter/`, con el backend en `localhost:8081` y limpiando las zonas `Zona de carga JMeter%` entre corridas:

```
jmeter -n -t zoo-creatures.jmx -Jusers=10  -Jrampup=1 -Jloops=50 -q report.properties -l results/res_10.jtl  -e -o results/report_10
jmeter -n -t zoo-creatures.jmx -Jusers=50  -Jrampup=1 -Jloops=50 -q report.properties -l results/res_50.jtl  -e -o results/report_50
jmeter -n -t zoo-creatures.jmx -Jusers=100 -Jrampup=1 -Jloops=50 -q report.properties -l results/res_100.jtl -e -o results/report_100
```

Si el backend corre en otro puerto: `-Jport=8080`. Los resultados (`results/`, `*.jtl`) están en `.gitignore` y se regeneran.
