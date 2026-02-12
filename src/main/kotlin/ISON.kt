@file:Suppress("UNCHECKED_CAST", "DuplicatedCode")

package com.rarnu.ison

import com.isyscore.kotlin.common.toObj
import com.rarnu.ison.Parser.Companion.parseFieldDef
import com.rarnu.ison.Parser.Companion.parseValue
import com.rarnu.ison.Parser.Companion.tokenizeLine
import java.io.File
import kotlin.collections.component1
import kotlin.collections.component2

object ISON {

    const val VERSION = "1.0.0"

    @JvmStatic
    fun parse(text: String): Document {
        val p = Parser(text, splitLines(text), 0)
        return p.parse()
    }

    @JvmStatic
    fun load(path: String): Document = load(File(path))

    @JvmStatic
    fun load(file: File): Document = parse(file.readText())

    /**
     * converts an ISON string directly to JSON
     */
    @JvmStatic
    fun toJson(isonText: String): String {
        val doc = parse(isonText)
        return doc.toJson()
    }

    /**
     * converts a JSON string to ISON Document
     */
    @JvmStatic
    fun fromJson(jsonText: String): Document {
        val data = jsonText.toObj<Map<String, Any>>()
        val doc = Document()
        data.forEach { (name, value) ->
            when (value) {
                is List<*> -> {
                    // Array of objects = table
                    val block = Block("table", name)
                    // Get fields from first row
                    if (value.isNotEmpty()) {
                        try {
                            value[0] as? Map<String, Any>
                        } catch (_: Exception) {
                            null
                        }?.forEach { (key, _) ->
                            block.addField(key, "")
                        }
                    }
                    // add rows
                    value.forEach { item ->
                        val rowData = try {
                            item as? Map<String, Any>
                        } catch (_: Exception) {
                            null
                        }
                        if (rowData != null) {
                            val row: Row = rowData.mapValues { (_, v) -> interfaceToValue(v) }.toMutableMap()
                            block.addRow(row)
                        }
                    }
                    doc.addBlock(block)
                }

                is Map<*, *> -> {
                    // Single object = object block
                    val block = Block("object", name)
                    value.forEach { (key, _) ->
                        block.addField("$key", "")
                    }
                    val row: Row = mutableMapOf()
                    value.forEach { (k, v) ->
                        row["$k"] = interfaceToValue(v)
                    }
                    block.addRow(row)
                    doc.addBlock(block)
                }
            }
        }
        return doc
    }

    /**
     * parses ISONL (line-based streaming format)
     */
    @JvmStatic
    fun parseISONL(text: String): Document {
        val doc = Document()
        val lines = splitLines(text)
        for (l in lines) {
            val line = l.trim()
            if (line.isBlank() || line.startsWith("#")) {
                continue
            }
            val parts = line.splitN("|", 3)
            if (parts.size != 3) {
                continue
            }
            // Parse block header
            val header = parts[0]
            val headerParts = header.splitN(".", 2)
            if (headerParts.size != 2) {
                continue
            }
            val kind = headerParts[0]
            val name = headerParts[1]
            // Get or create block
            var block = doc.get(name)
            if (block == null) {
                block = Block(kind, name)
                doc.addBlock(block)
                // Parse fields
                val fieldTokens = tokenizeLine(parts[1])
                fieldTokens.forEach { field ->
                    val (fname, ftype) = parseFieldDef(field)
                    block.addField(fname, ftype)
                }
            }
            // Parse row
            val tokens = tokenizeLine(parts[2])
            val row: Row = mutableMapOf()
            tokens.forEachIndexed { i, token ->
                if (i < block.fields.size) {
                    val fieldInfo = block.fields[i]
                    val v = parseValue(token, fieldInfo.typeHint)
                    row[fieldInfo.name] = v
                }
            }
            block.addRow(row)
        }
        return doc
    }

    /**
     * loads and parses an ISONL file
     */
    @JvmStatic
    fun loadISONL(path: String): Document = loadISONL(File(path))

    /**
     * loads and parses an ISONL file
     */
    @JvmStatic
    fun loadISONL(file: File): Document = parseISONL(file.readText())

    /**
     * serializes a Document and writes it to an ISONL file
     */


    /**
     * converts ISON format to ISONL format
     */
    @JvmStatic
    fun ISONToISONL(isonText: String): String {
        val doc = parse(isonText)
        return Dump.dumpsISONL(doc)
    }

    /**
     * converts ISONL format to ISON format
     */
    @JvmStatic
    fun ISONLToISON(isonlText: String): String {
        val doc = parseISONL(isonlText)
        return Dump.dumps(doc)
    }

    /**
     * returns default FromDict options
     */
    @JvmStatic
    fun defaultFromDictOptions(): FromDictOptions = FromDictOptions(autoRefs = false, smartOrder = false)


    /**
     * creates an ISON Document from a map
     */
    @JvmStatic
    fun fromDict(data: Map<String, Any?>): Document = fromDictWithOptions(data, defaultFromDictOptions())

    /**
     * creates an ISON Document from a map with options
     */
    @JvmStatic
    fun fromDictWithOptions(data: Map<String, Any?>, opts: FromDictOptions): Document {
        val doc = Document()
        // Collect all table names for reference detection
        val tableNames = data.mapValues { (_, _) -> true }
        // Detect reference fields if auto_refs is enabled
        val refFields = mutableMapOf<String, String>()
        if (opts.autoRefs) {
            data.forEach { (tableName, tableData) ->
                val arr = tableData as? List<Any>
                if (!arr.isNullOrEmpty()) {
                    val firstRow = arr[0] as? Map<String, Any?>
                    firstRow?.forEach { (key, _) ->
                        // Detect _id suffix pattern (e.g., customer_id -> customers)
                        if (key.endsWith("_id") && key != "id") {
                            val refType = key.dropLast(3)
                            if (tableNames[refType + "s"] == true || tableNames[refType] == true) {
                                refFields[key] = refType
                            }
                        }
                    }
                }
                // Special case: nodes/edges graph pattern
                if (tableName == "edges" && tableNames["nodes"] == true) {
                    refFields["source"] = "node"
                    refFields["target"] = "node"
                }
            }
        }

        // Sort table names for consistent ordering
        val names = data.map { (name, _) -> name }.toMutableList()
        names.sort()
        for (name in names) {
            when (val content = data[name]) {
                is List<*> -> {
                    // Table with multiple rows
                    if (content.isNotEmpty()) {
                        if (content[0] is Map<*, *>) {
                            // Collect all unique fields
                            val fieldSet = mutableMapOf<String, Boolean>()
                            var fieldOrder = mutableListOf<String>()
                            content.forEach { item ->
                                if (item is Map<*, *>) {
                                    item.forEach { (k, _) ->
                                        if (fieldSet["$k"] != true) {
                                            fieldSet["$k"] = true
                                            fieldOrder.add("$k")
                                        }
                                    }
                                }
                            }
                            // Apply smart ordering if enabled
                            if (opts.smartOrder) {
                                fieldOrder = smartOrderFields(fieldOrder)
                            }
                            val block = Block("table", name)
                            fieldOrder.forEach { field ->
                                block.addField(field, "")
                            }
                            // Convert rows with references if auto_refs
                            content.forEach { item ->
                                if (item is Map<*, *>) {
                                    val row: Row = mutableMapOf()
                                    for ((k, v) in item) {
                                        if (opts.autoRefs) {
                                            val refType = refFields["$k"]
                                            if (!refType.isNullOrBlank()) {
                                                when (v) {
                                                    is Int, is Long, is Double, is String -> {
                                                        row["$k"] = Value.REF(Reference(id = "$v", namespace = refType))
                                                        continue
                                                    }
                                                    else -> {}
                                                }
                                            }
                                        }
                                        row["$k"] = interfaceToValue(v)
                                    }
                                    block.addRow(row)
                                }
                            }
                            doc.addBlock(block)
                        }
                    }
                }

                is Map<*, *> -> {
                    // Single object = object block
                    val block = Block("object", name)
                    var fields = content.map { (k, _) -> "$k" }
                    if (opts.smartOrder) {
                        fields = smartOrderFields(fields)
                    }
                    fields.forEach { key ->
                        block.addField(key, "")
                    }
                    val row: Row = mutableMapOf()
                    content.forEach { (k, v) ->
                        row["$k"] = interfaceToValue(v)
                    }
                    block.addRow(row)
                    doc.addBlock(block)
                }
            }
        }
        return doc
    }

    /**
     * reorders fields for optimal LLM comprehension
     * Order priority: id first, then names, then data, then references
     */
    fun smartOrderFields(fields: List<String>): MutableList<String> {
        val priorityNames = mapOf(
            "name" to true,
            "title" to true,
            "label" to true,
            "description" to true,
            "display_name" to true,
            "full_name" to true
        )
        val idFields = mutableListOf<String>()
        val nameFields = mutableListOf<String>()
        val refFields = mutableListOf<String>()
        val otherFields = mutableListOf<String>()
        fields.forEach { field ->
            val fieldLower = field.lowercase()
            if (fieldLower == "id") {
                idFields.add(field)
            } else if (priorityNames[fieldLower] != null) {
                nameFields.add(fieldLower)
            } else if (fieldLower.endsWith("_id")) {
                refFields.add(field)
            } else {
                otherFields.add(fieldLower)
            }
        }
        return mutableListOf(
            *idFields.toTypedArray(),
            *nameFields.toTypedArray(),
            *otherFields.toTypedArray(),
            *refFields.toTypedArray()
        )
    }

    fun interfaceToValue(v: Any?): Value = when (v) {
        null -> Value.NULL()
        is Boolean -> Value.BOOL(v)
        is Double -> if (v == v.toLong().toDouble()) Value.INT(v.toLong()) else Value.FLOAT(v)
        is Int -> Value.INT(v.toLong())
        is Long -> Value.INT(v)
        is String -> Value.STRING(v)
        else -> Value.STRING("$v")
    }

    private fun javaTypeToType(t: String): String = when(t) {
        "integer", "long" -> "int"
        "double" -> "float"
        "string" -> "string"
        else -> t
    }



    private fun splitLines(text: String): List<String> {
        val lines = text.split("\n")
        return lines.map { it.trimEnd('\r') }
    }

}
