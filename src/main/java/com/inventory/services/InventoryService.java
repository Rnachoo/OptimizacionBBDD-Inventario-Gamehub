package com.inventory.services;

import com.inventory.models.dtos.InventoryCantidadDTO;
import com.inventory.models.dtos.InventoryDetalleDTO;
import com.inventory.models.dtos.InventoryMinimoDTO;
import com.inventory.models.dtos.InventoryMovimientoDTO;
import com.inventory.models.dtos.InventorySaveDTO;
import com.inventory.models.dtos.InventoryUpdateCantidadDisponibleDTO;

import java.util.List;

public interface InventoryService {
    List<InventoryDetalleDTO> findAllByProduct(Long productId);
    InventoryDetalleDTO findByID(String id);
    InventoryDetalleDTO save (InventorySaveDTO inventorySaveDTO);
    InventoryDetalleDTO updateCantidadDisponible (String id, InventoryUpdateCantidadDisponibleDTO cantidadDisponibleDTO);
    void deleteById(String id  );
    InventoryDetalleDTO reservarStock(Long productId, InventoryCantidadDTO cantidadDTO);
    InventoryDetalleDTO liberarStock(Long productId, InventoryCantidadDTO cantidadDTO);
    InventoryDetalleDTO confirmarVenta(Long productId, InventoryCantidadDTO cantidadDTO);
    List<InventoryDetalleDTO> findAll();
    InventoryDetalleDTO registrarMovimiento(String id, InventoryMovimientoDTO movimientoDTO);
    InventoryDetalleDTO actualizarStockMinimo(String id, InventoryMinimoDTO minimoDTO);
    List<InventoryDetalleDTO> reponerStockBajo();

}