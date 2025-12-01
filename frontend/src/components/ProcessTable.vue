<script setup>
defineProps({
  processes: {
    type: Array,
    default: () => [],
  },
})

defineEmits(['clear-all'])

// Función para generar un color único basado en el PID
function getProcessColor(pid) {
  // Paleta de colores suaves y distinguibles
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
    '#06b6d4', // Cian
    '#a855f7', // Violeta
  ]
  
  // Usar el PID para seleccionar un color de forma consistente
  return colors[pid % colors.length]
}

// Función para obtener el color de fondo más claro para la fila
function getProcessBackgroundColor(pid) {
  const baseColor = getProcessColor(pid)
  // Convertir hex a RGB y aplicar opacidad
  const hex = baseColor.replace('#', '')
  const r = parseInt(hex.substr(0, 2), 16)
  const g = parseInt(hex.substr(2, 2), 16)
  const b = parseInt(hex.substr(4, 2), 16)
  return `rgba(${r}, ${g}, ${b}, 0.08)`
}

// Función para obtener el color del borde
function getProcessBorderColor(pid) {
  const baseColor = getProcessColor(pid)
  const hex = baseColor.replace('#', '')
  const r = parseInt(hex.substr(0, 2), 16)
  const g = parseInt(hex.substr(2, 2), 16)
  const b = parseInt(hex.substr(4, 2), 16)
  return `rgba(${r}, ${g}, ${b}, 0.2)`
}
</script>

<template>
  <section class="panel">
    <div class="panel-header">
      <h2>Tabla de procesos</h2>
      <button class="btn-clear" @click="$emit('clear-all')" title="Limpiar todos los procesos">
        🗑️ Limpiar Todo
      </button>
    </div>
    <div class="table-container">
      <table>
      <thead>
        <tr>
          <th>PID</th>
          <th>Nombre</th>
          <th>Estado</th>
          <th>Llegada</th>
          <th>Ráfaga</th>
          <th>Restante</th>
          <th>Prioridad</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="!processes.length">
          <td colspan="7">Sin procesos creados</td>
        </tr>
        <tr 
          v-for="process in processes" 
          :key="process.pid"
          :style="{
            backgroundColor: getProcessBackgroundColor(process.pid),
            borderLeft: `3px solid ${getProcessColor(process.pid)}`
          }"
          class="process-row"
        >
          <td>
            <span 
              class="pid-badge" 
              :style="{ backgroundColor: getProcessColor(process.pid) }"
            >
              {{ process.pid }}
            </span>
          </td>
          <td>
            <span 
              class="process-name" 
              :style="{ color: getProcessColor(process.pid) }"
            >
              {{ process.name }}
            </span>
          </td>
          <td>
            <span class="state" :data-state="process.state">
              {{ process.state }}
            </span>
          </td>
          <td>{{ process.arrivalTime }}</td>
          <td>{{ process.burstTime }}</td>
          <td>
            <span class="remaining-time" :class="{ 'running': process.state === 'RUNNING' }">
              {{ process.remainingTime }}
            </span>
          </td>
          <td>{{ process.priority }}</td>
        </tr>
      </tbody>
    </table>
    </div>
  </section>
</template>

<style scoped>
.panel {
  background: #fff;
  border-radius: 12px;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.panel-header h2 {
  margin: 0;
}

.btn-clear {
  padding: 0.5rem 1rem;
  background: #dc3545;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 500;
  transition: background 0.2s;
}

.btn-clear:hover {
  background: #c82333;
}

.table-container {
  flex: 1;
  overflow-y: auto;
  overflow-x: auto;
  max-height: calc(100vh - 300px);
  min-height: 200px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

@media (max-width: 968px) {
  .panel {
    width: 100%;
    max-width: 100%;
    box-sizing: border-box;
    overflow: hidden;
  }
  
  .table-container {
    width: 100%;
    max-width: 100%;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
  
  table {
    min-width: max-content;
    width: auto;
  }
}

.table-container::-webkit-scrollbar {
  width: 8px;
}

.table-container::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.table-container::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 4px;
}

.table-container::-webkit-scrollbar-thumb:hover {
  background: #555;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  text-align: left;
  padding: 0.5rem;
  border-bottom: 1px solid #e5e7eb;
  white-space: nowrap;
}

@media (max-width: 768px) {
  th,
  td {
    padding: 0.4rem 0.35rem;
    font-size: 0.85rem;
  }
  
  th {
    font-size: 0.8rem;
  }
}

.process-row {
  transition: all 0.2s ease;
}

.process-row:hover {
  transform: translateX(2px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.pid-badge {
  display: inline-block;
  padding: 0.25rem 0.6rem;
  border-radius: 6px;
  color: white;
  font-weight: 600;
  font-size: 0.85rem;
  min-width: 2.5rem;
  text-align: center;
}

.process-name {
  font-weight: 600;
  font-size: 0.95rem;
}

.state {
  font-weight: 600;
  text-transform: capitalize;
}

.state[data-state='RUNNING'] {
  color: #16a34a;
}

.state[data-state='READY'] {
  color: #2563eb;
}

.state[data-state='WAITING'] {
  color: #f97316;
}

.state[data-state='TERMINATED'] {
  color: #6b7280;
}

.remaining-time {
  font-weight: 600;
  font-size: 1.1em;
}

.remaining-time.running {
  color: #16a34a;
  animation: pulse 1s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
}

/* Responsive: Pantallas pequeñas */
@media (max-width: 480px) {
  .panel {
    padding: 1rem;
  }
  
  .panel-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.75rem;
  }
  
  .panel-header h2 {
    font-size: 0.95rem;
  }
  
  .btn-clear {
    width: 100%;
    font-size: 0.85rem;
    padding: 0.45rem 0.9rem;
  }
  
  .table-container {
    max-height: calc(100vh - 250px);
    min-height: 150px;
  }
  
  table {
    min-width: 550px;
  }
  
  th,
  td {
    padding: 0.35rem 0.3rem;
    font-size: 0.8rem;
  }
  
  .pid-badge {
    font-size: 0.75rem;
    padding: 0.2rem 0.5rem;
    min-width: 2rem;
  }
  
  .process-name {
    font-size: 0.85rem;
  }
}
</style>

