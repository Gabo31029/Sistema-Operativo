<script setup>
import { reactive } from 'vue'

const emit = defineEmits(['submit'])

const form = reactive({
  name: '',
  arrivalTime: 0,
  burstTime: 5,
  priority: 1,
  memorySize: null,
})

function handleSubmit() {
  const payload = { ...form }
  if (!payload.memorySize || payload.memorySize === 0) {
    payload.memorySize = null
  }
  emit('submit', payload)
  form.name = ''
  form.arrivalTime = 0
  form.burstTime = 5
  form.priority = 1
  form.memorySize = null
}
</script>

<template>
  <section class="panel">
    <header>
      <h2>Nuevo proceso</h2>
    </header>
    <form class="form-compact" @submit.prevent="handleSubmit">
      <label>
        <span>Nombre</span>
        <input v-model="form.name" placeholder="P1" required />
      </label>
      <label>
        <span>Llegada</span>
        <input v-model.number="form.arrivalTime" min="0" type="number" required />
      </label>
      <label>
        <span>Ráfaga CPU</span>
        <input v-model.number="form.burstTime" min="1" type="number" required />
      </label>
      <label>
        <span>Prioridad</span>
        <input v-model.number="form.priority" min="0" max="10" type="number" required />
      </label>
      <label>
        <span>Memoria (KB)</span>
        <input v-model.number="form.memorySize" min="0" type="number" placeholder="Opcional" />
      </label>
      <button type="submit" class="submit-btn">➕ Crear proceso</button>
    </form>
  </section>
</template>

<style scoped>
.panel {
  background: #fff;
  border-radius: 12px;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  overflow: hidden;
}

header {
  margin-bottom: 0.75rem;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid #e5e7eb;
}

header h2 {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
  color: #1f2937;
}

.form-compact {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
  flex: 1;
}

label {
  font-size: 0.75rem;
  font-weight: 600;
  color: #374151;
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

label span {
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #6b7280;
}

input {
  border: 1px solid #d1d5db;
  border-radius: 6px;
  padding: 0.4rem 0.65rem;
  font-size: 0.85rem;
  transition: border-color 0.2s;
}

input:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

input::placeholder {
  color: #9ca3af;
}

.submit-btn {
  margin-top: 0.5rem;
  background: #2563eb;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 0.55rem 1rem;
  font-weight: 600;
  cursor: pointer;
  font-size: 0.85rem;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
}

.submit-btn:hover {
  background: #1d4ed8;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(37, 99, 235, 0.2);
}

/* Responsive: Pantallas pequeñas */
@media (max-width: 480px) {
  .panel {
    padding: 0.75rem;
  }
  
  header h2 {
    font-size: 0.95rem;
  }
  
  label {
    font-size: 0.7rem;
  }
  
  label span {
    font-size: 0.65rem;
  }
  
  input {
    font-size: 0.8rem;
    padding: 0.35rem 0.6rem;
  }
  
  .submit-btn {
    font-size: 0.8rem;
    padding: 0.5rem 0.9rem;
  }
}
</style>

