# DonaTrack API Gateway

Gateway Spring Cloud Gateway / Java 21. Es el punto de entrada para clientes externos; no cambia contratos de los módulos.

🧩 Link al despliegue en Render
- [https://api-gateway-donatrack.onrender.com](https://api-gateway-donatrack.onrender.com)

- Healht: https://api-gateway-donatrack.onrender.com/actuator/health

## Prefijos públicos

| Prefijo del gateway | Destino | Ejemplo |
|---|---|---|
| `/api/donaciones/**` | Donaciones | `/api/donaciones/productos` |
| `/api/donadores-entidades/**` | Donadores y Entidades | `/api/donadores-entidades/necesidades` |
| `/api/logistica/**` | Logística | `/api/logistica/stock/3` |
| `/api/incentivos/**` | Incentivos | `/api/incentivos/misiones` |
| `/api/consultas/**` | Servicio de Consultas | `/api/consultas/donadores/12` |

`StripPrefix=2` elimina `/api/{componente}` y conserva la ruta del módulo. Métodos HTTP, query params, body y headers se reenvían por Gateway. El tráfico interno entre módulos puede seguir directo como permite el instructivo.

## Despliegue en Render

Crear un Web Service desde este directorio/repo, runtime Docker. Configurar las cinco URL con los dominios reales de los servicios. Health check: `/actuator/health`. No hardcodear claves. La URL de consultas debe apuntar al servicio de consultas desplegado.

Para los servicios gratuitos de Render, contemplar cold starts. Gateway usa 5s de conexión y 45s de respuesta; aumentar el timeout si una operación legítima lo requiere. La URL pública de demo debe ser la del gateway.

## Métricas y Better Stack

- Prometheus: `/actuator/prometheus`
- Actuator: `/actuator/health`, `/actuator/metrics`, `/actuator/gateway/routes`
- Better Stack está integrado por `logback-logtail`: configurar `BETTERSTACK_SOURCE_TOKEN` y opcionalmente `BETTERSTACK_INGEST_URL`; el appender se activa solo si existe el token. La consola sigue activa como fallback. El `APP_NAME` permite identificar el servicio.

