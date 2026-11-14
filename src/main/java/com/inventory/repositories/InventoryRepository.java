package com.inventory.repositories;

import com.inventory.models.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    List<Inventory> findByProductId(Long productoId);

    // Descuenta stock en una sola sentencia SQL y solo si alcanza.
    // Devuelve 1 si descontó y 0 si no había stock suficiente.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Inventory i set i.stockDisponible = i.stockDisponible - :cantidad " +
            "where i.id = :id and i.stockDisponible >= :cantidad")
    int descontarSiHayStock(@Param("id") Long id, @Param("cantidad") Long cantidad);
}