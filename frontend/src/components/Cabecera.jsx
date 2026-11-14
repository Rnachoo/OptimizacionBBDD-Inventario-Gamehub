import logo from '../assets/logo.png'

export default function Cabecera() {
  return (
    <header className="encabezado">
      <div className="contenedor">
        <div className="marca-grupo">
          <img src={logo} alt="GameHub Store" className="logo-encabezado" />
        </div>
        <p className="mb-0">Panel de inventario</p>
      </div>
    </header>
  )
}
