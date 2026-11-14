package com.inventory.controllers;

import com.inventory.assemblers.InventoryModelAssembler;
import com.inventory.models.dtos.InventoryCantidadDTO;
import com.inventory.models.dtos.InventoryDetalleDTO;
import com.inventory.models.dtos.InventoryMinimoDTO;
import com.inventory.models.dtos.InventoryMovimientoDTO;
import com.inventory.models.dtos.InventorySaveDTO;
import com.inventory.models.dtos.InventoryUpdateCantidadDisponibleDTO;
import com.inventory.services.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventories")
@Validated
@Tag(name = "Inventario V1", description = "Metodos Crud para la gestión del gestor de Inventario: Stock, liberación y reserva de productos")
public class InventoryController {
    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryModelAssembler inventoryModelAssembler;

    @GetMapping("/producto/{productId}")
    @Operation(summary = "Listar inventario por Producto", description = "devuelve todos los registoros de un inventario asociado al ID de un producto")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Operación exitosa",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema (implementation = InventoryDetalleDTO.class)))
    })
    public ResponseEntity<CollectionModel<EntityModel<InventoryDetalleDTO>>> findAllByProduct(
            @Parameter(description = "ID del producto para búscar un Inventario", required = true, example = "1")
            @PathVariable Long productId
    ){
        List<EntityModel<InventoryDetalleDTO>> entityModels = this.inventoryService.findAllByProduct(productId)
                .stream()
                .map(inventoryModelAssembler::toModel)
                .toList();
        CollectionModel<EntityModel<InventoryDetalleDTO>> collectionModel = CollectionModel.of(entityModels);
        return ResponseEntity.status(HttpStatus.OK).body(collectionModel);
    }


    @GetMapping
    @Operation(summary = "Listar todos los inventarios", description = "Devuelve todos los registros de inventario")
    public ResponseEntity<CollectionModel<EntityModel<InventoryDetalleDTO>>> findAll() {
        List<EntityModel<InventoryDetalleDTO>> entityModels = this.inventoryService.findAll()
                .stream()
                .map(inventoryModelAssembler::toModel)
                .toList();
        return ResponseEntity.ok(CollectionModel.of(entityModels));
    }


    //Buscar inventario por el ID
    @GetMapping("/{id}")
    @Operation(summary = "Buscar Inventario por ID", description = "Devuelve el detalle de un registro de inventario específico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventario encontrado"),
            @ApiResponse(responseCode = "404", description = "Inventario no encontrado")
    })
    public ResponseEntity<EntityModel<InventoryDetalleDTO>> findById(
            @Parameter(description = "ID del inventario", required = true, example = "1")
            @PathVariable Long id
    ) {
        EntityModel<InventoryDetalleDTO> entityModel = inventoryModelAssembler.toModel(inventoryService.findByID(id));
        return ResponseEntity.status(HttpStatus.OK).body(entityModel);
    }

    //Registrar un inventario
    @PostMapping
    @Operation(summary = "Crear un registro de Inventario", description = "Crea un nuevo inventario para los productos")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Inventario a crear", required = true,
            content = @Content(schema = @Schema(implementation = InventorySaveDTO.class)))
    public ResponseEntity<EntityModel<InventoryDetalleDTO>> save(@Valid @RequestBody InventorySaveDTO inventorySaveDTO){
        InventoryDetalleDTO inventoryCreate = this.inventoryService.save(inventorySaveDTO);
        EntityModel<InventoryDetalleDTO> entityModel = this.inventoryModelAssembler.toModel(inventoryCreate);
        return ResponseEntity.status(HttpStatus.CREATED).body(entityModel);
    }

    //Actualizar Stock disponible
    @PatchMapping("/{id}/cantidad")
    @Operation(summary = "Actualizar stock disponíble", description = "Modifica manualmente la cantidad disponible del inventario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200" , description = "Stock actualizado con éxito"),
            @ApiResponse(responseCode = "400", description = "El stock disponible no puede quedar en negativo"),
            @ApiResponse(responseCode = "404", description = "Inventario no encontrado")
    })
    public ResponseEntity<EntityModel<InventoryDetalleDTO>> updateCantidadDisponible  (
            @Parameter(description = "ID del inventario", required = true, example = "1")
            @PathVariable Long id, @Valid @RequestBody InventoryUpdateCantidadDisponibleDTO cantidadDisponibleDTO
    ){
        InventoryDetalleDTO inventoryUpdate = inventoryService.updateCantidadDisponible(id, cantidadDisponibleDTO);
        EntityModel<InventoryDetalleDTO> entityModel = this.inventoryModelAssembler.toModel(inventoryUpdate);
        return ResponseEntity.status(HttpStatus.OK).body(entityModel);
    }

    //Borra definitivamente un Inventario por el ID
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar Inventario", description = "Elimina un inventario siempre y cuando no tenga stock reservado")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Inventario eliminado sin contenido"),
            @ApiResponse(responseCode = "404", description = "Inventario no encontrado"),
            @ApiResponse(responseCode = "400", description = "No se puede eliminar por tener productos reservados")
    })
    public ResponseEntity<Void> deleteById(
            @Parameter(description = "ID del inventario a eliminar", required = true, example = "1")
            @PathVariable Long id
    ){

        this.inventoryService.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    //Reservar un stock
    @PutMapping("/producto/{productId}/reservar")
    @Operation(summary = "Reservar Stock", description = "Mueve unidades de Stock disponibles a Stock reservado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reserva exitosa",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InventoryDetalleDTO.class))),
            @ApiResponse(responseCode = "400", description = "Stock insuficiente para realizar la reserva"),
            @ApiResponse(responseCode = "404", description = "Inventario no encontrado para el producto")
    })
    public ResponseEntity<EntityModel<InventoryDetalleDTO>> reservarStock(
            @Parameter(description = "ID del producto", required = true, example = "1")
            @PathVariable Long productId, @Valid @RequestBody InventoryCantidadDTO cantidadDTO
    ){

        InventoryDetalleDTO response = inventoryService.reservarStock(productId, cantidadDTO);
        EntityModel<InventoryDetalleDTO> entityModel = this.inventoryModelAssembler.toModel(response);
        return ResponseEntity.status(HttpStatus.OK).body(entityModel);
    }

    //Liberar un stock
    @PutMapping("/producto/{productId}/liberar")
    @Operation(summary = "Liberar Stock", description = "Devuelve unidades del stock reservado al stock disponible")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock liberado con éxito",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InventoryDetalleDTO.class))),
            @ApiResponse(responseCode = "404", description = "Inventario no encontrado para el producto")
    })
    public ResponseEntity<EntityModel<InventoryDetalleDTO>> liberarStock(
            @Parameter(description = "ID del producto", required = true, example = "1")
            @PathVariable Long productId, @Valid @RequestBody InventoryCantidadDTO cantidadDTO
    ){
        InventoryDetalleDTO response = inventoryService.liberarStock(productId, cantidadDTO);
        EntityModel<InventoryDetalleDTO> entityModel = this.inventoryModelAssembler.toModel(response);
        return ResponseEntity.status(HttpStatus.OK).body(entityModel);
    }

    //Confirmar una venta
    @PutMapping("/producto/{productId}/confirmar-venta")
    @Operation(summary = "Confirmar Venta", description = "Resta definitivamente las unidades del stock reservado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Venta confirmada exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InventoryDetalleDTO.class))),
            @ApiResponse(responseCode = "400", description = "Se intentan confirmar más productos de los que hay reservados"),
            @ApiResponse(responseCode = "404", description = "Inventario no encontrado para el producto")
    })
    public ResponseEntity<EntityModel<InventoryDetalleDTO>> confirmarVenta (
            @Parameter(description = "ID del producto", required = true, example = "1")
            @PathVariable Long productId, @Valid @RequestBody InventoryCantidadDTO cantidadDTO
    ){
        InventoryDetalleDTO response = inventoryService.confirmarVenta(productId, cantidadDTO);
        EntityModel<InventoryDetalleDTO> entityModel = this.inventoryModelAssembler.toModel(response);
        return ResponseEntity.status(HttpStatus.OK).body(entityModel);
    }

    //Registrar una entrada o salida de stock
    @PostMapping("/{id}/movimientos")
    @Operation(summary = "Registrar movimiento de stock", description = "Registra una ENTRADA o SALIDA. La salida falla con 400 si el stock disponible no alcanza")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Movimiento registrado"),
            @ApiResponse(responseCode = "400", description = "Stock insuficiente o datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Inventario no encontrado")
    })
    public ResponseEntity<EntityModel<InventoryDetalleDTO>> registrarMovimiento(
            @Parameter(description = "ID del inventario", required = true, example = "1")
            @PathVariable Long id, @Valid @RequestBody InventoryMovimientoDTO movimientoDTO
    ){
        InventoryDetalleDTO response = inventoryService.registrarMovimiento(id, movimientoDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryModelAssembler.toModel(response));
    }

    //Editar el stock mínimo
    @PatchMapping("/{id}/minimo")
    @Operation(summary = "Actualizar stock mínimo", description = "Modifica solo el stock mínimo de un inventario")
    public ResponseEntity<EntityModel<InventoryDetalleDTO>> actualizarMinimo(
            @Parameter(description = "ID del inventario", required = true, example = "1")
            @PathVariable Long id, @Valid @RequestBody InventoryMinimoDTO minimoDTO
    ){
        InventoryDetalleDTO response = inventoryService.actualizarStockMinimo(id, minimoDTO);
        return ResponseEntity.ok(inventoryModelAssembler.toModel(response));
    }

    //Reposición masiva
    @PostMapping("/reposicion")
    @Operation(summary = "Reposición masiva", description = "Lleva cada inventario con stock bajo hasta el doble de su mínimo y registra una ENTRADA por cada uno")
    public ResponseEntity<CollectionModel<EntityModel<InventoryDetalleDTO>>> reponerStockBajo(){
        List<EntityModel<InventoryDetalleDTO>> repuestos = inventoryService.reponerStockBajo()
                .stream()
                .map(inventoryModelAssembler::toModel)
                .toList();
        return ResponseEntity.ok(CollectionModel.of(repuestos));
    }
}