# Zoo Fantástico - Backend Spring Boot 3

Proyecto backend en Spring Boot 3 (Java 21, Maven) para la gestión del **Zoo Fantástico**.

---

## 🛠️ Tecnologías y Dependencias
- **Java 21**
- **Spring Boot 3.2.4**
- **Spring Web**
- **Spring Data JPA**
- **Validation**
- **MySQL Driver**
- **Lombok**
- **Docker / Docker Compose** (MySQL 8)

---

## 🌿 Convención de Ramas (Gitflow)

Seguimos una metodología basada en **Gitflow**:

- **`main`**: Rama de producción/estable. Solo recibe cambios mediante Pull Requests desde `develop`.
- **`develop`**: Rama principal de integración para nuevas características y correcciones.
- **`feature/<nombre>`**: Ramas temporales creadas a partir de `develop` para desarrollar una funcionalidad o tarea específica (ej: `feature/setup-infraestructura`, `feature/entidades-jpa`, `feature/crud-creatures`, `feature/crud-zones`).

---

## 📝 Convención de Commits (Conventional Commits)

Todos los commits deben seguir la especificación [Conventional Commits](https://www.conventionalcommits.org/):

`<tipo>(<ámbito opcional>): <descripción corta en presente o infinitivo>`

### Tipos comunes:
- **`feat`**: Una nueva funcionalidad para el usuario/sistema.
- **`fix`**: Corrección de un error o bug.
- **`chore`**: Tareas de mantenimiento, actualización de dependencias, configuración del proyecto.
- **`docs`**: Cambios únicamente en la documentación (ej. `README.md`).
- **`refactor`**: Reorganización o refactorización del código sin cambiar comportamiento.
- **`test`**: Añadir o corregir pruebas unitarias/integración.

### Ejemplos:
```bash
git commit -m "chore(infra): inicializar setup con Docker, Maven y Spring Boot"
git commit -m "feat(creature): agregar entidad JPA Creature"
git commit -m "docs(readme): añadir flujo de trabajo Gitflow y convenciones"
```

---

## 🔄 Flujo de Trabajo (Workflow)

1. **Crear rama de trabajo desde `develop`**:
   ```bash
   git checkout develop
   git pull origin develop
   git checkout -b feature/nombre-de-la-feature
   ```
2. **Realizar cambios y commits atómicos**:
   - Asegurarse de seguir la convención de commits.
   - Evitar commits gigantes.
3. **Push de la rama feature a GitHub**:
   ```bash
   git push -u origin feature/nombre-de-la-feature
   ```
4. **Abrir Pull Request (PR)**:
   - Crear un Pull Request asignando `develop` como la rama de destino (**base: develop** <- **compare: feature/nombre-de-la-feature**).
   - Realizar la revisión de código e integrar los cambios a `develop`.
5. **Cierre de Laboratorio**:
   - Al finalizar el desarrollo y verificar la estabilidad en `develop`, se abre un Pull Request final de `develop` hacia `main` (**base: main** <- **compare: develop**).
   - Asignar como revisor obligatorio al usuario de GitHub: **`soldash`**.

---

## 🚀 Inicio Rápido (Desarrollo con Docker)

1. Clonar el repositorio y copiar las variables de entorno:
   ```bash
   cp .env.example .env
   ```
2. Levantar la infraestructura (Base de Datos MySQL y Aplicación Spring Boot):
   ```bash
   docker compose up --build -d
   ```
3. Verificar logs de la aplicación:
   ```bash
   docker compose logs -f app
   ```

