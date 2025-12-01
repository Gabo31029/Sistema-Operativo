<script setup>
import { computed } from 'vue'

const props = defineProps({
  memoryState: {
    type: Object,
    default: () => ({
      blocks: [],
      totalSize: 1024,
      usedSize: 0,
      freeSize: 1024,
      internalFragmentation: 0,
      externalFragmentation: 0,
      currentAlgorithm: 'FIRST_FIT',
      segments: [],
    }),
  },
  expanded: {
    type: Boolean,
    default: true, // Siempre expandido por defecto
  },
  processes: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['expand', 'collapse', 'initialize', 'allocate', 'deallocate', 'change-algorithm'])

const selectedProcessId = computed(() => {
  const processWithMemory = props.processes.find(p => p.memoryAddress != null)
  return processWithMemory?.pid || null
})

const memoryPercentage = computed(() => {
  if (props.memoryState.totalSize === 0) return 0
  return Math.round((props.memoryState.usedSize / props.memoryState.totalSize) * 100)
})

const algorithmNames = {
  FIRST_FIT: 'First Fit',
  BEST_FIT: 'Best Fit',
  WORST_FIT: 'Worst Fit',
  SEGMENTATION: 'Segmentación',
}

function handleInitialize() {
  const size = prompt('Tamaño total de memoria (KB):', props.memoryState.totalSize || 1024)
  if (size) {
    emit('initialize', parseInt(size))
  }
}

function handleAllocate() {
  const processId = prompt('PID del proceso:')
  const size = prompt('Tamaño a asignar (KB):')
  if (processId && size) {
    emit('allocate', {
      processId: parseInt(processId),
      size: parseInt(size),
      algorithm: props.memoryState.currentAlgorithm,
    })
  }
}

function handleDeallocate() {
  const processId = prompt('PID del proceso a liberar:')
  if (processId) {
    emit('deallocate', parseInt(processId))
  }
}

function handleAlgorithmChange(event) {
  emit('change-algorithm', event.target.value)
}

// Función para generar un color único basado en el PID (misma paleta que ProcessTable)
function getProcessColor(pid) {
  if (!pid) return '#10b981' // Verde para bloques libres
  
  const colors = [
    '#3b82f6', // Azul
    '#10b981', // Verde
    '#f59e0b', // Ámbar
    '#ef4444', // Rojo
    '#8b5cf6', // Púrpura
    '#06b6d4', // Cyan
    '#ec4899', // Rosa
    '#14b8a6', // Teal
    '#f97316', // Naranja
    '#6366f1', // Índigo
    '#22c55e', // Verde esmeralda
    '#eab308', // Amarillo
    '#84cc16', // Lima
    '#a855f7', // Violeta
    '#f43f5e', // Rose
  ]
  
  return colors[pid % colors.length]
}

// Función para obtener el color de fondo más claro
function getBlockBackgroundColor(pid) {
  if (!pid) return '#10b981' // Verde para bloques libres
  
  const baseColor = getProcessColor(pid)
  const hex = baseColor.replace('#', '')
  const r = parseInt(hex.substr(0, 2), 16)
  const g = parseInt(hex.substr(2, 2), 16)
  const b = parseInt(hex.substr(4, 2), 16)
  return `rgba(${r}, ${g}, ${b}, 0.1)`
}

// Tamaño fijo de bloque en KB (debe coincidir con el backend)
const BLOCK_SIZE_KB = 32

// Número total de celdas físicas (bloques de 32KB)
const totalCells = computed(() => {
  if (!props.memoryState.totalSize || props.memoryState.totalSize <= 0) return 0
  return Math.max(1, Math.floor(props.memoryState.totalSize / BLOCK_SIZE_KB))
})

// Buscar el bloque de memoria que cubre una dirección base concreta
function findBlockForAddress(address) {
  return props.memoryState.blocks?.find((block) => {
    const start = block.baseAddress
    const end = block.baseAddress + block.size
    return address >= start && address < end
  })
}

// Estilo visual de cada celda de 32KB en la barra de memoria
function getCellStyle(cellIndex) {
  const cellStart = cellIndex * BLOCK_SIZE_KB
  const cellEnd = cellStart + BLOCK_SIZE_KB

  const baseStyle = {
    borderRight: '1px solid rgba(148, 163, 184, 0.5)',
  }

  const block = findBlockForAddress(cellStart)

  // Celda completamente libre (sin bloque asignado)
  if (!block || !block.allocated || !block.processId) {
    return {
      ...baseStyle,
      backgroundColor: '#ffffff',
    }
  }

  const color = getProcessColor(block.processId)

  // requestedSize puede ser menor que el tamaño reservado para este bloque.
  // Si no viene informado o es 0 (bloques antiguos), usar size completo.
  const requestedSize =
    block.requestedSize && block.requestedSize > 0
      ? block.requestedSize
      : block.size
  const requestedEnd = block.baseAddress + requestedSize

  // Si la parte usada del bloque termina antes de esta celda → celda vacía (desperdicio)
  if (requestedEnd <= cellStart) {
    return {
      ...baseStyle,
      backgroundColor: '#ffffff',
    }
  }

  // Si la parte usada cubre toda la celda → celda llena del color del proceso
  if (requestedEnd >= cellEnd) {
    return {
      ...baseStyle,
      backgroundColor: color,
    }
  }

  // Celda parcialmente utilizada: parte del color del proceso, parte blanca
  const usedInCell = Math.max(0, requestedEnd - cellStart)
  const usedPercent = Math.max(
    0,
    Math.min(100, (usedInCell / BLOCK_SIZE_KB) * 100)
  )

  return {
    ...baseStyle,
    backgroundImage: `linear-gradient(to right, ${color} 0%, ${color} ${usedPercent}%, #ffffff ${usedPercent}%, #ffffff 100%)`,
  }
}
</script>

<template>
  <section class="memory-panel">
    <header class="memory-header">
      <h2>Gestión de Memoria</h2>
      <button class="memory-toggle" @click="expanded ? $emit('collapse') : $emit('expand')">
        {{ expanded ? '−' : '+' }}
      </button>
    </header>

    <div v-if="!expanded" class="memory-summary">
      <span>Memoria: {{ memoryState.usedSize }}/{{ memoryState.totalSize }} KB</span>
      <span>|</span>
      <span>Algoritmo: {{ algorithmNames[memoryState.currentAlgorithm] || memoryState.currentAlgorithm }}</span>
    </div>

    <div v-if="expanded" class="memory-expanded">
      <div class="memory-controls">
        <button @click="handleInitialize">Inicializar Memoria</button>
        <button @click="handleAllocate">Asignar</button>
        <button @click="handleDeallocate">Liberar</button>
        <select :value="memoryState.currentAlgorithm" @change="handleAlgorithmChange">
          <option value="FIRST_FIT">First Fit</option>
          <option value="BEST_FIT">Best Fit</option>
          <option value="WORST_FIT">Worst Fit</option>
          <option value="SEGMENTATION">Segmentación</option>
        </select>
      </div>

      <div class="memory-stats">
        <div class="stat-item">
          <label>Total</label>
          <strong>{{ memoryState.totalSize }} KB</strong>
        </div>
        <div class="stat-item">
          <label>Usado</label>
          <strong>{{ memoryState.usedSize }} KB ({{ memoryPercentage }}%)</strong>
        </div>
        <div class="stat-item">
          <label>Libre</label>
          <strong>{{ memoryState.freeSize }} KB</strong>
        </div>
        <div class="stat-item">
          <label>Fragmentación Interna</label>
          <strong>{{ memoryState.internalFragmentation }} KB</strong>
        </div>
        <div class="stat-item">
          <label>Fragmentación Externa</label>
          <strong>{{ memoryState.externalFragmentation }} KB</strong>
        </div>
      </div>

      <div class="memory-visualization">
        <div
          v-for="cellIndex in totalCells"
          :key="cellIndex"
          class="memory-cell"
          :style="getCellStyle(cellIndex - 1)"
          :title="`${(cellIndex - 1) * BLOCK_SIZE_KB} KB - ${cellIndex * BLOCK_SIZE_KB} KB`"
        ></div>
      </div>

      <table class="memory-table">
        <thead>
          <tr>
            <th>Dirección</th>
            <th>Tamaño</th>
            <th>Estado</th>
            <th>Proceso (PID)</th>
          </tr>
        </thead>
        <tbody>
          <tr 
            v-for="block in memoryState.blocks" 
            :key="block.id"
            :style="{
              backgroundColor: block.allocated && block.processId 
                ? getBlockBackgroundColor(block.processId) 
                : 'transparent',
              borderLeft: block.allocated && block.processId 
                ? `3px solid ${getProcessColor(block.processId)}` 
                : 'none',
            }"
            class="memory-row"
          >
            <td>{{ block.baseAddress }}</td>
            <td>
              <span
                :title="block.allocated && block.requestedSize ? `Lógico: ${block.requestedSize} KB / Físico: ${block.size} KB` : `Físico: ${block.size} KB`"
              >
                {{
                  block.allocated && block.requestedSize && block.requestedSize > 0
                    ? block.requestedSize
                    : block.size
                }} KB
              </span>
            </td>
            <td>
              <span 
                class="block-status"
                :style="{
                  color: block.allocated && block.processId 
                    ? getProcessColor(block.processId) 
                    : '#10b981',
                }"
              >
                {{ block.allocated ? 'Asignado' : 'Libre' }}
              </span>
            </td>
            <td>
              <span 
                v-if="block.processId"
                class="pid-badge"
                :style="{ backgroundColor: getProcessColor(block.processId) }"
              >
                {{ block.processId }}
              </span>
              <span v-else>-</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<style scoped>
.memory-panel {
  background: #fff;
  border-radius: 8px;
  padding: 1rem;
  border: 1px solid #e5e7eb;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  overflow: hidden;
}

.memory-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.memory-header h2 {
  margin: 0;
  font-size: 1.1rem;
  color: #1f2937;
}

.memory-toggle {
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 4px;
  width: 24px;
  height: 24px;
  cursor: pointer;
  font-size: 1.2rem;
  line-height: 1;
}

.memory-summary {
  display: flex;
  gap: 0.5rem;
  font-size: 0.9rem;
  color: #6b7280;
}

.memory-expanded {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.memory-controls {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .memory-controls {
    flex-direction: column;
  }
  
  .memory-controls button,
  .memory-controls select {
    width: 100%;
  }
}

.memory-controls button,
.memory-controls select {
  padding: 0.5rem 1rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: white;
  cursor: pointer;
  font-size: 0.9rem;
}

.memory-controls button:hover {
  background: #f3f4f6;
}

.memory-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 0.75rem;
}

@media (max-width: 768px) {
  .memory-stats {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .memory-stats {
    grid-template-columns: 1fr;
  }
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.stat-item label {
  font-size: 0.8rem;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.stat-item strong {
  font-size: 1.1rem;
  color: #1f2937;
}

.memory-visualization {
  display: flex;
  width: 100%;
  max-width: 100%;
  height: 60px;
  border: 2px solid #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
  background: #f9fafb;
  box-sizing: border-box;
}

.memory-cell {
  flex: 1;
  min-width: 0;
  height: 100%;
  transition: background-color 0.2s, background-image 0.2s;
}

.memory-table {
  width: 100%;
  max-width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
  box-sizing: border-box;
}

.memory-table th,
.memory-table td {
  padding: 0.5rem;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
}

.memory-table th {
  background: #f9fafb;
  font-weight: 600;
  color: #374151;
}

.memory-row {
  transition: all 0.2s ease;
}

.memory-row:hover {
  transform: translateX(2px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.block-status {
  font-weight: 600;
}

.pid-badge {
  display: inline-block;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  color: white;
  font-weight: 600;
  font-size: 0.85rem;
  min-width: 2rem;
  text-align: center;
}

/* Responsive: Tabla de memoria */
@media (max-width: 768px) {
  .memory-table {
    font-size: 0.85rem;
    display: block;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    width: 100%;
    max-width: 100%;
  }
  
  .memory-table th,
  .memory-table td {
    padding: 0.4rem 0.35rem;
    white-space: nowrap;
  }
}

/* Responsive: Pantallas pequeñas */
@media (max-width: 480px) {
  .memory-panel {
    padding: 0.75rem;
  }
  
  .memory-header h2 {
    font-size: 0.95rem;
  }
  
  .memory-table {
    font-size: 0.8rem;
  }
  
  .memory-table th,
  .memory-table td {
    padding: 0.35rem 0.3rem;
  }
  
  .stat-item strong {
    font-size: 1rem;
  }
}
</style>

