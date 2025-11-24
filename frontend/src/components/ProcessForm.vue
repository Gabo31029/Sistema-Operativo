<script setup>
import { reactive } from 'vue'

const emit = defineEmits(['submit'])

const form = reactive({
  name: '',
  arrivalTime: 0,
  burstTime: 5,
  priority: 1,
})

function handleSubmit() {
  emit('submit', { ...form })
  form.name = ''
  form.arrivalTime = 0
  form.burstTime = 5
  form.priority = 1
}
</script>

<template>
  <section class="panel">
    <h2>Nuevo proceso</h2>
    <form class="form-grid" @submit.prevent="handleSubmit">
      <label>
        Nombre
        <input v-model="form.name" placeholder="P1" required />
      </label>
      <label>
        Llegada
        <input v-model.number="form.arrivalTime" min="0" type="number" required />
      </label>
      <label>
        Ráfaga CPU
        <input v-model.number="form.burstTime" min="1" type="number" required />
      </label>
      <label>
        Prioridad (0-10)
        <input v-model.number="form.priority" min="0" max="10" type="number" required />
      </label>
      <button type="submit">Crear proceso</button>
    </form>
  </section>
</template>

<style scoped>
.panel {
  background: #fff;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.08);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 1rem;
}

label {
  font-size: 0.9rem;
  font-weight: 600;
  color: #1f2937;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

input {
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 0.5rem 0.75rem;
}

button {
  margin-top: 0.5rem;
  background: #2563eb;
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 0.75rem 1rem;
  font-weight: 600;
  cursor: pointer;
}

button:hover {
  background: #1e4fd6;
}
</style>

