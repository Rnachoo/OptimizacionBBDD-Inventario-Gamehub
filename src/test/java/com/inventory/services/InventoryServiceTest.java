package com.inventory.services;

import com.inventory.exceptions.InventoryException;
import com.inventory.models.Inventory;
import com.inventory.models.MovimientoInventario;
import com.inventory.models.dtos.*;
import com.inventory.repositories.InventoryRepository;
import com.inventory.repositories.MovimientoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InventoryServiceTest {

    private static final String ID = "665f1c2ab4e8a91d2c3f0001";

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private MovimientoRepository movimientoRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private Inventory inventario;

    @BeforeEach
    public void setUp() {
        inventario = new Inventory();
        inventario.setId(ID);
        inventario.setProductId(100L);
        inventario.setStockDisponible(50L);
        inventario.setStockReservado(10L);
        inventario.setStockMinimo(5L);
        inventario.setUbicacion("Pasillo 3, Estante A");
        inventario.setFechaUltimaActualizacion(LocalDateTime.now());
    }

    private Inventory copiaCon(long disponible, long reservado) {
        Inventory inv = new Inventory();
        inv.setId(ID);
        inv.setProductId(100L);
        inv.setStockDisponible(disponible);
        inv.setStockReservado(reservado);
        inv.setStockMinimo(5L);
        inv.setUbicacion(inventario.getUbicacion());
        return inv;
    }

    private void cuandoModificaDevuelve(Inventory resultado) {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Inventory.class))).thenReturn(resultado);
    }

    private void cuandoBuscaPorIdDevuelve(Inventory inv) {
        when(inventoryRepository.findById(ID)).thenReturn(Optional.of(inv));
        lenient().when(movimientoRepository.findByInventoryIdOrderByFechaDesc(ID)).thenReturn(List.of());
    }

    @Test
    @DisplayName("Debe listar inventarios con su historial en una sola consulta de movimientos")
    public void shouldFindAllWithMovimientos() {
        MovimientoInventario mov = new MovimientoInventario();
        mov.setId("m1");
        mov.setInventoryId(ID);
        mov.setTipo("ENTRADA");
        mov.setCantidad(50L);
        mov.setFecha(LocalDateTime.now());
        when(inventoryRepository.findAll()).thenReturn(List.of(inventario));
        when(movimientoRepository.findByInventoryIdIn(List.of(ID))).thenReturn(List.of(mov));

        List<InventoryDetalleDTO> resultado = inventoryService.findAll();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getMovimientosDTO()).hasSize(1);
        verify(movimientoRepository, times(1)).findByInventoryIdIn(anyList());
    }

    @Test
    @DisplayName("Debe buscar un inventario por su ID")
    public void shouldFindById() {
        cuandoBuscaPorIdDevuelve(inventario);

        InventoryDetalleDTO dto = inventoryService.findByID(ID);

        assertThat(dto.getId()).isEqualTo(ID);
        assertThat(dto.getProductId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Debe lanzar excepción si el inventario no existe")
    public void shouldThrowWhenNotFound() {
        when(inventoryRepository.findById("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.findByID("x"))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("no encontrado");
    }

    @Test
    @DisplayName("Debe crear un inventario y registrar sus movimientos iniciales")
    public void shouldSaveInventory() {
        InventorySaveDTO dto = new InventorySaveDTO();
        dto.setProductId(100L);
        dto.setStockDisponible(50L);
        dto.setStockReservado(0L);
        dto.setStockMinimo(5L);
        dto.setUbicacion("Pasillo 3, Estante A");
        MovimientoDetalleDTO mov = new MovimientoDetalleDTO();
        mov.setTipo("ENTRADA");
        mov.setCantidad(50L);
        dto.setMovimientosDTO(List.of(mov));

        when(inventoryRepository.insert(any(Inventory.class))).thenReturn(inventario);
        cuandoBuscaPorIdDevuelve(inventario);

        InventoryDetalleDTO resultado = inventoryService.save(dto);

        assertThat(resultado.getId()).isEqualTo(ID);
        verify(movimientoRepository).insert(any(MovimientoInventario.class));
    }

    @Test
    @DisplayName("Debe rechazar un productId duplicado (índice único)")
    public void shouldRejectDuplicateProduct() {
        InventorySaveDTO dto = new InventorySaveDTO();
        dto.setProductId(100L);
        dto.setStockDisponible(1L);
        dto.setStockReservado(0L);
        dto.setStockMinimo(1L);
        dto.setUbicacion("A");
        when(inventoryRepository.insert(any(Inventory.class))).thenThrow(new DuplicateKeyException("dup"));

        assertThatThrownBy(() -> inventoryService.save(dto))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("Ya existe");
    }

    @Test
    @DisplayName("Debe lanzar excepción al actualizar stock a un valor negativo")
    public void shouldThrowWhenNegativeStock() {
        InventoryUpdateCantidadDisponibleDTO dto = new InventoryUpdateCantidadDisponibleDTO();
        dto.setStockDisponible(-5L);

        assertThatThrownBy(() -> inventoryService.updateCantidadDisponible(ID, dto))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("negativo");
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe eliminar el inventario y su historial si no tiene stock reservado")
    public void shouldDeleteById() {
        when(inventoryRepository.findById(ID)).thenReturn(Optional.of(copiaCon(50, 0)));

        inventoryService.deleteById(ID);

        verify(movimientoRepository).deleteByInventoryId(ID);
        verify(inventoryRepository).delete(any(Inventory.class));
    }

    @Test
    @DisplayName("No debe eliminar un inventario con stock reservado")
    public void shouldNotDeleteWithReservedStock() {
        when(inventoryRepository.findById(ID)).thenReturn(Optional.of(inventario));

        assertThatThrownBy(() -> inventoryService.deleteById(ID))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("reservados");
        verify(inventoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Debe registrar una SALIDA cuando hay stock suficiente")
    public void shouldRegisterSalida() {
        InventoryMovimientoDTO dto = new InventoryMovimientoDTO();
        dto.setTipo("salida");
        dto.setCantidad(20L);
        cuandoModificaDevuelve(copiaCon(30, 10));
        cuandoBuscaPorIdDevuelve(copiaCon(30, 10));

        InventoryDetalleDTO resultado = inventoryService.registrarMovimiento(ID, dto);

        assertThat(resultado.getStockDisponible()).isEqualTo(30L);
        verify(movimientoRepository).insert(any(MovimientoInventario.class));
    }

    @Test
    @DisplayName("Debe rechazar una SALIDA con stock insuficiente sin registrar movimiento")
    public void shouldRejectSalidaWithoutStock() {
        InventoryMovimientoDTO dto = new InventoryMovimientoDTO();
        dto.setTipo("SALIDA");
        dto.setCantidad(999L);
        cuandoModificaDevuelve(null);
        when(inventoryRepository.findById(ID)).thenReturn(Optional.of(inventario));

        assertThatThrownBy(() -> inventoryService.registrarMovimiento(ID, dto))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("Stock insuficiente");
        verify(movimientoRepository, never()).insert(any(MovimientoInventario.class));
    }

    @Test
    @DisplayName("Debe rechazar un tipo de movimiento inválido")
    public void shouldRejectInvalidTipo() {
        InventoryMovimientoDTO dto = new InventoryMovimientoDTO();
        dto.setTipo("REGALO");
        dto.setCantidad(1L);

        assertThatThrownBy(() -> inventoryService.registrarMovimiento(ID, dto))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("ENTRADA o SALIDA");
    }

    @Test
    @DisplayName("Debe reservar stock de forma atómica")
    public void shouldReservarStock() {
        InventoryCantidadDTO dto = new InventoryCantidadDTO();
        dto.setCantidad(10L);
        cuandoModificaDevuelve(copiaCon(40, 20));
        cuandoBuscaPorIdDevuelve(copiaCon(40, 20));

        InventoryDetalleDTO resultado = inventoryService.reservarStock(100L, dto);

        assertThat(resultado.getStockDisponible()).isEqualTo(40L);
        assertThat(resultado.getStockReservado()).isEqualTo(20L);
    }

    @Test
    @DisplayName("Debe rechazar la reserva si el stock no alcanza")
    public void shouldRejectReservaWithoutStock() {
        InventoryCantidadDTO dto = new InventoryCantidadDTO();
        dto.setCantidad(100L);
        cuandoModificaDevuelve(null);
        when(inventoryRepository.findByProductId(100L)).thenReturn(List.of(inventario));

        assertThatThrownBy(() -> inventoryService.reservarStock(100L, dto))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("Stock insuficiente");
    }

    @Test
    @DisplayName("Debe confirmar la venta restando del stock reservado")
    public void shouldConfirmarVenta() {
        InventoryCantidadDTO dto = new InventoryCantidadDTO();
        dto.setCantidad(5L);
        cuandoModificaDevuelve(copiaCon(50, 5));
        cuandoBuscaPorIdDevuelve(copiaCon(50, 5));

        assertThat(inventoryService.confirmarVenta(100L, dto).getStockReservado()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Debe liberar como máximo lo que hay reservado")
    public void shouldLiberarStock() {
        InventoryCantidadDTO dto = new InventoryCantidadDTO();
        dto.setCantidad(50L);
        when(inventoryRepository.findByProductId(100L)).thenReturn(List.of(inventario));
        cuandoModificaDevuelve(copiaCon(60, 0));
        cuandoBuscaPorIdDevuelve(copiaCon(60, 0));

        InventoryDetalleDTO resultado = inventoryService.liberarStock(100L, dto);

        assertThat(resultado.getStockDisponible()).isEqualTo(60L);
        assertThat(resultado.getStockReservado()).isZero();
    }
}
