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
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.08);
}

header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  gap: 1rem;
}

.timeline {
  display: flex;
  width: 100%;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  overflow-x: auto;
  overflow-y: hidden;
}

.slice {
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  color: #fff;
  padding: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  border-right: 1px solid #fff;
  min-width: 120px;
}

.slice:nth-child(2n) {
  background: linear-gradient(135deg, #059669, #0ea5e9);
}

small {
  font-size: 0.75rem;
  opacity: 0.85;
}
</style>

