package com.inventory.services;

import com.inventory.exceptions.InventoryException;
import com.inventory.models.Inventory;
import com.inventory.models.MovimientoInventario;
import com.inventory.models.dtos.*;
import com.inventory.repositories.InventoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class InventoryServiceImpl implements InventoryService {
    @Autowired
    private InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    @Override
    public List<InventoryDetalleDTO> findAllByProduct(Long productId) {
        log.info("Listando Inventario registrado por producto");
        return this.inventoryRepository.findByProductId(productId).stream().map(inventory -> {
            InventoryDetalleDTO dto = new InventoryDetalleDTO();
            dto.setId(inventory.getId());
            dto.setProductId(inventory.getProductId());
            dto.setStockDisponible(inventory.getStockDisponible());
            dto.setStockMinimo(inventory.getStockMinimo());
            dto.setStockReservado(inventory.getStockReservado());
            dto.setUbicacion(inventory.getUbicacion());

            List<MovimientoDetalleDTO> movimientoDTO = inventory.getMovimientos().stream().map(mov->{
                MovimientoDetalleDTO movDTO = new MovimientoDetalleDTO();
                movDTO.setProductId(mov.getProductId());
                movDTO.setTipo(mov.getTipo());
                movDTO.setCantidad(mov.getCantidad());
                movDTO.setFecha(mov.getFecha());
                return movDTO;
            }).toList();
            dto.setMovimientosDTO(movimientoDTO);
            return dto;
        }).toList();
    }


    @Transactional(readOnly = true)
    @Override
    public List<InventoryDetalleDTO> findAll() {
        log.info("Listando todos los inventarios");
        return this.inventoryRepository.findAll().stream().map(inventory -> {
            InventoryDetalleDTO dto = new InventoryDetalleDTO();
            dto.setId(inventory.getId());
            dto.setProductId(inventory.getProductId());
            dto.setStockDisponible(inventory.getStockDisponible());
            dto.setStockMinimo(inventory.getStockMinimo());
            dto.setStockReservado(inventory.getStockReservado());
            dto.setUbicacion(inventory.getUbicacion());
            dto.setMovimientosDTO(inventory.getMovimientos().stream().map(mov -> {
                MovimientoDetalleDTO m = new MovimientoDetalleDTO();
                m.setId(mov.getId());
                m.setProductId(mov.getProductId());
                m.setTipo(mov.getTipo());
                m.setCantidad(mov.getCantidad());
                m.setFecha(mov.getFecha());
                return m;
            }).toList());
            return dto;
        }).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public InventoryDetalleDTO findByID(Long id) {
        log.info("Buscando inventario registrado en el sistema!");
        Inventory inventory = this.inventoryRepository.findById(id).orElseThrow(
                ()-> new InventoryException("Inventario con ID "+ id + " no encontrado"));
        InventoryDetalleDTO dto = new InventoryDetalleDTO();
        dto.setId(inventory.getId());
        dto.setProductId(inventory.getProductId());
        dto.setStockDisponible(inventory.getStockDisponible());
        dto.setStockMinimo(inventory.getStockMinimo());
        dto.setStockReservado(inventory.getStockReservado());
        dto.setUbicacion(inventory.getUbicacion());

        List<MovimientoDetalleDTO> movimientoDTO = inventory.getMovimientos().stream().map(mov->{
            MovimientoDetalleDTO movDTO = new MovimientoDetalleDTO();
            movDTO.setProductId(mov.getProductId());
            movDTO.setTipo(mov.getTipo());
            movDTO.setCantidad(mov.getCantidad());
            movDTO.setFecha(mov.getFecha());
            return movDTO;
        }).toList();
        dto.setMovimientosDTO(movimientoDTO);
        return dto;
    }

    @Transactional
    @Override
    public InventoryDetalleDTO save(InventorySaveDTO inventorySaveDTO) {
        // Sin msvc-product en este repo: productId se trata como referencia logica (no se valida su existencia)
        log.info("Creando nuevo inventario para producto con id "+ inventorySaveDTO.getProductId());
        Inventory inventory = new Inventory();
        inventory.setProductId(inventorySaveDTO.getProductId());
        inventory.setStockDisponible(inventorySaveDTO.getStockDisponible());
        inventory.setStockReservado(inventorySaveDTO.getStockReservado());

        inventory.setStockMinimo(inventorySaveDTO.getStockMinimo());

        inventory.setUbicacion(inventorySaveDTO.getUbicacion());

        List<MovimientoDetalleDTO> movimientosEntrada = inventorySaveDTO.getMovimientosDTO() == null
                ? List.of() : inventorySaveDTO.getMovimientosDTO();
        List<MovimientoInventario> movimientoInventarios = movimientosEntrada.stream().map(movDTO ->{
            MovimientoInventario movimientoInventario = new MovimientoInventario();
            movimientoInventario.setProductId(movDTO.getProductId());
            movimientoInventario.setTipo(movDTO.getTipo());
            movimientoInventario.setCantidad(movDTO.getCantidad());
            movimientoInventario.setFecha(LocalDateTime.now());
            movimientoInventario.setInventario(inventory);
            return movimientoInventario;
        }).toList();
        inventory.setMovimientos(movimientoInventarios);

        Inventory inventorySave = this.inventoryRepository.save(inventory);
        log.info("Inventario con id "+inventorySave.getId()+" ha sido guardado con exito");

        InventoryDetalleDTO dto = new InventoryDetalleDTO();
        dto.setId(inventorySave.getId());
        dto.setProductId(inventorySave.getProductId());
        dto.setStockDisponible(inventorySave.getStockDisponible());
        dto.setStockMinimo(inventorySave.getStockMinimo());
        dto.setStockReservado(inventorySave.getStockReservado());
        dto.setUbicacion(inventorySave.getUbicacion());

        List<MovimientoDetalleDTO> movimientoDTO = inventorySave.getMovimientos().stream().map(mov->{
            MovimientoDetalleDTO movDTO = new MovimientoDetalleDTO();
            movDTO.setProductId(mov.getProductId());
            movDTO.setTipo(mov.getTipo());
            movDTO.setCantidad(mov.getCantidad());
            movDTO.setFecha(mov.getFecha());
            return movDTO;
        }).toList();
        dto.setMovimientosDTO(movimientoDTO);
        return dto;
    }


    @Transactional
    @Override
    public InventoryDetalleDTO updateCantidadDisponible(Long id, InventoryUpdateCantidadDisponibleDTO cantidadDisponibleDTO) {
        return this.inventoryRepository.findById(id).map(inventory -> {
            if (cantidadDisponibleDTO.getStockDisponible() < 0) {
                log.error("Intento de actualizar stock a un valor negativo en el inventario ID " + id);
                throw new InventoryException("El stock disponible no puede quedar en negativo.");
            }
            inventory.setStockDisponible(cantidadDisponibleDTO.getStockDisponible());

            inventory = this.inventoryRepository.save(inventory);
            log.info("Cantidad del stock actualizado con exito");

            InventoryDetalleDTO dto = new InventoryDetalleDTO();
            dto.setId(inventory.getId());
            dto.setProductId(inventory.getProductId());
            dto.setStockDisponible(inventory.getStockDisponible());
            dto.setStockMinimo(inventory.getStockMinimo());
            dto.setStockReservado(inventory.getStockReservado());
            dto.setUbicacion(inventory.getUbicacion());

            return dto;

        }).orElseThrow(
                () -> new InventoryException("Inventario no encontrado, no se puede actualizar el stock")
        );
    }

    @Transactional
    @Override
    public void deleteById(Long id) {
        Inventory inventory = inventoryRepository.findById(id).orElseThrow(
                ()-> new InventoryException("Inventario con ID " +id+" no encontrado"));

        if (inventory.getStockReservado() > 0){
            log.error("Intento de eliminación rechazado, el inventario con id "+ id +" tiene stock reservado");
            throw new InventoryException("No se puede eliminar el inventario dado que hay productos reservados");
        }
        inventoryRepository.delete(inventory);
        log.info("Inventario eliminado con exito!");

    }

    @Transactional
    @Override
    public InventoryDetalleDTO reservarStock(Long productId, InventoryCantidadDTO cantidadDTO){
        Long cantidad = cantidadDTO.getCantidad();
        Inventory inventory = this.inventoryRepository.findByProductId(productId).stream().findFirst().orElseThrow(
                ()-> new InventoryException("Inventario no encontrado para el producto con id" + productId));

        if(inventory.getStockDisponible() < cantidad){
            throw new InventoryException("Stock insufciente, Solicitado:"  + cantidad + ", Disponible:"  + inventory.getStockDisponible());
        }

        inventory.setStockDisponible(inventory.getStockDisponible() - cantidad);
        inventory.setStockReservado(inventory.getStockReservado() + cantidad);

        Inventory inventorySave = this.inventoryRepository.save(inventory);
        log.info("Reserva exitosa para el producto ID " + productId);
        return this.findByID(inventorySave.getId());

    }

    @Transactional
    @Override
    public InventoryDetalleDTO confirmarVenta(Long productId, InventoryCantidadDTO cantidadDTO){
        Long cantidad = cantidadDTO.getCantidad();

        log.info("Confirmando venta de unidades: "+cantidad+" de producto con id "+ productId);
        Inventory inventory = this.inventoryRepository.findByProductId(productId).stream().findFirst().orElseThrow(
                ()-> new InventoryException("Inventario no encontrado para el producto con id" + productId));
        if(inventory.getStockReservado() < cantidad){
            throw new InventoryException("Error: Se estan intentado confirmar mas productos de los reservados");
        }

        inventory.setStockReservado(inventory.getStockReservado() - cantidad);
        Inventory inventorySave = this.inventoryRepository.save(inventory);
        log.info("Venta confirmada para el producto con ID "+ productId);
        return this.findByID(inventorySave.getId());

    }

    @Transactional
    @Override
    public InventoryDetalleDTO liberarStock (Long productId, InventoryCantidadDTO cantidadDTO){
        Long cantidad = cantidadDTO.getCantidad();
        log.info("Liberando reserva de "+cantidad+" del producto con id "+ productId);

        Inventory inventory = this.inventoryRepository.findByProductId(productId).stream().findFirst()
                .orElseThrow(() -> new InventoryException("Inventario no encontrado para el producto con id " + productId));

        if (inventory.getStockReservado() < cantidad) {
            log.warn("Se intenta liberar mas stock del que hay reservado. Ajustando a la cantidad maxima en reserva.");
            cantidad = inventory.getStockReservado();
        }

        inventory.setStockReservado(inventory.getStockReservado() - cantidad);
        inventory.setStockDisponible(inventory.getStockDisponible() + cantidad);

        Inventory inventorySave = this.inventoryRepository.save(inventory);
        log.info("Stock liberado con exito para el producto ID " + productId);

        return this.findByID(inventorySave.getId());

    }

    @Transactional
    @Override
    public InventoryDetalleDTO registrarMovimiento(Long id, InventoryMovimientoDTO movimientoDTO) {
        String tipo = movimientoDTO.getTipo().trim().toUpperCase();
        Long cantidad = movimientoDTO.getCantidad();
        if (!tipo.equals("ENTRADA") && !tipo.equals("SALIDA")) {
            throw new InventoryException("El tipo de movimiento debe ser ENTRADA o SALIDA");
        }

        Inventory inventory = this.inventoryRepository.findById(id).orElseThrow(
                () -> new InventoryException("Inventario con ID " + id + " no encontrado"));

        if (tipo.equals("SALIDA")) {
            long disponibleAntes = inventory.getStockDisponible();
            // Una sola sentencia: descuenta solo si alcanza, sin ventana para la venta sin stock
            if (this.inventoryRepository.descontarSiHayStock(id, cantidad) == 0) {
                throw new InventoryException("Stock insuficiente, Solicitado:" + cantidad + ", Disponible:" + disponibleAntes);
            }
            // clearAutomatically dejó la entidad desactualizada: se vuelve a cargar
            inventory = this.inventoryRepository.findById(id).orElseThrow(
                    () -> new InventoryException("Inventario con ID " + id + " no encontrado"));
        } else {
            inventory.setStockDisponible(inventory.getStockDisponible() + cantidad);
        }

        agregarMovimiento(inventory, tipo, cantidad);
        this.inventoryRepository.save(inventory);
        log.info("Movimiento " + tipo + " de " + cantidad + " registrado en el inventario " + id);
        return this.findByID(id);
    }

    @Transactional
    @Override
    public InventoryDetalleDTO actualizarStockMinimo(Long id, InventoryMinimoDTO minimoDTO) {
        Inventory inventory = this.inventoryRepository.findById(id).orElseThrow(
                () -> new InventoryException("Inventario con ID " + id + " no encontrado"));
        inventory.setStockMinimo(minimoDTO.getStockMinimo());
        this.inventoryRepository.save(inventory);
        log.info("Stock mínimo del inventario " + id + " actualizado");
        return this.findByID(id);
    }

    // Reposición masiva: lleva cada inventario con stock bajo hasta el doble de su mínimo,
    // registrando una ENTRADA por cada uno. Todo ocurre en una sola transacción.
    @Transactional
    @Override
    public List<InventoryDetalleDTO> reponerStockBajo() {
        List<Inventory> repuestos = new ArrayList<>();
        for (Inventory inventory : this.inventoryRepository.findAll()) {
            if (inventory.getStockDisponible() <= inventory.getStockMinimo()) {
                long cantidad = (inventory.getStockMinimo() * 2) - inventory.getStockDisponible();
                if (cantidad > 0) {
                    inventory.setStockDisponible(inventory.getStockDisponible() + cantidad);
                    agregarMovimiento(inventory, "ENTRADA", cantidad);
                    repuestos.add(inventory);
                }
            }
        }
        this.inventoryRepository.saveAll(repuestos);
        log.info("Reposición masiva: " + repuestos.size() + " inventarios repuestos");
        return repuestos.stream().map(inv -> this.findByID(inv.getId())).toList();
    }

    private void agregarMovimiento(Inventory inventory, String tipo, Long cantidad) {
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setProductId(inventory.getProductId());
        movimiento.setTipo(tipo);
        movimiento.setCantidad(cantidad);
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setInventario(inventory);
        inventory.getMovimientos().add(movimiento);
    }
}