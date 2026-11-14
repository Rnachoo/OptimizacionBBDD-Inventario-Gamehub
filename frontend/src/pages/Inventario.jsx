import { Fragment, useEffect, useState } from 'react'
import {
  actualizarStockMinimo,
  eliminarInventario,
  obtenerInventarios,
  registrarMovimiento,
  reponerStockBajo
} from '../services/inventario'
import { NOMBRES_PRODUCTOS } from '../catalogo/nombres-productos'
import '../styles/inventario.css'

// Un inventario está en alerta cuando el stock disponible llega al mínimo o baja de él
const esStockBajo = (inventario) => inventario.stockDisponible <= inventario.stockMinimo

// El inventario no trae nombres, solo el id del producto
const nombreProducto = (productoId) => NOMBRES_PRODUCTOS[productoId] ?? 'Producto #' + productoId

const formatearFecha = (fecha) =>
  new Date(fecha).toLocaleString('es-CL', { dateStyle: 'short', timeStyle: 'short' })

function PanelInventario({ inventario, ocupado, onMovimiento, onMinimo }) {
  const [tipo, setTipo] = useState('ENTRADA')
  const [cantidad, setCantidad] = useState('')
  const [minimo, setMinimo] = useState(inventario.stockMinimo)
  const id = inventario.id

  async function manejarMovimiento(evento) {
    evento.preventDefault()
    const registrado = await onMovimiento(id, tipo, Number(cantidad))
    if (registrado) {
      setCantidad('')
    }
  }

  function manejarMinimo(evento) {
    evento.preventDefault()
    onMinimo(id, Number(minimo))
  }

  return (
    <div className="panel-inventario">
      <div className="forms-inventario">
        <form onSubmit={manejarMovimiento} aria-labelledby={'titulo-movimiento-' + id}>
          <h3 id={'titulo-movimiento-' + id}>Registrar movimiento</h3>
          <div className="fila-formulario">
            <div className="campo-formulario">
              <label htmlFor={'tipo-' + id}>Tipo</label>
              <select id={'tipo-' + id} value={tipo} onChange={(e) => setTipo(e.target.value)}>
                <option value="ENTRADA">Entrada</option>
                <option value="SALIDA">Salida</option>
              </select>
            </div>
            <div className="campo-formulario">
              <label htmlFor={'cantidad-' + id}>Cantidad</label>
              <input
                id={'cantidad-' + id}
                type="number"
                min="1"
                required
                value={cantidad}
                onChange={(e) => setCantidad(e.target.value)}
              />
            </div>
            <button type="submit" className="boton-primario" disabled={ocupado}>
              Registrar
            </button>
          </div>
        </form>

        <form onSubmit={manejarMinimo} aria-labelledby={'titulo-minimo-' + id}>
          <h3 id={'titulo-minimo-' + id}>Stock mínimo</h3>
          <div className="fila-formulario">
            <div className="campo-formulario">
              <label htmlFor={'minimo-' + id}>Unidades</label>
              <input
                id={'minimo-' + id}
                type="number"
                min="0"
                required
                value={minimo}
                onChange={(e) => setMinimo(e.target.value)}
              />
            </div>
            <button type="submit" className="boton-secundario" disabled={ocupado}>
              Guardar mínimo
            </button>
          </div>
        </form>
      </div>

      <div>
        <h3>Historial de movimientos</h3>
        {inventario.movimientosDTO?.length ? (
          <table className="table tabla-inventario tabla-historial">
            <thead>
              <tr>
                <th scope="col">Fecha</th>
                <th scope="col">Tipo</th>
                <th scope="col" className="text-end">Cantidad</th>
              </tr>
            </thead>
            <tbody>
              {inventario.movimientosDTO.map((movimiento, indice) => (
                <tr key={movimiento.id ?? indice}>
                  <td>{formatearFecha(movimiento.fecha)}</td>
                  <td>{movimiento.tipo === 'ENTRADA' ? 'Entrada' : 'Salida'}</td>
                  <td className="text-end">{movimiento.cantidad}</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <p>Este producto aún no tiene movimientos.</p>
        )}
      </div>
    </div>
  )
}

export default function Inventario() {
  const [inventarios, setInventarios] = useState([])
  const [cargando, setCargando] = useState(true)
  const [ocupado, setOcupado] = useState(false)
  const [aviso, setAviso] = useState(null) // { exito: boolean, texto: string }
  const [abierto, setAbierto] = useState(null) // id del inventario con el panel desplegado

  function avisoErrorCarga(error) {
    return {
      exito: false,
      texto:
        'No se pudo cargar el inventario: ' +
        error.message +
        '. Revisa que el microservicio esté corriendo en el puerto 8005.'
    }
  }

  // Botón "Actualizar"
  async function cargar() {
    setCargando(true)
    try {
      setInventarios(await obtenerInventarios())
    } catch (error) {
      setAviso(avisoErrorCarga(error))
    } finally {
      setCargando(false)
    }
  }

  // Carga inicial (el estado "cargando" ya parte en true)
  useEffect(() => {
    let activo = true

    obtenerInventarios()
      .then((lista) => {
        if (activo) setInventarios(lista)
      })
      .catch((error) => {
        if (activo) setAviso(avisoErrorCarga(error))
      })
      .finally(() => {
        if (activo) setCargando(false)
      })

    return () => {
      activo = false
    }
  }, [])

  // Ejecuta una acción, muestra el resultado y recarga la tabla. Devuelve true si salió bien.
  async function ejecutar(accion, mensajeExito) {
    setOcupado(true)
    setAviso(null)
    try {
      const resultado = await accion()
      const texto = typeof mensajeExito === 'function' ? mensajeExito(resultado) : mensajeExito
      setAviso({ exito: true, texto })
      setInventarios(await obtenerInventarios())
      return true
    } catch (error) {
      setAviso({ exito: false, texto: error.message })
      return false
    } finally {
      setOcupado(false)
    }
  }

  function manejarMovimiento(id, tipo, cantidad) {
    const nombre = tipo === 'ENTRADA' ? 'Entrada' : 'Salida'
    return ejecutar(
      () => registrarMovimiento(id, tipo, cantidad),
      nombre + ' de ' + cantidad + ' unidades registrada.'
    )
  }

  function manejarMinimo(id, minimo) {
    return ejecutar(() => actualizarStockMinimo(id, minimo), 'Stock mínimo actualizado.')
  }

  function manejarReposicion() {
    return ejecutar(reponerStockBajo, (repuestos) =>
      repuestos.length
        ? 'Se repusieron ' + repuestos.length + ' productos con stock bajo.'
        : 'No hay productos con stock bajo para reponer.'
    )
  }

  function manejarEliminar(inventario) {
    const nombre = nombreProducto(inventario.productId)
    if (!window.confirm('¿Eliminar el inventario de "' + nombre + '"? Esta acción no se puede deshacer.')) {
      return
    }
    ejecutar(() => eliminarInventario(inventario.id), 'Inventario eliminado.')
  }

  const totalBajos = inventarios.filter(esStockBajo).length

  return (
    <div className="contenedor">
      <nav className="ruta-navegacion" aria-label="Ruta de navegación">
        GameHub &gt; <span aria-current="page">Inventario</span>
      </nav>

      <section className="seccion-inventario" aria-labelledby="titulo-inventario">
        <div className="cabecera-inventario">
          <div>
            <h1 id="titulo-inventario">Inventario</h1>
            {!cargando && (
              <p>
                {inventarios.length} productos, {totalBajos} con stock bajo.
              </p>
            )}
          </div>
          <div className="acciones-inventario">
            <button type="button" className="boton-secundario" onClick={cargar} disabled={cargando || ocupado}>
              Actualizar
            </button>
            <button
              type="button"
              className="boton-primario"
              onClick={manejarReposicion}
              disabled={cargando || ocupado || totalBajos === 0}
            >
              Reponer stock bajo
            </button>
          </div>
        </div>

        {aviso && (
          <p className={aviso.exito ? 'aviso-inventario aviso-exito' : 'aviso-inventario mensaje-error'} role="alert">
            {aviso.texto}
          </p>
        )}

        {cargando && <p>Cargando inventario...</p>}

        {!cargando && inventarios.length === 0 && (
          <div className="sin-ordenes">
            <p>Todavía no hay inventarios registrados.</p>
          </div>
        )}

        {!cargando && inventarios.length > 0 && (
          <div className="table-responsive">
            <table className="table tabla-inventario align-middle" aria-label="Inventario de productos">
              <thead>
                <tr>
                  <th scope="col">Producto</th>
                  <th scope="col">Ubicación</th>
                  <th scope="col" className="text-end">Disponible</th>
                  <th scope="col" className="text-end">Reservado</th>
                  <th scope="col" className="text-end">Mínimo</th>
                  <th scope="col">Estado</th>
                  <th scope="col">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {inventarios.map((inventario) => {
                  const bajo = esStockBajo(inventario)
                  const desplegado = abierto === inventario.id
                  return (
                    <Fragment key={inventario.id}>
                      <tr className={bajo ? 'fila-stock-bajo' : ''}>
                        <td>{nombreProducto(inventario.productId)}</td>
                        <td>{inventario.ubicacion}</td>
                        <td className="text-end">{inventario.stockDisponible}</td>
                        <td className="text-end">{inventario.stockReservado}</td>
                        <td className="text-end">{inventario.stockMinimo}</td>
                        <td>
                          <span className={'estado-orden ' + (bajo ? 'estado-bajo' : 'estado-entregado')}>
                            {bajo ? 'Stock bajo' : 'Normal'}
                          </span>
                        </td>
                        <td className="acciones-fila">
                          <button
                            type="button"
                            className="boton-secundario"
                            aria-expanded={desplegado}
                            onClick={() => setAbierto(desplegado ? null : inventario.id)}
                          >
                            {desplegado ? 'Cerrar' : 'Gestionar'}
                          </button>
                          <button
                            type="button"
                            className="boton-secundario boton-peligro"
                            onClick={() => manejarEliminar(inventario)}
                            disabled={ocupado}
                          >
                            Eliminar
                          </button>
                        </td>
                      </tr>

                      {desplegado && (
                        <tr>
                          <td colSpan={7} className="celda-panel">
                            <PanelInventario
                              inventario={inventario}
                              ocupado={ocupado}
                              onMovimiento={manejarMovimiento}
                              onMinimo={manejarMinimo}
                            />
                          </td>
                        </tr>
                      )}
                    </Fragment>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  )
}
