@file:Suppress("DuplicatedCode")

package com.rarnu.ison

/**
 * handles parsing ISON text into Document structures
 */
class Parser(
    var text: String,
    var lines: List<String>,
    var pos: Int
) {

    companion object {
        fun isValidKind(kind: String): Boolean = kind == "table" || kind == "object" || kind == "meta"

        fun parseFieldDef(field: String): Pair<String, String> {
            val idx = field.indexOf(":")
            return if (idx > 0) {
                field.substring(0, idx) to field.substring(idx + 1)
            } else {
                field to ""
            }
        }

        fun tokenizeLine(line: String): List<String> {
            val tokens = mutableListOf<String>()
            val current = StringBuilder()
            var inQuotes = false
            var escaped = false

            for (i in line.indices) {
                val ch = line[i]
                if (escaped) {
                    when(ch) {
                        'n' -> current.append('\n')
                        't' -> current.append('\t')
                        '"' -> current.append('"')
                        '\\' -> current.append('\\')
                        else -> current.append(ch)
                    }
                    escaped = false
                    continue
                }
                if (ch == '\\' && inQuotes) {
                    escaped = true
                    continue
                }

                if (ch == '"') {
                    inQuotes = !inQuotes
                    continue
                }
                if (!inQuotes && (ch == ' ' || ch == '\t')) {
                    if (current.isNotEmpty()) {
                        tokens.add(current.toString())
                        current.clear()
                    }
                    continue
                }
                current.append(ch)
            }

            if (current.isNotEmpty()) {
                tokens.add(current.toString())
            }

            return tokens
        }

        fun parseReference(token: String): Reference {
            if (!token.startsWith(":")) {
                return Reference(id = token)
            }
            val token = token.drop(1)
            val parts = token.splitN(":", 2)
            if (parts.size == 1) {
                return Reference(id = parts[0])
            }
            val namespace = parts[0]
            val id = parts[1]
            // Check if it's a relationship (all uppercase)
            if (namespace.uppercase() == namespace && namespace.isNotEmpty()) {
                var isUpper = true
                for (r in namespace) {
                    if (r != '_' && (r !in 'A'..'Z')) {
                        isUpper = false
                        break
                    }
                }
                if (isUpper) {
                    return Reference(id = id, relationship = namespace)
                }
            }
            return Reference(id = id, namespace = namespace)
        }

        fun parseValue(token: String, typeHint: String): Value {
            // Null
            if (token == "~" || token == "null" || token == "NULL") {
                return Value.NULL()
            }
            // Boolean
            if (token == "true" || token == "TRUE") {
                return Value.BOOL(true)
            }
            if (token == "false" || token == "FALSE") {
                return Value.BOOL(false)
            }
            // Reference
            if (token.startsWith(":")) {
                val ref = parseReference(token)
                return Value.REF(ref)
            }
            // Type hint handling
            when(typeHint) {
                "int" -> {
                    val v = token.toLongOrNull()
                    if (v != null) {
                        return Value.INT(v)
                    }
                }
                "float" -> {
                    val v = token.toDoubleOrNull()
                    if (v != null) {
                        return Value.FLOAT(v)
                    }
                }
                "bool" -> {
                    if (token == "1") {
                        return Value.BOOL(true)
                    }
                    if (token == "0") {
                        return Value.BOOL(false)
                    }
                }
                "string" -> return Value.STRING(token)
                "ref" -> {
                    if (token.startsWith(":")) {
                        return Value.REF(parseReference(token))
                    }
                    return Value.STRING(token)
                }
            }
            // Auto-inference
            // Try integer
            val vi = token.toLongOrNull()
            if (vi != null) {
                return Value.INT(vi)
            }
            // Try float
            val vf = token.toDoubleOrNull()
            if (vf != null) {
                return Value.FLOAT(vf)
            }
            // Default to string
            return Value.STRING(token)
        }

    }

    fun parse(): Document {
        val doc = Document()

        while (pos < lines.size) {
            val line = lines[pos].trim()
            // Skip empty lines and comments
            if (line.isBlank() || line.startsWith("#")) {
                pos++
                continue
            }
            // Check for block header
            if (line.contains(".") && !line.startsWith("\"")) {
                val parts = line.splitN(".", 2)
                if (parts.size == 2 && isValidKind(parts[0])) {
                    val block = parseBlock(parts[0], parts[1])
                    doc.addBlock(block)
                    continue
                }
            }
            pos++
        }
        return doc
    }

    fun parseBlock(kine: String, name: String): Block {
        val block = Block(kine, name)
        pos++
        // Parse field definitions (next non-empty, non-comment line)
        while (pos < lines.size) {
            val line = lines[pos].trim()
            if (line.isBlank() || line.startsWith("#")) {
                pos++
                continue
            }
            break
        }
        if (pos >= lines.size) {
            return block
        }
        // Parse fields
        val fieldsLine = lines[pos].trim()
        val fs = tokenizeLine(fieldsLine)
        fs.forEach { field ->
            val (name, typeHint) = parseFieldDef(field)
            block.addField(name, typeHint)
        }
        pos++

        // Parse rows
        var inSummary = false
        while (pos < lines.size) {
            val line = lines[pos].trim()
            // Empty line ends block
            if (line.isBlank()) {
                pos++
                break
            }
            // Comment
            if (line.startsWith("#")) {
                pos++
                continue
            }
            // New block starts
            if (line.contains(".") && !line.startsWith("\"")) {
                val parts = line.splitN(".", 2)
                if (parts.size == 2 && isValidKind(parts[0])) {
                    break
                }
            }
            // Summary separator
            if (line == "---") {
                inSummary = true
                pos++
                continue
            }
            // Parse row
            val tokens = tokenizeLine(line)
            val row: Row = mutableMapOf()
            tokens.forEachIndexed { i, token ->
                if (i < block.fields.size) {
                    val fieldInfo = block.fields[i]
                    val v = parseValue(token, fieldInfo.typeHint)
                    row[fieldInfo.name] = v
                }
            }
            if (inSummary) {
                block.summaryRow = row
            } else {
                block.addRow(row)
            }
            pos++
        }
        return block
    }

}