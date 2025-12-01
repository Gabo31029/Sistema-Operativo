const defaultApiUrl = (() => {
  const { protocol, hostname } = window.location;
  return `${protocol}//${hostname}:8080/api`;
})();

const BASE_URL = import.meta.env.VITE_API_URL ?? defaultApiUrl;

async function request(path, options = {}) {
  const response = await fetch(`${BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers ?? {}),
    },
    ...options,
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || 'Error en la comunicación con el backend');
  }

  if (response.status === 204 || response.status === 202) {
    return null;
  }

  const contentType = response.headers.get('content-type') ?? '';
  if (!contentType.includes('application/json')) {
    return null;
  }

  return response.json();
}

export const osApi = {
  createProcess(payload) {
    return request('/processes', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },
  listProcesses() {
    return request('/processes');
  },
  clearAllProcesses() {
    return request('/processes', {
      method: 'DELETE',
    });
  },
  startSimulation(payload) {
    return request('/simulation/start', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },
  pauseSimulation() {
    return request('/simulation/pause', { method: 'POST' });
  },
  resumeSimulation() {
    return request('/simulation/resume', { method: 'POST' });
  },
  stopSimulation() {
    return request('/simulation/stop', { method: 'POST' });
  },
  getSystemState() {
    return request('/simulation/state');
  },
  getTimeline() {
    return request('/simulation/timeline');
  },
  emitInterruption(payload) {
    return request('/interruptions', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },
  initializeMemory(totalSize) {
    return request('/memory/initialize', {
      method: 'POST',
      body: JSON.stringify({ totalSize }),
    });
  },
  allocateMemory(processId, size, algorithm) {
    return request('/memory/allocate', {
      method: 'POST',
      body: JSON.stringify({ processId, size, algorithm }),
    });
  },
  deallocateMemory(processId) {
    return request(`/memory/deallocate/${processId}`, {
      method: 'POST',
    });
  },
  getMemoryState() {
    return request('/memory/state');
  },
  setMemoryAlgorithm(algorithm) {
    return request('/memory/algorithm', {
      method: 'PUT',
      body: JSON.stringify({ algorithm: algorithm }),
    });
  },
  setIOSettings(ioProbability, ioDuration, autoIO) {
    return request('/simulation/io-settings', {
      method: 'POST',
      body: JSON.stringify({
        ioProbability,
        ioDurationSeconds: ioDuration,
        autoIOEnabled: autoIO,
      }),
    });
  },
  setMode(automatic) {
    return request('/simulation/mode', {
      method: 'POST',
      body: JSON.stringify({ automatic }),
    });
  },
  getMode() {
    return request('/simulation/mode');
  },
};

