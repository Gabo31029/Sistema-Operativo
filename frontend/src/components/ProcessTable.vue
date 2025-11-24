<script setup>
defineProps({
  processes: {
    type: Array,
    default: () => [],
  },
})
</script>

<template>
  <section class="panel">
    <h2>Tabla de procesos</h2>
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
        <tr v-for="process in processes" :key="process.pid">
          <td>{{ process.pid }}</td>
          <td>{{ process.name }}</td>
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
  </section>
</template>

<style scoped>
.panel {
  background: #fff;
  border-radius: 12px;
  padding: 1.5rem;
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
</style>

