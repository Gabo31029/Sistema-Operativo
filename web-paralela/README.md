# Aplicación Web Paralela - Generador de Procesos

Esta es una aplicación web simple que simula procesos siendo generados y enviados al panel de control del Sistema Operativo.

## Características

- **Envío manual de procesos**: Crea y envía procesos individuales con parámetros personalizados
- **Modo automático**: Genera procesos aleatoriamente cada X segundos
- **Estadísticas en tiempo real**: Muestra cantidad de procesos enviados, exitosos y errores
- **Historial de procesos**: Log de todos los procesos enviados con timestamps
- **Asignación de memoria**: Permite especificar tamaño de memoria para cada proceso

## Cómo usar

### Opción 1: Abrir directamente en el navegador

1. Asegúrate de que el backend esté corriendo (`mvn spring-boot:run` en la carpeta `backend`)
2. Abre `index.html` en tu navegador (doble clic o arrastra al navegador)
3. La aplicación se conectará automáticamente al backend

### Opción 2: Usar un servidor local (recomendado)

Si tienes Python instalado:

```bash
cd web-paralela
python -m http.server 3000
```

Luego abre: `http://localhost:3000`

O con Node.js (si tienes `http-server` instalado):

```bash
cd web-paralela
npx http-server -p 3000
```

## Funcionalidades

### Envío Manual
1. Completa los campos del formulario:
   - Nombre del proceso
   - Tiempo de llegada
   - Ráfaga de CPU
   - Prioridad (0-10)
   - Tamaño de memoria (opcional)
2. Haz clic en "Enviar Proceso Manual"

### Modo Automático
1. Haz clic en "Iniciar Modo Automático"
2. Los procesos se generarán automáticamente con valores aleatorios
3. Puedes ajustar el intervalo de generación
4. Los procesos incluyen nombres aleatorios y parámetros variados

## Integración con el Panel de Control

Los procesos enviados desde esta aplicación aparecerán automáticamente en:
- La tabla de procesos del panel de control
- Las colas del sistema (ready, waiting, terminated)
- El timeline cuando se ejecute una simulación
- El panel de memoria (si se especificó `memorySize`)

## Notas

- La aplicación intenta conectarse a `http://localhost:8080/api`
- Si el backend no está corriendo, verás un mensaje de error
- Los procesos con `memorySize` se asignarán automáticamente en el panel de memoria
- El modo automático genera procesos con nombres y parámetros aleatorios para simular una aplicación real

