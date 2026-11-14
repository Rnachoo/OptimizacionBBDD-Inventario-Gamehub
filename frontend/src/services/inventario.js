const URL_BASE =
  import.meta.env.VITE_INVENTARIO_URL || 'http://localhost:8005/api/v1/inventories'

// El backend responde los errores como {"mensaje": "..."} (reglas de negocio)
// o como {"campo": "mensaje"} (validaciones). Aquí se deja todo en un solo texto.
async function pedir(url, opciones = {}) {
  const respuesta = await fetch(url, {
    ...opciones,
    headers: { Accept: 'application/json', 'Content-Type': 'application/json' }
  })

  if (respuesta.status === 204) {
    return null
  }

  const cuerpo = await respuesta.json().catch(() => null)

  if (!respuesta.ok) {
    const texto = cuerpo
      ? cuerpo.mensaje ?? Object.values(cuerpo).join('. ')
      : 'El servicio de inventario respondió ' + respuesta.status
    throw new Error(texto)
  }

  return cuerpo
}

// La respuesta viene en formato HATEOAS: si no hay inventarios,
// Spring omite "_embedded", por eso se usa ?. y un arreglo vacío.
const extraerLista = (datos) => datos?._embedded?.inventoryDetalleDTOList ?? []

export async function obtenerInventarios() {
  return extraerLista(await pedir(URL_BASE))
}

// Registra una ENTRADA o SALIDA (la salida falla si no hay stock suficiente)
export function registrarMovimiento(id, tipo, cantidad) {
  return pedir(URL_BASE + '/' + id + '/movimientos', {
    method: 'POST',
    body: JSON.stringify({ tipo, cantidad })
  })
}

export function actualizarStockMinimo(id, stockMinimo) {
  return pedir(URL_BASE + '/' + id + '/minimo', {
    method: 'PATCH',
    body: JSON.stringify({ stockMinimo })
  })
}

// Repone todos los inventarios con stock bajo y devuelve los que repuso
export async function reponerStockBajo() {
  return extraerLista(await pedir(URL_BASE + '/reposicion', { method: 'POST' }))
}

export function eliminarInventario(id) {
  return pedir(URL_BASE + '/' + id, { method: 'DELETE' })
}
