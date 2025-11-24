<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import ProcessForm from './components/ProcessForm.vue'
import ProcessTable from './components/ProcessTable.vue'
import QueuesPanel from './components/QueuesPanel.vue'
import TimelineChart from './components/TimelineChart.vue'
import ControlPanel from './components/ControlPanel.vue'
import InterruptionPanel from './components/InterruptionPanel.vue'
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
const loading = ref(false)
const errorMessage = ref('')

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

async function refreshAll() {
  try {
    const [proc, state, time] = await Promise.all([
      osApi.listProcesses(),
      osApi.getSystemState(),
      osApi.getTimeline(),
    ])
    processes.value = proc
    systemState.value = state
    timeline.value = time
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

let refreshInterval

onMounted(() => {
  refreshAll()
  refreshInterval = setInterval(refreshAll, 4000)
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
        <h1 class="sim">Simulador de un SO</h1>
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
          @update:algorithm="algorithm = $event"
          @update:quantum="quantum = $event"
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
        <ProcessTable :processes="processes" />
      </article>

      <article class="card timeline-card">
        <TimelineChart :timeline="timeline" />
      </article>

      <article class="card queues-card">
        <QueuesPanel :state="systemState" />
      </article>
    </section>

    <p v-if="loading" class="hint">Procesando...</p>
    <p v-if="errorMessage" class="error">⚠️ {{ errorMessage }}</p>
  </main>
</template>
