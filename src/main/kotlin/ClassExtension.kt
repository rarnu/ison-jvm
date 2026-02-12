package com.rarnu.ison

import com.isyscore.kotlin.common.toJson
import com.isyscore.kotlin.common.toObj
import org.apache.commons.lang3.BooleanUtils.forEach
import kotlin.collections.first
import kotlin.collections.forEach
import kotlin.collections.isNotEmpty

inline fun <reified T> List<T>.toIson(table: String): String {
    val doc = Document()
    val block = Block("table", table)

    if (isNotEmpty()) {
        var fields = first().toJson().toObj<Map<String, *>>().map { (k, _) -> k }
        fields = ISON.smartOrderFields(fields)
        fields.forEach { block.addField(it, "") }
        forEach { item ->
            val mItem = item.toJson().toObj<Map<String, *>>()
            val row: Row = mItem.mapValues { (_, v) -> ISON.interfaceToValue(v) }.toMutableMap()
            block.addRow(row)
        }
    }

    doc.addBlock(block)
    return Dump.dumps(doc)
}

inline fun <reified T> T.toIson(obj: String): String {
    val doc = Document()
    val block = Block("object", obj)
    val m = toJson().toObj<Map<String, *>>()
    var fields = m.map { (k, _) -> k }
    fields = ISON.smartOrderFields(fields)
    fields.forEach { block.addField(it, "") }
    val row: Row = m.mapValues { (_, v) -> ISON.interfaceToValue(v) }.toMutableMap()
    block.addRow(row)
    doc.addBlock(block)
    return Dump.dumps(doc)
}

inline fun <reified T> String.toIsonTable(table: String): List<T> {
    val doc = ISON.parse(this)
    val block = doc.get(table) ?: return listOf()
    if (block.kind != "table") return listOf()
    return block.rows.map { row ->
        row.mapValues { (_, v) -> v.json() }.toJson().toObj<T>()
    }
}

inline fun <reified T> String.toIsonObj(obj: String): T? {
    val doc = ISON.parse(this)
    val block = doc.get(obj) ?: return null
    if (block.kind != "object") return null
    if (block.rows.isEmpty()) return null
    return block.rows.first().mapValues { (_, v) -> v.json() }.toJson().toObj<T>()
}
