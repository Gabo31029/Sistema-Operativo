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
    <header>
      <h2>Interrupciones</h2>
    </header>
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
      <button type="submit" class="submit-btn">📤 Emitir</button>
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
  height: 100%;
}

header {
  margin-bottom: 0.5rem;
  padding-bottom: 0.75rem;
  border-bottom: 2px solid #e5e7eb;
}

header h2 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 700;
  color: #1f2937;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 0.85rem;
  align-items: end;
}

label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #374151;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

select,
input {
  border: 1px solid #d1d5db;
  border-radius: 6px;
  padding: 0.5rem 0.75rem;
  font-size: 0.9rem;
  transition: border-color 0.2s;
}

select:focus,
input:focus {
  outline: none;
  border-color: #047857;
  box-shadow: 0 0 0 3px rgba(4, 120, 87, 0.1);
}

.reason {
  grid-column: span 2;
}

.submit-btn {
  border: none;
  border-radius: 6px;
  padding: 0.6rem 1.2rem;
  font-weight: 600;
  cursor: pointer;
  background: #047857;
  color: #fff;
  font-size: 0.9rem;
  transition: all 0.2s;
  grid-column: span 2;
  justify-self: start;
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.submit-btn:hover {
  background: #065f46;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(4, 120, 87, 0.2);
}
</style>

