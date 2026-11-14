package com.inventory.config;

import com.inventory.models.dtos.InventorySaveDTO;
import com.inventory.models.dtos.MovimientoDetalleDTO;
import com.inventory.services.InventoryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
@Profile("dev")
public class DevDataSeeder {

    @Bean
    CommandLineRunner seedInventario(InventoryService service) {
        return args -> {
            if (!service.findAll().isEmpty()) return;

            // productId, stockDisponible, stockReservado, stockMinimo, ubicacion
            Object[][] datos = {
                    {1L, 50L, 0L, 10L, "Pasillo 1, Estante A"},
                    {2L, 8L, 2L, 10L, "Pasillo 1, Estante B"},
                    {3L, 120L, 5L, 20L, "Pasillo 2, Estante A"},
                    {4L, 3L, 0L, 5L, "Pasillo 2, Estante B"},
                    {5L, 40L, 0L, 10L, "Pasillo 3, Estante A"},
                    {6L, 15L, 1L, 15L, "Pasillo 3, Estante B"},
                    {7L, 200L, 10L, 30L, "Bodega Central"},
                    {8L, 6L, 0L, 8L, "Bodega Central"}
            };

            for (Object[] d : datos) {
                InventorySaveDTO dto = new InventorySaveDTO();
                dto.setProductId((Long) d[0]);
                dto.setStockDisponible((Long) d[1]);
                dto.setStockReservado((Long) d[2]);
                dto.setStockMinimo((Long) d[3]);
                dto.setUbicacion((String) d[4]);

                MovimientoDetalleDTO mov = new MovimientoDetalleDTO();
                mov.setProductId((Long) d[0]);
                mov.setTipo("ENTRADA");
                mov.setCantidad((Long) d[1]);
                dto.setMovimientosDTO(List.of(mov));

                service.save(dto);
            }
        };
    }
}