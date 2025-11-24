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
      <div>
        <h3>Ready</h3>
        <ul>
          <li v-for="p in state.readyQueue" :key="p.pid">
            {{ p.name }} (PID {{ p.pid }})
          </li>
          <li v-if="!state.readyQueue?.length">Vacío</li>
        </ul>
      </div>
      <div>
        <h3>Waiting</h3>
        <ul>
          <li v-for="p in state.waitingQueue" :key="p.pid">
            {{ p.name }} (PID {{ p.pid }})
          </li>
          <li v-if="!state.waitingQueue?.length">Vacío</li>
        </ul>
      </div>
      <div>
        <h3>Terminated</h3>
        <ul>
          <li v-for="p in state.terminatedQueue" :key="p.pid">
            {{ p.name }} (PID {{ p.pid }})
          </li>
          <li v-if="!state.terminatedQueue?.length">Vacío</li>
        </ul>
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

.queues {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 1rem;
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
  padding: 0.35rem 0.5rem;
  background: #f3f4f6;
  border-radius: 6px;
  font-size: 0.9rem;
}
</style>

