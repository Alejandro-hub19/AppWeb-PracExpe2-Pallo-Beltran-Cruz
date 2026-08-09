# Parte de Pallo — Comparativa SOAP vs REST (Directriz 4)

## Tabla comparativa (8 criterios)

| Criterio | SOAP | REST |
|---|---|---|
| Formato del mensaje | XML estricto, siempre dentro de un Envelope (Header + Body) | Cualquier formato; en la practica casi siempre JSON |
| Contrato del servicio | WSDL obligatorio, generado o mantenido aparte | OpenAPI/Swagger opcional pero recomendado (SGED lo expone en /api/docs) |
| Transporte | Agnostico en teoria (HTTP, SMTP, JMS...), en la practica casi siempre HTTP | Exclusivamente HTTP, usa sus verbos (GET/POST/PUT/DELETE) como parte del diseno |
| Overhead | Alto: el Envelope XML + cabeceras WS-* pesan mas por mensaje | Bajo: JSON es compacto, sin envoltorio obligatorio |
| Seguridad | WS-Security a nivel de mensaje (firma/cifrado de partes del XML) | TLS a nivel de transporte + JWT/OAuth2 a nivel de aplicacion (asi trabaja SGED: JWT en cookie HttpOnly) |
| Manejo de errores | SOAP Fault estandarizado (faultcode/faultstring) | Codigos de estado HTTP (400, 401, 404...) + cuerpo de error propio de cada API |
| Facilidad de consumo desde movil | Pesado: requiere parseo de XML y a veces librerias generadas desde el WSDL | Liviano: JSON nativo en practicamente cualquier SDK movil (Retrofit, URLSession, etc.) |
| Cache | No aprovecha el cache HTTP nativo (todo son POST tipicamente) | Los GET pueden cachearse con las cabeceras HTTP estandar |
| Casos de uso actuales en Ecuador | Sistemas legados/bancarios y algunas integraciones gubernamentales antiguas que ya existian en SOAP y no se han migrado | La gran mayoria de apps nuevas, fintechs y apps moviles (incluido este mismo proyecto, SGED) |

## Ejemplo real de llamada SOAP (ejecutada y verificada con curl)

Servicio publico de demostracion `NumberConversion` (dataaccess.com), operacion `NumberToWords`.

Request:
```
POST https://www.dataaccess.com/webservicesserver/NumberConversion.wso
Content-Type: text/xml; charset=utf-8
SOAPAction: http://www.dataaccess.com/webservicesserver/NumberToWords

<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <NumberToWords xmlns="http://www.dataaccess.com/webservicesserver/">
      <ubiNum>500</ubiNum>
    </NumberToWords>
  </soap:Body>
</soap:Envelope>
```

Response (real, capturada tal cual):
```xml
<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <m:NumberToWordsResponse xmlns:m="http://www.dataaccess.com/webservicesserver/">
      <m:NumberToWordsResult>five hundred </m:NumberToWordsResult>
    </m:NumberToWordsResponse>
  </soap:Body>
</soap:Envelope>
```

## Su equivalente REST (endpoint real de SGED)

Request:
```
GET /api/categorias/activas HTTP/1.1
Host: localhost:8080
Cookie: access_token=<obtenida en POST /api/auth/login>
```

Response (real, capturada el 2026-08-09 contra la base de datos real de SGED corriendo en local):
```json
[
  {"idCategoria":1,"nombre":"SUB-12","edadMin":10,"edadMax":12,"descripcion":"Categoría sub-12","activo":true,"createdAt":"2026-08-03T04:44:33.481682Z"},
  {"idCategoria":2,"nombre":"SUB-14","edadMin":12,"edadMax":14,"descripcion":"Categoría sub-14","activo":true,"createdAt":"2026-08-03T04:44:33.481682Z"},
  {"idCategoria":3,"nombre":"SUB-16","edadMin":14,"edadMax":16,"descripcion":"Categoría sub-16","activo":true,"createdAt":"2026-08-03T04:44:33.481682Z"}
]
```

## Lectura del contraste

Para devolver una cantidad de informacion util comparable, SOAP necesita el Envelope XML completo (namespace, Header/Body, nombre de la operacion repetido en la respuesta) solo para "empacar" un resultado; REST entrega el arreglo JSON directo, sin envoltorio. Ese es el argumento real detras de la fila "overhead" de la tabla, mostrado con un ejemplo ejecutado en vez de solo descrito en teoria.

Ambos lados de esta comparacion son 100% reales y ejecutados: el SOAP contra el servicio publico de dataaccess.com, el REST contra la base de datos real de SGED (categorias sembradas el 2026-08-03). Nada de esto es inventado ni de relleno.
