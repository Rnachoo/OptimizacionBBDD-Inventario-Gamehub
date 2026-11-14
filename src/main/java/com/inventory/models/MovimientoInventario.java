package com.inventory.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

// Referenciado (no embebido): el historial crece sin límite y un documento tiene tope de 16 MB
@Document(collection = "movimientos_inv")
@CompoundIndex(name = "inventario_fecha", def = "{'inventoryId': 1, 'fecha': -1}")
@Getter
@Setter
@ToString
@NoArgsConstructor
public class MovimientoInventario {

    @Id
    private String id;

    private String inventoryId;
    private Long productId;
    private String tipo;
    private Long cantidad;
    private LocalDateTime fecha;
}
