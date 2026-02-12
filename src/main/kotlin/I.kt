package com.rarnu.ison

/**
 * I provides a namespace for schema creation (like Zod's z)
 */
object I {

    @JvmStatic
    fun STRING(): StringSchema = StringSchema()

    @JvmStatic
    fun NUMBER(): NumberSchema = NumberSchema()

    @JvmStatic
    fun INT(): NumberSchema = NumberSchema.INT()

    @JvmStatic
    fun FLOAT(): NumberSchema = NumberSchema()

    @JvmStatic
    fun BOOLEAN(): BooleanSchema = BooleanSchema()

    @JvmStatic
    fun BOOL(): BooleanSchema = BooleanSchema()

    @JvmStatic
    fun NULL(): NullSchema = NullSchema()

    @JvmStatic
    fun REF(): RefSchema = RefSchema()

    @JvmStatic
    fun REFERENCE(): RefSchema = RefSchema()

    @JvmStatic
    fun OBJECT(fields: Map<String, Schema>): ObjectSchema = ObjectSchema.OBJECT(fields.toMutableMap())

    @JvmStatic
    fun ARRAY(itemSchema: Schema): ArraySchema = ArraySchema(itemSchema)

    @JvmStatic
    fun TABLE(name: String, fields: Map<String, Schema>): TableSchema = TableSchema(name, fields.toMutableMap())
}
