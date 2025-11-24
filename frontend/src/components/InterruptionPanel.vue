<script setup>
import { reactive } from 'vue'

const props = defineProps({
  processes: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['submit'])

const form = reactive({
  pid: '',
  type: 'IO',
  reason: 'Interrupción manual',
})

function handleSubmit() {
  emit('submit', { pid: Number(form.pid), type: form.type, reason: form.reason })
}
</script>

<template>
  <section class="panel">
    <h2>Interrupciones</h2>
    <form class="form-grid" @submit.prevent="handleSubmit">
      <label>
        Proceso
        <select v-model="form.pid" required>
          <option value="" disabled>Selecciona PID</option>
          <option v-for="process in processes" :key="process.pid" :value="process.pid">
            {{ process.name }} (PID {{ process.pid }})
          </option>
        </select>
      </label>
      <label>
        Tipo
        <select v-model="form.type">
          <option value="IO">I/O</option>
          <option value="QUANTUM_EXPIRED">Expiración de quantum</option>
          <option value="MANUAL_STOP">Detener</option>
          <option value="MANUAL_PAUSE">Pausar</option>
        </select>
      </label>
      <label class="reason">
        Motivo
        <input v-model="form.reason" required />
      </label>
      <button type="submit">Emitir</button>
    </form>
  </section>
</template>

<style scoped>
.panel {
  background: #fff;
  border-radius: 12px;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 0.75rem;
}

label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #0f172a;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

select,
input {
  border: 1px solid #d7dfe9;
  border-radius: 6px;
  padding: 0.45rem 0.65rem;
  font-size: 0.9rem;
}

.reason {
  grid-column: span 2;
}

button {
  border: none;
  border-radius: 6px;
  padding: 0.55rem 0.9rem;
  font-weight: 600;
  cursor: pointer;
  background: #047857;
  color: #fff;
  font-size: 0.95rem;
}
</style>

