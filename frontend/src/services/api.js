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
};

