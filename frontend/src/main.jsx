import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import 'bootstrap/dist/css/bootstrap.min.css'
import './styles/tema.css'
import './styles/base.css'
import './styles/encabezado-pie.css'
import './styles/componentes-gamehub.css'
import './styles/bootstrap-personalizado.css'
import 'bootstrap/dist/js/bootstrap.bundle.min.js'
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>
)
