# Documentación Completa del Simulador de Sistema Operativo

## Tabla de Contenidos

1. [Arquitectura del Sistema](#arquitectura-del-sistema)
2. [Requerimientos Atendidos](#requerimientos-atendidos)
3. [Funcionamiento Detallado](#funcionamiento-detallado)
4. [Componentes Técnicos](#componentes-técnicos)
5. [Flujos de Datos](#flujos-de-datos)

---

## Arquitectura del Sistema

### Visión General

El sistema está diseñado con una arquitectura de **tres capas** que permite la simulación educativa de un sistema operativo completo:

```
┌─────────────────────────────────────────────────────────────┐
│                    CAPA DE PRESENTACIÓN                      │
├─────────────────────────────────────────────────────────────┤
│  Frontend Vue.js (Panel de Control)                         │
│  + Aplicación Web Paralela (E-Commerce)                     │
└──────────────────────┬──────────────────────────────────────┘
                       │ HTTP/REST API
┌──────────────────────▼──────────────────────────────────────┐
│                    CAPA DE LÓGICA                           │
├─────────────────────────────────────────────────────────────┤
│  Backend Spring Boot                                        │
│  - Controladores REST                                       │
│  - Servicios de Negocio                                     │
│  - Gestión de Estado                                        │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│                    CAPA DE DATOS                            │
├─────────────────────────────────────────────────────────────┤
│  Estructuras en Memoria                                     │
│  - Tabla de Procesos (PCBs)                                │
│  - Colas de Procesos (Ready, Waiting, Terminated)          │
│  - Gestión de Memoria (Bloques)                             │
│  - Timeline de Ejecución                                    │
└─────────────────────────────────────────────────────────────┘
```

### Componentes Principales

#### 1. **Frontend - Panel de Control (Vue.js 3 + Vite)**

**Tecnologías:**
- Vue.js 3 (Composition API)
- Vite (Build tool)
- CSS3 Responsive

**Componentes:**
- `App.vue`: Componente principal que orquesta toda la aplicación
- `ProcessForm.vue`: Formulario para crear nuevos procesos
- `ProcessTable.vue`: Tabla que muestra todos los PCBs con colores únicos
- `ControlPanel.vue`: Panel de control de simulación (algoritmos, quantum, I/O)
- `InterruptionPanel.vue`: Panel para emitir interrupciones manuales
- `QueuesPanel.vue`: Visualización de colas del sistema (Ready, Waiting, Terminated)
- `TimelineChart.vue`: Diagrama de Gantt con colores sincronizados con la tabla
- `MemoryPanel.vue`: Gestión y visualización de memoria

**Características:**
- Diseño completamente responsive (móviles, tablets, desktop)
- Actualización en tiempo real cada 500ms
- Sincronización de colores entre tabla y timeline
- Interfaz intuitiva y moderna

#### 2. **Backend - API REST (Spring Boot 3.3 + Java 17)**

**Arquitectura:**
```
com.edu.ossimulator/
├── config/
│   └── WebConfig.java          # Configuración CORS
├── controller/                 # Capa de Controladores REST
│   ├── ProcessController.java
│   ├── SimulationController.java
│   ├── InterruptionController.java
│   └── MemoryController.java
├── service/                    # Capa de Servicios (Lógica de Negocio)
│   ├── ProcessSchedulerService.java
│   └── MemoryManagerService.java
├── model/                      # Modelos de Dominio
│   ├── ProcessControlBlock.java
│   ├── ProcessState.java
│   ├── SchedulerAlgorithm.java
│   ├── MemoryAllocationAlgorithm.java
│   └── MemoryBlock.java
└── dto/                        # Data Transfer Objects
    ├── CreateProcessRequest.java
    ├── SimulationRequest.java
    └── SystemStateResponse.java
```

**Servicios Principales:**

**ProcessSchedulerService:**
- Gestiona la tabla de procesos (PCBs)
- Mantiene las colas: Ready, Waiting, Terminated
- Implementa 4 algoritmos de planificación
- Maneja el timeline de ejecución
- Gestiona interrupciones y cambios de estado
- Soporta modo automático y manual

**MemoryManagerService:**
- Gestiona la asignación de memoria
- Implementa 4 algoritmos de asignación
- Calcula fragmentación interna y externa
- Fusiona bloques libres adyacentes
- Libera memoria automáticamente al terminar procesos

#### 3. **Aplicación Web Paralela (E-Commerce)**

**Propósito:**
Simular una aplicación web real donde cada acción del usuario genera un proceso que es monitoreado por el sistema operativo.

**Tecnologías:**
- HTML5, CSS3, JavaScript vanilla
- Fetch API para comunicación con backend

**Procesos Generados:**
- `Render-Page`: Carga inicial de la página
- `Load-Product`: Visualización de producto
- `Search-Query`: Búsqueda de productos
- `Add-To-Cart`: Agregar al carrito
- `Remove-From-Cart`: Eliminar del carrito
- `Submit-Order`: Procesar pedido
- `Background-Sync`: Sincronización en segundo plano
- `Cache-Update`: Actualización de caché
- `Analytics-Collect`: Recolección de analytics

---

## Requerimientos Atendidos

### 1. **Gestión de Procesos (PCB)**

**Implementado:**
- Estructura completa de Process Control Block (PCB)
- Campos: PID, nombre, estado, tiempo de llegada, ráfaga CPU, tiempo restante, prioridad
- Historial de eventos del proceso
- Información de memoria (dirección y tamaño)
- Timestamp de creación

**Funcionalidades:**
- Creación de procesos manual y automática
- Asignación automática de PID secuencial
- Actualización de nombre con PID real
- Visualización en tabla con colores únicos por PID

### 2. **Estados de Procesos**

**Estados Implementados:**
- `NEW`: Proceso recién creado
- `READY`: Listo para ejecutarse
- `RUNNING`: En ejecución
- `WAITING`: Esperando I/O o recurso
- `TERMINATED`: Finalizado

**Transiciones:**
- NEW → READY: Automático al crear
- READY → RUNNING: Por el planificador
- RUNNING → READY: Expiración de quantum o preemption
- RUNNING → WAITING: Interrupción I/O
- WAITING → READY: Fin de I/O
- RUNNING → TERMINATED: Fin de ráfaga

### 3. **Colas de Procesos**

**Colas Implementadas:**
- **Ready Queue**: Procesos listos para ejecutarse
- **Waiting Queue**: Procesos esperando I/O
- **Terminated Queue**: Procesos finalizados

**Visualización:**
- Panel dedicado con las tres colas
- Actualización en tiempo real
- Scroll para listas largas

### 4. **Algoritmos de Planificación**

**4 Algoritmos Implementados:**

1. **FCFS (First Come First Served)**
   - Ejecuta procesos en orden de llegada
   - No preemptivo
   - Simple y justo

2. **Round Robin**
   - Quantum configurable (1-10 unidades)
   - Preemptivo
   - Tiempo de respuesta mejorado
   - Evita inanición

3. **Prioridad**
   - Ejecuta por prioridad (0-10, mayor = más prioridad)
   - Preemptivo
   - Útil para sistemas en tiempo real

4. **SJF (Shortest Job First)**
   - Ejecuta el proceso con menor ráfaga primero
   - No preemptivo
   - Minimiza tiempo de espera promedio

### 5. **Gestión de Memoria**

**4 Algoritmos de Asignación:**

1. **First Fit**
   - Asigna el primer bloque libre que quepa
   - Rápido pero puede causar fragmentación

2. **Best Fit**
   - Asigna el bloque libre más pequeño que quepa
   - Reduce fragmentación interna
   - Más lento (busca todo)

3. **Worst Fit**
   - Asigna el bloque libre más grande
   - Deja bloques grandes para futuras asignaciones
   - Puede aumentar fragmentación

4. **Segmentación**
   - Permite múltiples segmentos por proceso
   - Más flexible
   - Simula memoria segmentada

**Características:**
- Visualización gráfica de bloques de memoria
- Cálculo de fragmentación interna y externa
- Asignación automática al crear procesos
- Liberación automática al terminar procesos
- Fusión automática de bloques libres adyacentes
- Estadísticas en tiempo real

### 6. **Interrupciones**

**Tipos de Interrupciones:**
- `IO`: Interrupción de entrada/salida
- `QUANTUM_EXPIRED`: Expiración de quantum (Round Robin)
- `MANUAL_STOP`: Detención manual
- `MANUAL_PAUSE`: Pausa manual

**Funcionalidades:**
- Emisión manual de interrupciones
- I/O automático configurable (probabilidad y duración)
- Cambio de estado automático por interrupciones
- Historial de interrupciones en el PCB

### 7. **Timeline / Diagrama de Gantt**

**Características:**
- Visualización de ejecución de procesos en el tiempo
- Colores sincronizados con la tabla de procesos
- Muestra algoritmo usado en cada slice
- Tiempo de inicio y fin de cada ejecución
- Responsive con scroll horizontal

### 8. **Modo Automático y Manual**

**Modo Automático:**
- Los procesos se ejecutan automáticamente al crearse
- No requiere intervención manual
- Ideal para demostraciones

**Modo Manual:**
- Control total sobre la simulación
- Iniciar, pausar, reanudar, detener
- Control de algoritmos y parámetros

### 9. **Integración Externa**

**API REST Completa:**
- Endpoints para todas las operaciones
- CORS configurado para desarrollo
- JSON como formato de intercambio
- Validación de datos de entrada

**Aplicación Web Paralela:**
- Genera procesos en tiempo real
- Simula aplicación web real
- Integración transparente con el sistema

### 10. **Interfaz de Usuario**

**Características:**
- Diseño moderno y profesional
- Completamente responsive
- Actualización en tiempo real
- Colores consistentes entre componentes
- Feedback visual inmediato
- Manejo de errores amigable

---

## Funcionamiento Detallado

### Flujo de Creación de Proceso

```
1. Usuario crea proceso (Frontend o Aplicación Web)
   ↓
2. POST /api/processes con datos del proceso
   ↓
3. ProcessController recibe la petición
   ↓
4. ProcessSchedulerService.createProcess()
   - Crea nuevo PCB con PID auto-incremental
   - Asigna estado NEW → READY
   - Si tiene memorySize, asigna memoria automáticamente
   - Agrega a tabla de procesos
   - Agrega a cola Ready
   ↓
5. Si modo automático: inicia simulación automáticamente
   ↓
6. Retorna PCB creado al cliente
   ↓
7. Cliente actualiza nombre con PID real (si es aplicación web)
```

### Flujo de Simulación

```
1. Usuario inicia simulación (selecciona algoritmo y quantum)
   ↓
2. POST /api/simulation/start
   ↓
3. SimulationController recibe petición
   ↓
4. ProcessSchedulerService.startSimulation()
   - Valida que no esté en modo automático (si es manual)
   - Prepara working set (procesos no terminados)
   - Crea hilo de simulación
   ↓
5. Hilo ejecuta algoritmo seleccionado:
   - FCFS: Ejecuta en orden de llegada
   - Round Robin: Ejecuta con quantum, preempta
   - Prioridad: Ejecuta por prioridad
   - SJF: Ejecuta por ráfaga más corta
   ↓
6. Durante ejecución:
   - Actualiza estado del proceso (RUNNING)
   - Decrementa tiempo restante
   - Genera entradas en timeline
   - Maneja interrupciones I/O automáticas
   - Preempta si es necesario
   ↓
7. Al terminar proceso:
   - Cambia estado a TERMINATED
   - Libera memoria automáticamente
   - Mueve a cola Terminated
   ↓
8. Frontend actualiza cada 500ms para mostrar cambios
```

### Flujo de Gestión de Memoria

```
1. Usuario inicializa memoria (opcional, default 1024 KB)
   ↓
2. POST /api/memory/initialize
   ↓
3. MemoryController recibe petición
   ↓
4. MemoryManagerService.initializeMemory()
   - Crea bloque inicial libre
   - Establece tamaño total
   ↓
5. Al crear proceso con memorySize:
   - MemoryManagerService.allocateMemory()
   - Busca bloque según algoritmo:
     * First Fit: Primer bloque que quepa
     * Best Fit: Bloque más pequeño que quepa
     * Worst Fit: Bloque más grande
     * Segmentación: Crea nuevo segmento
   - Asigna bloque al proceso
   - Actualiza PCB con dirección y tamaño
   - Calcula fragmentación
   ↓
6. Al terminar proceso:
   - MemoryManagerService.deallocateMemory()
   - Marca bloques como libres
   - Fusiona bloques libres adyacentes
   - Recalcula fragmentación
```

### Flujo de Interrupciones

```
1. Usuario emite interrupción manual
   O
   Sistema genera interrupción I/O automática
   ↓
2. POST /api/interruptions
   ↓
3. InterruptionController recibe petición
   ↓
4. ProcessSchedulerService.emitInterruption()
   - Busca proceso por PID
   - Aplica interrupción según tipo:
     * IO: RUNNING → WAITING
     * QUANTUM_EXPIRED: RUNNING → READY
     * MANUAL_STOP: RUNNING → TERMINATED
     * MANUAL_PAUSE: Pausa simulación
   - Actualiza historial del proceso
   - Mueve proceso a cola correspondiente
   ↓
5. Si es I/O automático:
   - Programa fin de I/O después de duración configurada
   - WAITING → READY automáticamente
```

### Flujo de Actualización en Tiempo Real

```
Frontend (cada 500ms):
   ↓
1. refreshAll() ejecuta en paralelo:
   - GET /api/processes → Lista de PCBs
   - GET /api/simulation/state → Estado del sistema y colas
   - GET /api/simulation/timeline → Timeline de ejecución
   - GET /api/memory/state → Estado de memoria
   ↓
2. Actualiza componentes Vue reactivos:
   - processes.value = nuevos datos
   - systemState.value = nuevo estado
   - timeline.value = nuevo timeline
   - memoryState.value = nuevo estado de memoria
   ↓
3. Vue re-renderiza componentes automáticamente
   ↓
4. Usuario ve cambios en tiempo real
```

---

## Componentes Técnicos

### Backend - Estructura de Datos

#### ProcessControlBlock (PCB)
```java
- pid: long (único, auto-incremental)
- name: String
- state: ProcessState (NEW, READY, RUNNING, WAITING, TERMINATED)
- arrivalTime: int
- burstTime: int
- remainingTime: int
- priority: int (0-10)
- createdAt: Instant
- history: List<String>
- memoryAddress: Integer
- memorySize: Integer
```

#### Colas de Procesos
```java
- readyQueue: Deque<ProcessControlBlock>
- waitingQueue: Deque<ProcessControlBlock>
- terminatedQueue: List<ProcessControlBlock>
- processTable: List<ProcessControlBlock>
```

#### Gestión de Memoria
```java
- blocks: List<MemoryBlock>
  - id: String
  - baseAddress: int
  - size: int
  - isAllocated: boolean
  - processId: Long (opcional)
```

### Frontend - Estructura de Componentes

#### Estado Reactivo (Vue 3)
```javascript
- processes: Array<PCB>
- systemState: {
    status: String,
    readyQueue: Array,
    waitingQueue: Array,
    terminatedQueue: Array,
    runningProcess: PCB | null
  }
- timeline: Array<TimelineEntry>
- memoryState: {
    blocks: Array,
    totalSize: number,
    usedSize: number,
    freeSize: number,
    internalFragmentation: number,
    externalFragmentation: number,
    currentAlgorithm: String
  }
```

### API REST - Endpoints

#### Procesos
- `POST /api/processes` - Crear proceso
- `GET /api/processes` - Listar todos los procesos
- `PUT /api/processes/{pid}/name` - Actualizar nombre
- `DELETE /api/processes` - Limpiar todos los procesos

#### Simulación
- `POST /api/simulation/start` - Iniciar simulación
- `POST /api/simulation/pause` - Pausar simulación
- `POST /api/simulation/resume` - Reanudar simulación
- `POST /api/simulation/stop` - Detener simulación
- `GET /api/simulation/state` - Estado del sistema
- `GET /api/simulation/timeline` - Timeline de ejecución
- `GET /api/simulation/mode` - Obtener modo (auto/manual)
- `PUT /api/simulation/mode` - Cambiar modo

#### Interrupciones
- `POST /api/interruptions` - Emitir interrupción
- `PUT /api/interruptions/settings` - Configurar I/O automático

#### Memoria
- `POST /api/memory/initialize` - Inicializar memoria
- `POST /api/memory/allocate` - Asignar memoria
- `POST /api/memory/deallocate/{processId}` - Liberar memoria
- `GET /api/memory/state` - Estado de memoria
- `PUT /api/memory/algorithm` - Cambiar algoritmo

---

## Flujos de Datos

### Diagrama de Secuencia - Creación y Ejecución de Proceso

```
Usuario          Frontend          Backend API      ProcessScheduler    MemoryManager
  |                 |                   |                  |                  |
  |--Crear Proceso->|                   |                  |                  |
  |                 |--POST /processes->|                  |                  |
  |                 |                   |--createProcess()->                  |
  |                 |                   |                  |--allocateMemory()|
  |                 |                   |                  |<-----------------|
  |                 |                   |<--PCB creado----|                  |
  |                 |<--200 OK----------|                  |                  |
  |<--Actualizado---|                   |                  |                  |
  |                 |                   |                  |                  |
  |--Iniciar Simul->|                   |                  |                  |
  |                 |--POST /sim/start->|                  |                  |
  |                 |                   |--startSim()------|                  |
  |                 |                   |                  |--[Ejecuta]-------|
  |                 |<--Estado----------|                  |                  |
  |<--Timeline------|                   |                  |                  |
```

### Diagrama de Estados - Proceso

```
     [NEW]
       |
       | (Admisión)
       v
    [READY] <-------------------+
       |                        |
       | (Planificador)         | (Preemption/Quantum)
       v                        |
   [RUNNING]                    |
       |                        |
       | (I/O)                  |
       v                        |
   [WAITING]                    |
       |                        |
       | (Fin I/O)              |
       +------------------------+
       |
       | (Fin ráfaga)
       v
  [TERMINATED]
```

---

## Características Destacadas

### 1. **Sincronización de Colores**
- Cada proceso tiene un color único basado en su PID
- El mismo color se usa en:
  - Tabla de procesos (fondo, borde, badge)
  - Timeline de Gantt (fondo del slice)
  - Panel de memoria (bloques asignados)

### 2. **Responsive Design**
- Adaptación completa a móviles, tablets y desktop
- Grid layout que se reorganiza según tamaño de pantalla
- Tablas con scroll horizontal en pantallas pequeñas
- Componentes que se apilan verticalmente en móviles

### 3. **Tiempo Real**
- Actualización automática cada 500ms
- Sin necesidad de refrescar manualmente
- Sincronización entre múltiples componentes

### 4. **Modo Automático Inteligente**
- Los procesos se ejecutan automáticamente al crearse
- Ideal para demostraciones y laboratorios
- Puede desactivarse para control manual

### 5. **Integración Externa**
- API REST completa y documentada
- CORS configurado para desarrollo
- Fácil integración con otras aplicaciones

---

## Notas Técnicas

### Limitaciones Conocidas
- El simulador es determinista (no simula latencia de hardware real)
- Los tiempos son en unidades abstractas, no segundos reales
- La memoria se gestiona de forma simplificada (no hay paginación)

### Mejoras Futuras Posibles
- Paginación de memoria
- Algoritmos de planificación adicionales (SRTF, Multilevel Queue)
- Simulación de dispositivos I/O
- Exportación de resultados a CSV/JSON
- Gráficas de métricas (tiempo de espera, turnaround time)

---

## Conclusión

Este simulador educativo proporciona una implementación completa y funcional de los conceptos fundamentales de sistemas operativos, incluyendo:

- Gestión de procesos con PCB completo
- Múltiples algoritmos de planificación
- Gestión de memoria con varios algoritmos
- Sistema de interrupciones
- Visualización en tiempo real
- Interfaz moderna y responsive
- Integración con aplicaciones externas

Es ideal para:
- Enseñanza de sistemas operativos
- Laboratorios prácticos
- Demostraciones interactivas
- Experimentación con algoritmos

---

