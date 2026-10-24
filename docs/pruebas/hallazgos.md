# Hallazgos durante las pruebas (Cuarta Práctica)

Observaciones detectadas al escribir las pruebas unitarias y el plan de carga. **No se corrigió el código de producción**; las pruebas documentan el comportamiento actual.

## H1. Una misma regla de validación responde con códigos HTTP distintos (400 vs 409)

- **Dónde:** `CreatureRequest` (anotaciones `@Min(0)` en `size`, `@Min(1)`/`@Max(10)` en `dangerLevel`) y `CreatureService.validateCreatureData()`.
- **Qué pasa:** la regla "size ≥ 0 y dangerLevel entre 1 y 10" está duplicada:
  - Vía HTTP, `@Valid` en `CreatureController` la rechaza antes de llegar al servicio → `MethodArgumentNotValidException` → **400 Bad Request**.
  - Si el servicio se invoca sin pasar por el controlador (otro servicio, un job, un test), la valida `validateCreatureData()` y lanza `BusinessRuleException` → **409 Conflict**.
- **Impacto:** el contrato de error depende del punto de entrada. Un dato mal formado es un error del cliente (400), no un conflicto con el estado del recurso (409).
- **Evidencia en pruebas:** `RequestValidationTest` (camino 400) y `CreatureServiceTest.testCreateCreature_WhenSizeIsNegative_...` / `testCreateCreature_WhenDangerLevelOutOfRange_...` (camino 409).
- **Sugerencia:** dejar la validación de formato solo en Bean Validation, o lanzar desde el servicio una excepción que se mapee a 400.

## H2. La validación de capacidad de zona no es segura ante concurrencia

- **Dónde:** `CreatureService.validateZoneCapacityForNewCreature()` (llamado desde `createCreature` y `updateCreature`).
- **Qué pasa:** primero cuenta (`countByZone`) y después guarda (`save`) sin bloqueo ni restricción en BD. Con la transacción por defecto de MySQL (REPEATABLE READ), dos peticiones concurrentes pueden leer el mismo conteo `capacity - 1` y ambas insertar → la zona queda **por encima de su capacidad máxima**.
- **Impacto:** la regla de negocio "zona llena → 409" puede violarse bajo carga (precisamente el escenario de JMeter).
- **Mitigación en la prueba de carga:** el plan de JMeter crea en el setUp una zona con capacidad muy alta para que la regla no sesgue los resultados; por tanto las corridas oficiales **no** ejercen este caso.
- **Confirmación experimental (corrida exploratoria, fuera de la tabla oficial):** `-Jusers=100 -Jrampup=1 -Jloops=50 -Jcapacity=50` (comando completo y resultados en `jmeter-resultados.md`, *Corrida exploratoria*).

  | Medida | Valor |
  |---|---|
  | Peticiones `POST /api/creatures` | 5 000 (máx. 100 hilos activos) |
  | Respuestas 201 | 56 |
  | Respuestas 409 | 4 944 (98,88 %) |
  | Capacidad de la zona | **50** |
  | Criaturas persistidas en la zona (BD) | **56** (56 nombres distintos) |

  La zona quedó con **6 criaturas por encima de su capacidad máxima** (+12 %). Las 56 respuestas 201 coinciden con las 56 filas de la BD, así que no es un error de conteo de JMeter: el servidor aceptó y guardó 6 inserciones que la regla de negocio debía rechazar. Todas las 201 llegaron en los primeros ~180 ms (arranque simultáneo de los 100 hilos); a partir de ahí todas las peticiones recibieron 409.
- **Conclusión:** el hallazgo H2 queda **confirmado con datos**: la regla "zona llena → 409" se cumple en régimen (98,88 % de 409 una vez llena), pero **no es atómica** y se viola bajo concurrencia en el momento en que varias peticiones leen el mismo conteo antes de que las demás confirmen su inserción. El resultado depende de la temporización y puede variar entre ejecuciones (no se forzó ni se repitió para buscar un número concreto).
- **Sugerencia:** bloqueo pesimista de la zona (`@Lock(PESSIMISTIC_WRITE)` al leerla), bloqueo optimista con `@Version` en `Zone`, o un contador con restricción en BD.

## H3. El diseño inicial de la prueba de carga no generaba concurrencia real (rampup=10)

- **Dónde:** `performance/jmeter/zoo-creatures.jmx`, parámetros de la primera iteración: `-Jrampup=10 -Jloops=10` con `-Jusers` = 10, 50 y 100.
- **Qué pasaba:** con un ramp-up de 10 s JMeter arranca un hilo nuevo cada `10 s / users` (cada 100 ms con 100 usuarios). Cada hilo hacía sus 10 peticiones en ~200 ms y terminaba antes de que arrancaran los siguientes, así que los hilos casi nunca se solapaban. Calculado desde la columna `allThreads` de los `.jtl`:

  | Corrida (iteración 1) | Hilos activos promedio | Hilos activos máximo |
  |---|---|---|
  | 10 usuarios | 1,0 | 1 |
  | 50 usuarios | 1,2 | 2 |
  | 100 usuarios | 2,0 | 3 |

- **Síntoma:** el throughput solo reflejaba la tasa de llegada (10, 50 y 100 req/s ≈ usuarios/ramp-up) y la latencia **bajaba** al aumentar los "usuarios" (24 → 22 → 19 ms) por calentamiento de la JVM, con 0 % de error. Los resultados no medían el comportamiento bajo carga concurrente.
- **Corrección:** se repitieron las tres corridas con `-Jrampup=1 -Jloops=50` (iguales en las tres; solo cambia `-Jusers`) y se verificó la simultaneidad tras cada corrida:

  | Corrida (iteración 2) | Peticiones | Hilos activos promedio | Hilos activos máximo |
  |---|---|---|---|
  | 10 usuarios | 500 | 6,9 | 10 |
  | 50 usuarios | 2 500 | 43,7 | 50 |
  | 100 usuarios | 5 000 | 93,5 | 100 |

- **Evidencia conservada:** los resultados de la iteración 1 no se borraron, pero no se versionan (los resultados de JMeter están en `.gitignore`); se regeneran con `-Jrampup=10 -Jloops=10`.
- **Lección:** el número de hilos configurado no es la concurrencia efectiva; hay que comprobarla (columna `allThreads` del `.jtl` o gráfico *Active Threads Over Time* del dashboard) y dimensionar `rampup` y `loops` para que los hilos se solapen.

## H4. El estado "critical" se compara sin distinguir mayúsculas y `healthStatus` acepta cualquier texto

- **Dónde:** `CreatureService.deleteCreature()` (`"critical".equalsIgnoreCase(creature.getHealthStatus())`) y `CreatureRequest.healthStatus`, que solo tiene `@NotBlank`.
- **Qué pasa:** `healthStatus` es un `String` libre: no hay `enum`, `@Pattern` ni lista de valores permitidos, así que se guarda cualquier texto no vacío (`critical`, `CRITICAL`, `Critical`, `crítico`, `xyz`…). La regla "no eliminar una criatura en estado crítico" solo reconoce variantes de mayúsculas de `critical`; un valor como `crítico` o `critical ` (con espacio) no la activa.
- **Impacto:** la protección contra borrado depende de cómo se escribió el estado al crear la criatura, y los datos pueden quedar con el mismo estado escrito de formas distintas.
- **Evidencia en pruebas:** `CreatureServiceTest.testDeleteCreature_WhenCritical_ShouldThrowConflict`, parametrizado con `critical`, `CRITICAL` y `Critical` (las tres → 409). Documenta el comportamiento actual; **no se corrigió**.
- **Sugerencia:** modelar `healthStatus` como `enum` (o validarlo con `@Pattern`/lista cerrada) y normalizarlo al guardar.
