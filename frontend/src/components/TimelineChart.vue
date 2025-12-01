<script setup>
import { computed } from 'vue'

const props = defineProps({
  timeline: {
    type: Array,
    default: () => [],
  },
})

const totalTime = computed(() => {
  if (!props.timeline.length) return 0
  return Math.max(...props.timeline.map((slice) => slice.end))
})

// Función para generar un color único basado en el PID (misma que ProcessTable)
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

// Función para obtener un gradiente basado en el color del proceso
function getSliceStyle(pid) {
  const baseColor = getProcessColor(pid)
  
  // Convertir hex a RGB para crear un gradiente más oscuro
  const hex = baseColor.replace('#', '')
  const r = parseInt(hex.substr(0, 2), 16)
  const g = parseInt(hex.substr(2, 2), 16)
  const b = parseInt(hex.substr(4, 2), 16)
  
  // Crear un color más oscuro para el gradiente
  const darkerR = Math.max(0, r - 30)
  const darkerG = Math.max(0, g - 30)
  const darkerB = Math.max(0, b - 30)
  const darkerColor = `rgb(${darkerR}, ${darkerG}, ${darkerB})`
  
  return {
    background: `linear-gradient(135deg, ${baseColor}, ${darkerColor})`,
  }
}
</script>

<template>
  <section class="panel">
    <header>
      <h2>Timeline / Gantt</h2>
      <span v-if="totalTime">Duración total: {{ totalTime }} unidades</span>
    </header>
    <div class="timeline" v-if="timeline.length">
      <div
        v-for="slice in timeline"
        :key="`${slice.pid}-${slice.start}`"
        class="slice"
        :style="{
          width: totalTime ? `${((slice.end - slice.start) / totalTime) * 100}%` : '0',
          ...getSliceStyle(slice.pid),
        }"
      >
        <span>{{ slice.name }} ({{ slice.algorithm }})</span>
        <small>{{ slice.start }} - {{ slice.end }}</small>
      </div>
    </div>
    <p v-else>Ejecuta una simulación para ver el cronograma.</p>
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
  margin-bottom: 1rem;
  gap: 1rem;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  header {
    flex-direction: column;
    align-items: flex-start;
  }
}

.timeline {
  display: flex;
  width: 100%;
  max-width: 100%;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  overflow-x: auto;
  overflow-y: hidden;
  box-sizing: border-box;
  -webkit-overflow-scrolling: touch;
}

.slice {
  color: #fff;
  padding: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  border-right: 1px solid rgba(255, 255, 255, 0.3);
  min-width: 120px;
  flex-shrink: 0;
  /* El background se aplica dinámicamente via :style */
}

@media (max-width: 768px) {
  .slice {
    min-width: 100px;
    padding: 0.6rem 0.5rem;
    font-size: 0.85rem;
  }
  
  .slice small {
    font-size: 0.7rem;
  }
}

@media (max-width: 480px) {
  .slice {
    min-width: 80px;
    padding: 0.5rem 0.4rem;
    font-size: 0.8rem;
  }
  
  .slice small {
    font-size: 0.65rem;
  }
}

/* Los colores se aplican dinámicamente basados en el PID del proceso */

small {
  font-size: 0.75rem;
  opacity: 0.85;
}

/* Responsive: Pantallas pequeñas */
@media (max-width: 480px) {
  .panel {
    padding: 1rem;
  }
  
  header h2 {
    font-size: 0.95rem;
  }
  
  .timeline {
    border-radius: 8px;
  }
}
</style>

