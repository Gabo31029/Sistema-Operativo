<script setup>
defineProps({
  state: {
    type: Object,
    required: true,
  },
})
</script>

<template>
  <section class="panel">
    <h2>Colas del sistema</h2>
    <div class="queues">
      <div class="queue-column">
        <h3>Ready</h3>
        <div class="queue-container">
          <ul>
            <li v-for="p in state.readyQueue" :key="p.pid">
              {{ p.name }} (PID {{ p.pid }})
            </li>
            <li v-if="!state.readyQueue?.length" class="empty">Vacío</li>
          </ul>
        </div>
      </div>
      <div class="queue-column">
        <h3>Waiting</h3>
        <div class="queue-container">
          <ul>
            <li v-for="p in state.waitingQueue" :key="p.pid">
              {{ p.name }} (PID {{ p.pid }})
            </li>
            <li v-if="!state.waitingQueue?.length" class="empty">Vacío</li>
          </ul>
        </div>
      </div>
      <div class="queue-column">
        <h3>Terminated</h3>
        <div class="queue-container">
          <ul>
            <li v-for="p in state.terminatedQueue" :key="p.pid">
              {{ p.name }} (PID {{ p.pid }})
            </li>
            <li v-if="!state.terminatedQueue?.length" class="empty">Vacío</li>
          </ul>
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
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  overflow: hidden;
}

.panel h2 {
  margin: 0 0 1rem 0;
  font-size: 1.1rem;
  font-weight: 700;
  color: #1f2937;
}

.queues {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 1rem;
  flex: 1;
  min-height: 0;
}

@media (max-width: 768px) {
  .queues {
    grid-template-columns: 1fr;
    gap: 0.75rem;
  }
}

.queue-column {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.queue-column h3 {
  margin: 0 0 0.5rem 0;
  font-size: 0.9rem;
  font-weight: 700;
  color: #374151;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.queue-container {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  max-height: calc(100vh - 300px);
  min-height: 100px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 0.5rem;
  background: #f9fafb;
}

.queue-container::-webkit-scrollbar {
  width: 6px;
}

.queue-container::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.queue-container::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 3px;
}

.queue-container::-webkit-scrollbar-thumb:hover {
  background: #555;
}

ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

li {
  padding: 0.4rem 0.6rem;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 0.85rem;
  color: #374151;
  transition: background 0.2s;
}

li:hover:not(.empty) {
  background: #f3f4f6;
}

li.empty {
  color: #9ca3af;
  font-style: italic;
  text-align: center;
  background: transparent;
  border: none;
}

/* Responsive: Pantallas pequeñas */
@media (max-width: 480px) {
  .panel {
    padding: 1rem;
  }
  
  .panel h2 {
    font-size: 0.95rem;
  }
  
  .queue-column h3 {
    font-size: 0.85rem;
  }
  
  .queue-container {
    max-height: calc(100vh - 250px);
    min-height: 80px;
  }
  
  li {
    font-size: 0.8rem;
    padding: 0.35rem 0.5rem;
  }
}
</style>

