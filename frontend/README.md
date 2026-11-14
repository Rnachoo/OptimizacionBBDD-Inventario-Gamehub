# Vista de Inventario (MVP)

Vista de React que consume el microservicio de inventario de este repositorio.

## Cómo correrla
1. Levanta el microservicio (puerto 8005). Con el perfil `dev` no pide token.
2. En esta carpeta: `npm install` y luego `npm run dev`.
3. Abre http://localhost:5173

El puerto 5173 es fijo porque el CORS del backend (`SecurityConfig`) solo permite 5173 y 3000.

## Nombres de producto
El inventario solo trae el `productId`. Los nombres salen de `src/catalogo/nombres-productos.js`;
si un id no está ahí, la tabla muestra "Producto #id".
