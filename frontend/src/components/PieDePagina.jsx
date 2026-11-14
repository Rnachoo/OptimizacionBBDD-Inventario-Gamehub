import logo from '../assets/logo.png'

export default function PieDePagina() {
  return (
    <footer className="footer">
      <div className="contenedor">
        <p>GameHub Store - Proyecto Académico, Taller de Base de Datos (BDY1103)</p>
        <p>© 2026 GameHub Store. Todos los derechos reservados.</p>
        <img src={logo} alt="Logo de GameHub Store" className="logo-footer" />
      </div>
    </footer>
  )
}
