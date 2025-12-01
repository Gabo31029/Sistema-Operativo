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
  ioProbability: {
    type: Number,
    default: 0.3,
  },
  ioDuration: {
    type: Number,
    default: 3,
  },
  autoIO: {
    type: Boolean,
    default: true,
  },
  automaticMode: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits(['update:algorithm', 'update:quantum', 'update:ioProbability', 'update:ioDuration', 'update:autoIO', 'update:automaticMode', 'start', 'pause', 'resume', 'stop'])
</script>

<template>
  <section class="panel">
    <header>
      <h2>Control de simulación</h2>
      <div class="header-right">
        <span class="status">Estado: {{ status }}</span>
        <button 
          class="mode-toggle" 
          :class="{ 'automatic': automaticMode, 'manual': !automaticMode }"
          @click="emit('update:automaticMode', !automaticMode)"
          :title="automaticMode ? 'Modo Automático: Los procesos se ejecutan automáticamente' : 'Modo Manual: Control total sobre la simulación'"
        >
          {{ automaticMode ? '🔄 Automático' : '✋ Manual' }}
        </button>
      </div>
    </header>
    <div class="controls">
      <div class="section">
        <h3>Algoritmo de Planificación</h3>
        <div class="section-content">
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
        </div>
      </div>

      <div class="section">
        <h3>Configuración I/O</h3>
        <div class="section-content">
          <label>
            Probabilidad I/O (%)
            <input
              :value="Math.round(ioProbability * 100)"
              min="0"
              max="100"
              type="number"
              @input="emit('update:ioProbability', Number($event.target.value) / 100)"
            />
          </label>
          <label>
            Duración I/O (seg)
            <input
              :value="ioDuration"
              min="1"
              max="10"
              type="number"
              @input="emit('update:ioDuration', Number($event.target.value))"
            />
          </label>
          <label class="checkbox-label">
            <input
              type="checkbox"
              :checked="autoIO"
              @change="emit('update:autoIO', $event.target.checked)"
            />
            I/O Automático
          </label>
        </div>
      </div>

      <div class="section">
        <h3>Control de Ejecución</h3>
        <div class="buttons" :class="{ 'disabled': automaticMode }">
          <button 
            class="primary" 
            @click="emit('start')"
            :disabled="automaticMode"
            :title="automaticMode ? 'Deshabilitado en modo automático' : 'Iniciar simulación'"
          >
            ▶️ Iniciar
          </button>
          <button 
            @click="emit('pause')"
            :disabled="automaticMode"
            :title="automaticMode ? 'Deshabilitado en modo automático' : 'Pausar simulación'"
          >
            ⏸️ Pausar
          </button>
          <button 
            @click="emit('resume')"
            :disabled="automaticMode"
            :title="automaticMode ? 'Deshabilitado en modo automático' : 'Reanudar simulación'"
          >
            ▶️ Reanudar
          </button>
          <button 
            class="danger" 
            @click="emit('stop')"
            :disabled="automaticMode"
            :title="automaticMode ? 'Deshabilitado en modo automático' : 'Detener simulación'"
          >
            ⏹️ Detener
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.panel {
  background: #fff;
  border-radius: 12px;
  padding: 1.5rem;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  overflow: hidden;
}

header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.25rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid #e5e7eb;
  flex-wrap: wrap;
  gap: 0.75rem;
}

@media (max-width: 768px) {
  header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .header-right {
    width: 100%;
    flex-wrap: wrap;
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.status {
  font-weight: 600;
  color: #2563eb;
  font-size: 0.9rem;
  padding: 0.4rem 0.8rem;
  background: #eff6ff;
  border-radius: 6px;
}

.mode-toggle {
  padding: 0.5rem 1rem;
  border: 2px solid #2563eb;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 0.85rem;
  white-space: nowrap;
}

.mode-toggle.automatic {
  background: #2563eb;
  color: white;
}

.mode-toggle.manual {
  background: white;
  color: #2563eb;
}

.mode-toggle:hover {
  opacity: 0.85;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(37, 99, 235, 0.2);
}

.controls {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.section {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 1rem;
  background: #f9fafb;
}

.section h3 {
  margin: 0 0 0.75rem 0;
  font-size: 0.85rem;
  font-weight: 700;
  color: #374151;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.section-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 0.75rem;
  align-items: end;
}

@media (max-width: 768px) {
  .section-content {
    grid-template-columns: 1fr;
  }
  
  label.checkbox-label {
    grid-column: span 1;
  }
}

label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #374151;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

label.checkbox-label {
  flex-direction: row;
  align-items: center;
  gap: 0.5rem;
  grid-column: span 2;
}

label.checkbox-label input[type="checkbox"] {
  width: auto;
  margin: 0;
  cursor: pointer;
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
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  justify-content: flex-start;
}

@media (max-width: 768px) {
  .buttons {
    flex-direction: column;
  }
  
  .buttons button {
    width: 100%;
    justify-content: center;
  }
}

button {
  border: none;
  border-radius: 6px;
  padding: 0.6rem 1.2rem;
  font-weight: 600;
  cursor: pointer;
  background: #e5e7eb;
  color: #374151;
  font-size: 0.9rem;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

button.primary {
  background: #2563eb;
  color: #fff;
}

button.primary:hover:not(:disabled) {
  background: #1d4ed8;
}

button.danger {
  background: #dc2626;
  color: #fff;
}

button.danger:hover:not(:disabled) {
  background: #b91c1c;
}

.buttons.disabled button {
  opacity: 0.4;
  cursor: not-allowed;
  pointer-events: none;
}

button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* Responsive: Pantallas pequeñas */
@media (max-width: 480px) {
  .panel {
    padding: 1rem;
  }
  
  header h2 {
    font-size: 0.95rem;
  }
  
  .status {
    font-size: 0.8rem;
    padding: 0.3rem 0.6rem;
  }
  
  .mode-toggle {
    font-size: 0.8rem;
    padding: 0.4rem 0.8rem;
  }
  
  .section {
    padding: 0.75rem;
  }
  
  .section h3 {
    font-size: 0.8rem;
  }
  
  label {
    font-size: 0.8rem;
  }
  
  select,
  input {
    font-size: 0.85rem;
    padding: 0.45rem 0.65rem;
  }
  
  button {
    font-size: 0.85rem;
    padding: 0.5rem 1rem;
  }
}
</style>

