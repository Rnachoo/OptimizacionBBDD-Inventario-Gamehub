package com.inventory.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "inventarios")
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Inventory {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long productId;

    private Long stockDisponible;
    private Long stockReservado;
    private Long stockMinimo;
    private String ubicacion;
    private LocalDateTime fechaUltimaActualizacion;
}
