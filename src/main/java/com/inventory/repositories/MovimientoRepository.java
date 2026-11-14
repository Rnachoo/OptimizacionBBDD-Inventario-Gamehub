package com.inventory.repositories;

import com.inventory.models.MovimientoInventario;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimientoRepository extends MongoRepository<MovimientoInventario, String> {
    List<MovimientoInventario> findByInventoryIdOrderByFechaDesc(String inventoryId);
    List<MovimientoInventario> findByInventoryIdIn(List<String> inventoryIds);
    void deleteByInventoryId(String inventoryId);
}
