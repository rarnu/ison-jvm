package com.rarnu.ison

/**
 * represents an ISON block (table or object)
 */
data class Block @JvmOverloads constructor(
    /**
     * "table", "object", or "meta"
     */
    var kind: String,
    /**
     * Block name (e.g., "users", "config")
     */
    var name: String,
    /**
     * Field definitions in order
     */
    var fields: MutableList<FieldInfo> = mutableListOf(),
    /**
     * Data rows
     */
    var rows: MutableList<Row> = mutableListOf(),
    /**
     * Summary row after ---
     */
    var summaryRow: Row? = null
) {

    /**
     * adds a field to the block
     */
    fun addField(name: String, typeHint: String) {
        fields.add(FieldInfo(name = name, typeHint = typeHint))
    }

    /**
     * adds a row to the block
     */
    fun addRow(row: Row) {
        rows.add(row)
    }

    /**
     * returns the field names in order
     */
    fun getFieldNames(): List<String> = fields.map { it.name }

    /**
     * converts the block to a map representation
     */
    fun toDict(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>(
            "kind" to kind,
            "name" to name
        )
        // Fields with type hints
        val fs = fields.map { mapOf("name" to it.name, "typeHint" to it.typeHint) }
        result["fields"] = fs
        // Rows as list of maps
        val rs = rows.map { r -> r.mapValues { (_, v) -> v.intf() } }
        result["rows"] = rs
        if (summaryRow != null) {
            val sum = summaryRow?.mapValues { (_, v) -> v.intf() }
            result["summary"] = sum
        }
        return result
    }

}