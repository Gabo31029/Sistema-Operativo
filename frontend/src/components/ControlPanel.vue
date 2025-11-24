<script setup>
const algorithms = [
  { value: 'FCFS', label: 'First Come First Served' },
  { value: 'ROUND_ROBIN', label: 'Round Robin' },
  { value: 'PRIORITY', label: 'Prioridad' },
  { value: 'SJF', label: 'Shortest Job First' },
]

const props = defineProps({
  algorithm: {
    type: String,
    default: 'FCFS',
  },
  quantum: {
    type: Number,
    default: 2,
  },
  status: {
    type: String,
    default: 'IDLE',
  },
})

const emit = defineEmits(['update:algorithm', 'update:quantum', 'start', 'pause', 'resume', 'stop'])
</script>

<template>
  <section class="panel">
    <header>
      <h2>Control de simulación</h2>
      <span class="status">Estado: {{ status }}</span>
    </header>
    <div class="controls">
      <label>
        Algoritmo
        <select :value="algorithm" @change="emit('update:algorithm', $event.target.value)">
          <option v-for="option in algorithms" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </label>
      <label>
        Quantum (RR)
        <input
          :value="quantum"
          min="1"
          type="number"
          @input="emit('update:quantum', Number($event.target.value))"
        />
      </label>
      <div class="buttons">
        <button class="primary" @click="emit('start')">Iniciar</button>
        <button @click="emit('pause')">Pausar</button>
        <button @click="emit('resume')">Reanudar</button>
        <button class="danger" @click="emit('stop')">Detener</button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.panel {
  background: #fff;
  border-radius: 12px;
  padding: 1.5rem;
}

header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.status {
  font-weight: 600;
  color: #2563eb;
}

.controls {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 1rem;
  align-items: end;
}

label {
  font-size: 0.9rem;
  font-weight: 600;
  color: #1f2937;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

select,
input {
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 0.5rem 0.75rem;
}

.buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

button {
  border: none;
  border-radius: 8px;
  padding: 0.5rem 1rem;
  font-weight: 600;
  cursor: pointer;
  background: #e5e7eb;
}

button.primary {
  background: #2563eb;
  color: #fff;
}

button.danger {
  background: #dc2626;
  color: #fff;
}
</style>

