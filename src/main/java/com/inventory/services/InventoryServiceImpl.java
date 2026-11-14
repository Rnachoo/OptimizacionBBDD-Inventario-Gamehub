package com.inventory.services;

import com.inventory.exceptions.InventoryException;
import com.inventory.models.Inventory;
import com.inventory.models.MovimientoInventario;
import com.inventory.models.dtos.*;
import com.inventory.repositories.InventoryRepository;
import com.inventory.repositories.MovimientoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.MongoExpression;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<InventoryDetalleDTO> findAllByProduct(Long productId) {
        log.info("Listando inventario del producto " + productId);
        return conMovimientos(this.inventoryRepository.findByProductId(productId));
    }

    @Override
    public List<InventoryDetalleDTO> findAll() {
        log.info("Listando todos los inventarios");
        return conMovimientos(this.inventoryRepository.findAll());
    }

    @Override
    public InventoryDetalleDTO findByID(String id) {
        Inventory inventory = buscar(id);
        return toDTO(inventory, this.movimientoRepository.findByInventoryIdOrderByFechaDesc(id));
    }

    @Transactional
    @Override
    public InventoryDetalleDTO save(InventorySaveDTO dto) {
        Inventory inventory = new Inventory();
        inventory.setProductId(dto.getProductId());
        inventory.setStockDisponible(dto.getStockDisponible());
        inventory.setStockReservado(dto.getStockReservado());
        inventory.setStockMinimo(dto.getStockMinimo());
        inventory.setUbicacion(dto.getUbicacion());
        inventory.setFechaUltimaActualizacion(LocalDateTime.now());

        try {
            inventory = this.inventoryRepository.insert(inventory);
        } catch (DuplicateKeyException e) {
            throw new InventoryException("Ya existe un inventario para el producto " + dto.getProductId());
        }

        if (dto.getMovimientosDTO() != null) {
            for (MovimientoDetalleDTO mov : dto.getMovimientosDTO()) {
                registrarEnHistorial(inventory, mov.getTipo(), mov.getCantidad());
            }
        }
        log.info("Inventario " + inventory.getId() + " creado");
        return findByID(inventory.getId());
    }

    @Override
    public InventoryDetalleDTO updateCantidadDisponible(String id, InventoryUpdateCantidadDisponibleDTO dto) {
        if (dto.getStockDisponible() < 0) {
            throw new InventoryException("El stock disponible no puede quedar en negativo.");
        }
        Inventory inventory = buscar(id);
        inventory.setStockDisponible(dto.getStockDisponible());
        inventory.setFechaUltimaActualizacion(LocalDateTime.now());
        this.inventoryRepository.save(inventory);
        return findByID(id);
    }

    @Transactional
    @Override
    public void deleteById(String id) {
        Inventory inventory = buscar(id);
        if (inventory.getStockReservado() > 0) {
            throw new InventoryException("No se puede eliminar el inventario dado que hay productos reservados");
        }
        this.movimientoRepository.deleteByInventoryId(id);
        this.inventoryRepository.delete(inventory);
        log.info("Inventario " + id + " y su historial eliminados");
    }

    @Override
    public InventoryDetalleDTO reservarStock(Long productId, InventoryCantidadDTO cantidadDTO) {
        Long cantidad = cantidadDTO.getCantidad();
        Inventory inventory = modificarAtomico(
                Criteria.where("productId").is(productId).and("stockDisponible").gte(cantidad),
                new Update().inc("stockDisponible", -cantidad).inc("stockReservado", cantidad));
        if (inventory == null) {
            Inventory actual = buscarPorProducto(productId);
            throw new InventoryException("Stock insuficiente, Solicitado:" + cantidad + ", Disponible:" + actual.getStockDisponible());
        }
        return findByID(inventory.getId());
    }

    @Override
    public InventoryDetalleDTO confirmarVenta(Long productId, InventoryCantidadDTO cantidadDTO) {
        Long cantidad = cantidadDTO.getCantidad();
        Inventory inventory = modificarAtomico(
                Criteria.where("productId").is(productId).and("stockReservado").gte(cantidad),
                new Update().inc("stockReservado", -cantidad));
        if (inventory == null) {
            buscarPorProducto(productId);
            throw new InventoryException("Error: Se estan intentado confirmar mas productos de los reservados");
        }
        return findByID(inventory.getId());
    }

    @Override
    public InventoryDetalleDTO liberarStock(Long productId, InventoryCantidadDTO cantidadDTO) {
        Inventory actual = buscarPorProducto(productId);
        long cantidad = Math.min(cantidadDTO.getCantidad(), actual.getStockReservado());
        Inventory inventory = modificarAtomico(
                Criteria.where("productId").is(productId).and("stockReservado").gte(cantidad),
                new Update().inc("stockReservado", -cantidad).inc("stockDisponible", cantidad));
        if (inventory == null) {
            throw new InventoryException("La reserva cambió mientras se liberaba, intenta de nuevo");
        }
        return findByID(inventory.getId());
    }

    // Equivalente al FOR UPDATE de Oracle: el filtro y el $inc ocurren en una sola operación atómica
    @Transactional
    @Override
    public InventoryDetalleDTO registrarMovimiento(String id, InventoryMovimientoDTO movimientoDTO) {
        String tipo = movimientoDTO.getTipo().trim().toUpperCase();
        Long cantidad = movimientoDTO.getCantidad();
        if (!tipo.equals("ENTRADA") && !tipo.equals("SALIDA")) {
            throw new InventoryException("El tipo de movimiento debe ser ENTRADA o SALIDA");
        }

        Criteria filtro = Criteria.where("_id").is(id);
        Update update = new Update();
        if (tipo.equals("SALIDA")) {
            filtro.and("stockDisponible").gte(cantidad);
            update.inc("stockDisponible", -cantidad);
        } else {
            update.inc("stockDisponible", cantidad);
        }

        Inventory inventory = modificarAtomico(filtro, update);
        if (inventory == null) {
            Inventory actual = buscar(id);
            throw new InventoryException("Stock insuficiente, Solicitado:" + cantidad + ", Disponible:" + actual.getStockDisponible());
        }

        registrarEnHistorial(inventory, tipo, cantidad);
        log.info("Movimiento " + tipo + " de " + cantidad + " en inventario " + id);
        return findByID(id);
    }

    @Override
    public InventoryDetalleDTO actualizarStockMinimo(String id, InventoryMinimoDTO minimoDTO) {
        Inventory inventory = modificarAtomico(Criteria.where("_id").is(id),
                new Update().set("stockMinimo", minimoDTO.getStockMinimo()));
        if (inventory == null) {
            throw new InventoryException("Inventario con ID " + id + " no encontrado");
        }
        return findByID(id);
    }

    // Equivalente a BULK COLLECT + FORALL: se envían todas las actualizaciones en un solo viaje (bulkWrite)
    @Transactional
    @Override
    public List<InventoryDetalleDTO> reponerStockBajo() {
        Query stockBajo = new Query(Criteria.expr(
                MongoExpression.create("{ $lte: ['$stockDisponible', '$stockMinimo'] }")));
        List<Inventory> bajos = this.mongoTemplate.find(stockBajo, Inventory.class);
        if (bajos.isEmpty()) return List.of();

        LocalDateTime ahora = LocalDateTime.now();
        BulkOperations bulk = this.mongoTemplate.bulkOps(BulkOperations.BulkMode.ORDERED, Inventory.class);
        List<MovimientoInventario> entradas = new ArrayList<>();
        List<String> ids = new ArrayList<>();

        for (Inventory inv : bajos) {
            long cantidad = inv.getStockMinimo() * 2 - inv.getStockDisponible();
            if (cantidad <= 0) continue;
            bulk.updateOne(new Query(Criteria.where("_id").is(inv.getId())),
                    new Update().inc("stockDisponible", cantidad).set("fechaUltimaActualizacion", ahora));
            entradas.add(nuevoMovimiento(inv, "ENTRADA", cantidad, ahora));
            ids.add(inv.getId());
        }
        if (ids.isEmpty()) return List.of();

        bulk.execute();
        this.movimientoRepository.insert(entradas);
        log.info("Reposición masiva: " + ids.size() + " inventarios repuestos");
        return ids.stream().map(this::findByID).toList();
    }

    private Inventory modificarAtomico(Criteria filtro, Update update) {
        update.set("fechaUltimaActualizacion", LocalDateTime.now());
        return this.mongoTemplate.findAndModify(new Query(filtro), update,
                FindAndModifyOptions.options().returnNew(true), Inventory.class);
    }

    private void registrarEnHistorial(Inventory inventory, String tipo, Long cantidad) {
        this.movimientoRepository.insert(nuevoMovimiento(inventory, tipo, cantidad, LocalDateTime.now()));
    }

    private MovimientoInventario nuevoMovimiento(Inventory inventory, String tipo, Long cantidad, LocalDateTime fecha) {
        MovimientoInventario mov = new MovimientoInventario();
        mov.setInventoryId(inventory.getId());
        mov.setProductId(inventory.getProductId());
        mov.setTipo(tipo);
        mov.setCantidad(cantidad);
        mov.setFecha(fecha);
        return mov;
    }

    private Inventory buscar(String id) {
        return this.inventoryRepository.findById(id).orElseThrow(
                () -> new InventoryException("Inventario con ID " + id + " no encontrado"));
    }

    private Inventory buscarPorProducto(Long productId) {
        return this.inventoryRepository.findByProductId(productId).stream().findFirst().orElseThrow(
                () -> new InventoryException("Inventario no encontrado para el producto con id " + productId));
    }

    // Una sola consulta para todos los historiales (evita N+1, el "JOIN" se hace en la aplicación)
    private List<InventoryDetalleDTO> conMovimientos(List<Inventory> inventarios) {
        List<String> ids = inventarios.stream().map(Inventory::getId).toList();
        Map<String, List<MovimientoInventario>> porInventario = this.movimientoRepository.findByInventoryIdIn(ids)
                .stream().collect(Collectors.groupingBy(MovimientoInventario::getInventoryId));
        return inventarios.stream()
                .map(inv -> toDTO(inv, porInventario.getOrDefault(inv.getId(), List.of())))
                .toList();
    }

    private InventoryDetalleDTO toDTO(Inventory inventory, List<MovimientoInventario> movimientos) {
        InventoryDetalleDTO dto = new InventoryDetalleDTO();
        dto.setId(inventory.getId());
        dto.setProductId(inventory.getProductId());
        dto.setStockDisponible(inventory.getStockDisponible());
        dto.setStockReservado(inventory.getStockReservado());
        dto.setStockMinimo(inventory.getStockMinimo());
        dto.setUbicacion(inventory.getUbicacion());
        dto.setMovimientosDTO(movimientos.stream()
                .sorted((a, b) -> b.getFecha().compareTo(a.getFecha()))
                .map(mov -> {
                    MovimientoDetalleDTO m = new MovimientoDetalleDTO();
                    m.setId(mov.getId());
                    m.setProductId(mov.getProductId());
                    m.setTipo(mov.getTipo());
                    m.setCantidad(mov.getCantidad());
                    m.setFecha(mov.getFecha());
                    return m;
                }).toList());
        return dto;
    }
}
