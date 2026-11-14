import Cabecera from './components/Cabecera'
import PieDePagina from './components/PieDePagina'
import Inventario from './pages/Inventario'

function App() {
  return (
    <>
      <Cabecera />
      <main>
        <Inventario />
      </main>
      <PieDePagina />
    </>
  )
}

export default App
