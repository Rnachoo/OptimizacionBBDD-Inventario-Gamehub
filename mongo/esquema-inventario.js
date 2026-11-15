// Crea las colecciones del microservicio de inventario con validación e índices.
// Uso: mongosh "<MONGODB_URI>" mongo/esquema-inventario.js

db = db.getSiblingDB("gamehub_inventario");

const reglasInventarios = {
  $jsonSchema: {
    bsonType: "object",
    required: ["productId", "stockDisponible", "stockReservado", "stockMinimo", "ubicacion"],
    properties: {
      productId:       { bsonType: ["long", "int"], minimum: 1, description: "Id del producto, obligatorio y positivo" },
      stockDisponible: { bsonType: ["long", "int"], minimum: 0, description: "No puede ser negativo" },
      stockReservado:  { bsonType: ["long", "int"], minimum: 0, description: "No puede ser negativo" },
      stockMinimo:     { bsonType: ["long", "int"], minimum: 0, description: "No puede ser negativo" },
      ubicacion:       { bsonType: "string", minLength: 1, description: "Ubicación en bodega, obligatoria" },
      fechaUltimaActualizacion: { bsonType: "date" }
    }
  }
};

const reglasMovimientos = {
  $jsonSchema: {
    bsonType: "object",
    required: ["inventoryId", "productId", "tipo", "cantidad", "fecha"],
    properties: {
      inventoryId: { bsonType: "string", minLength: 1, description: "Referencia al _id del inventario" },
      productId:   { bsonType: ["long", "int"], minimum: 1 },
      tipo:        { enum: ["ENTRADA", "SALIDA"], description: "Solo ENTRADA o SALIDA" },
      cantidad:    { bsonType: ["long", "int"], minimum: 1, description: "Mayor que cero" },
      fecha:       { bsonType: "date" }
    }
  }
};

function crearOActualizar(nombre, reglas) {
  const existe = db.getCollectionNames().includes(nombre);
  if (existe) {
    db.runCommand({ collMod: nombre, validator: reglas, validationLevel: "strict", validationAction: "error" });
  } else {
    db.createCollection(nombre, { validator: reglas, validationLevel: "strict", validationAction: "error" });
  }
}

crearOActualizar("inventarios", reglasInventarios);
crearOActualizar("movimientos_inv", reglasMovimientos);

db.inventarios.createIndex({ productId: 1 }, { unique: true, name: "productId" });
db.movimientos_inv.createIndex({ inventoryId: 1, fecha: -1 }, { name: "inventario_fecha" });

print("Colecciones, validaciones e índices listos");