<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import ProcessForm from './components/ProcessForm.vue'
import ProcessTable from './components/ProcessTable.vue'
import QueuesPanel from './components/QueuesPanel.vue'
import TimelineChart from './components/TimelineChart.vue'
import ControlPanel from './components/ControlPanel.vue'
import InterruptionPanel from './components/InterruptionPanel.vue'
import MemoryPanel from './components/MemoryPanel.vue'
import { osApi } from './services/api'

const processes = ref([])
const timeline = ref([])
const systemState = ref({
  status: 'IDLE',
  readyQueue: [],
  waitingQueue: [],
  terminatedQueue: [],
  runningProcess: null,
})
const algorithm = ref('FCFS')
const quantum = ref(2)
const ioProbability = ref(0.3)
const ioDuration = ref(3)
const autoIO = ref(true)
const automaticMode = ref(true)
const loading = ref(false)
const errorMessage = ref('')
const memoryState = ref({
  blocks: [],
  totalSize: 1024,
  usedSize: 0,
  freeSize: 1024,
  internalFragmentation: 0,
  externalFragmentation: 0,
  currentAlgorithm: 'FIRST_FIT',
  segments: [],
})
const memoryExpanded = ref(true) // Siempre expandido - gestión automática

const queueSummary = computed(() => ({
  total: processes.value.length,
  ready: systemState.value.readyQueue?.length ?? 0,
  waiting: systemState.value.waitingQueue?.length ?? 0,
  terminated: systemState.value.terminatedQueue?.length ?? 0,
}))

function mapError(error) {
  if (!error) return 'Error desconocido'
  const message = error.message ?? String(error)
  if (message.includes('Failed to fetch')) {
    return 'No se pudo contactar al backend. ¿Está el servidor en ejecución?'
  }
  return message
}

async function safeCall(callback) {
  loading.value = true
  errorMessage.value = ''
  try {
    await callback()
  } catch (error) {
    errorMessage.value = mapError(error)
  } finally {
    loading.value = false
  }
}

async function refreshMemory() {
  try {
    const state = await osApi.getMemoryState()
    memoryState.value = state
  } catch (error) {
    // Silently fail for memory state
    console.error('Failed to refresh memory state:', error)
  }
}

async function refreshAll() {
  try {
    const [proc, state, time, mem] = await Promise.all([
      osApi.listProcesses(),
      osApi.getSystemState(),
      osApi.getTimeline(),
      osApi.getMemoryState().catch(() => null),
    ])
    processes.value = proc
    systemState.value = state
    timeline.value = time
    if (mem) {
      memoryState.value = mem
    }
  } catch (error) {
    errorMessage.value = error.message
  }
}

async function handleCreateProcess(payload) {
  await safeCall(async () => {
    await osApi.createProcess(payload)
    await refreshAll()
  })
}

async function handleSimulation(action) {
  const payload =
    action === 'start'
      ? { algorithm: algorithm.value, quantum: quantum.value }
      : undefined

  await safeCall(async () => {
    switch (action) {
      case 'start':
        await osApi.startSimulation(payload)
        break
      case 'pause':
        await osApi.pauseSimulation()
        break
      case 'resume':
        await osApi.resumeSimulation()
        break
      case 'stop':
        await osApi.stopSimulation()
        break
    }
    await refreshAll()
  })
}

async function handleInterruption(payload) {
  await safeCall(async () => {
    await osApi.emitInterruption(payload)
    await refreshAll()
  })
}

async function handleMemoryInitialize(totalSize) {
  await safeCall(async () => {
    await osApi.initializeMemory(totalSize)
    await refreshAll()
  })
}

async function handleMemoryAllocate(payload) {
  await safeCall(async () => {
    await osApi.allocateMemory(payload.processId, payload.size, payload.algorithm)
    await refreshAll()
  })
}

async function handleMemoryDeallocate(processId) {
  await safeCall(async () => {
    await osApi.deallocateMemory(processId)
    await refreshAll()
  })
}

async function handleMemoryAlgorithmChange(algorithm) {
  await safeCall(async () => {
    await osApi.setMemoryAlgorithm(algorithm)
    await refreshAll()
  })
}

function openWebApp() {
  window.open('http://localhost:3000', '_blank')
}

async function handleIOProbabilityChange(value) {
  ioProbability.value = value
  await safeCall(async () => {
    await osApi.setIOSettings(ioProbability.value, ioDuration.value, autoIO.value)
  })
}

async function handleIODurationChange(value) {
  ioDuration.value = value
  await safeCall(async () => {
    await osApi.setIOSettings(ioProbability.value, ioDuration.value, autoIO.value)
  })
}

async function handleAutoIOChange(value) {
  autoIO.value = value
  await safeCall(async () => {
    await osApi.setIOSettings(ioProbability.value, ioDuration.value, autoIO.value)
  })
}

async function handleAutomaticModeChange(value) {
  automaticMode.value = value
  await safeCall(async () => {
    await osApi.setMode(value)
    await refreshAll()
  })
}

async function handleClearAll() {
  if (!confirm('¿Estás seguro de que quieres eliminar todos los procesos? Esta acción no se puede deshacer.')) {
    return
  }
  await safeCall(async () => {
    await osApi.clearAllProcesses()
    await refreshAll()
  })
}

let refreshInterval

function setupRefreshInterval() {
  // Limpiar intervalo anterior si existe
  if (refreshInterval) {
    clearInterval(refreshInterval)
  }
  
  // Actualizar cada 500ms para actualización en tiempo real más fluida
  refreshInterval = setInterval(() => {
    refreshAll()
    refreshMemory() // Actualizar memoria también
  }, 500)
}

onMounted(async () => {
  refreshAll()
  setupRefreshInterval()
  
  // Cargar modo actual del backend
  try {
    const mode = await osApi.getMode()
    if (mode && mode.automatic !== undefined) {
      automaticMode.value = mode.automatic
    }
  } catch (error) {
    console.error('Error loading mode:', error)
  }
  
  // Observar cambios en el estado para ajustar el intervalo
  watch(() => systemState.value.status, () => {
    setupRefreshInterval()
  })
})

onUnmounted(() => {
  if (refreshInterval) {
    clearInterval(refreshInterval)
  }
})
</script>

<template>
  <main class="workspace">
    <header class="strip">
        <div>
          <h1 class="sim">Panel de Control del Sistema Operativo</h1>
          <p class="subtitle-header">Monitoreando procesos en tiempo real desde aplicaciones web</p>
        </div>
        <div class="header-actions">
          <button class="btn-external" @click="openWebApp" title="Abrir aplicación web paralela">
            🌐 Aplicación Web
          </button>
        </div>
    </header>

    <section class="summary-strip">
      <article>
        <label>Total</label>
        <strong>{{ queueSummary.total }}</strong>
      </article>
      <article>
        <label>Ready</label>
        <strong>{{ queueSummary.ready }}</strong>
      </article>
      <article>
        <label>Waiting</label>
        <strong>{{ queueSummary.waiting }}</strong>
      </article>
      <article>
        <label>Terminated</label>
        <strong>{{ queueSummary.terminated }}</strong>
      </article>
    </section>

    <section class="grid-layout">
      <article class="card form-card">
        <ProcessForm @submit="handleCreateProcess" />
      </article>

          <article class="card control-card">
            <ControlPanel
              :algorithm="algorithm"
              :quantum="quantum"
              :status="systemState.status"
              :io-probability="ioProbability"
              :io-duration="ioDuration"
              :auto-i-o="autoIO"
              :automatic-mode="automaticMode"
              @update:algorithm="algorithm = $event"
              @update:quantum="quantum = $event"
              @update:io-probability="handleIOProbabilityChange"
              @update:io-duration="handleIODurationChange"
              @update:auto-i-o="handleAutoIOChange"
              @update:automatic-mode="handleAutomaticModeChange"
              @start="handleSimulation('start')"
              @pause="handleSimulation('pause')"
              @resume="handleSimulation('resume')"
              @stop="handleSimulation('stop')"
            />
          </article>

      <article class="card interrupt-card">
        <InterruptionPanel :processes="processes" @submit="handleInterruption" />
      </article>

      <article class="card table-card">
        <ProcessTable :processes="processes" @clear-all="handleClearAll" />
      </article>

      <article class="card timeline-card">
        <TimelineChart :timeline="timeline" />
      </article>

      <article class="card queues-card">
        <QueuesPanel :state="systemState" />
      </article>

      <article class="card memory-card">
        <MemoryPanel
          :memory-state="memoryState"
          :expanded="memoryExpanded"
          :processes="processes"
          @expand="memoryExpanded = true"
          @collapse="memoryExpanded = false"
          @initialize="handleMemoryInitialize"
          @allocate="handleMemoryAllocate"
          @deallocate="handleMemoryDeallocate"
          @change-algorithm="handleMemoryAlgorithmChange"
        />
      </article>
    </section>

    <p v-if="loading" class="hint">Procesando...</p>
    <p v-if="errorMessage" class="error">⚠️ {{ errorMessage }}</p>
  </main>
</template>
