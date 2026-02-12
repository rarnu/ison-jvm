package com.rarnu.ison

import java.io.File

/**
 * configures serialization behavior
 */
data class DumpsOptions @JvmOverloads constructor(
    /**
     * Pad columns for visual alignment
     */
    var alignColumns: Boolean = false,
    /**
     * Column separator (default: " ")
     */
    var delimiter: String = " "
)

object Dump {
    /**
     * returns default serialization options
     */
    @JvmStatic
    fun defaultDumpsOptions(): DumpsOptions = DumpsOptions(false, " ")

    /**
     * serializes a Document and writes it to a file
     */
    @JvmStatic
    fun dump(doc: Document, path: String) {
        dump(doc, File(path))
    }

    /**
     * serializes a Document and writes it to a file
     */
    @JvmStatic
    fun dump(doc: Document, file: File) {
        val text = dumps(doc)
        file.writeText(text)
    }

    /**
     * serializes a Document back to ISON format
     */
    @JvmStatic
    fun dumps(doc: Document): String = dumpsWithOptions(doc, defaultDumpsOptions())

    /**
     * serializes a Document with options and writes to a file
     */
    @JvmStatic
    fun dumpWithOptions(doc: Document, path: String, opts: DumpsOptions) {
        dumpWithOptions(doc, File(path), opts)
    }

    /**
     * serializes a Document with options and writes to a file
     */
    @JvmStatic
    fun dumpWithOptions(doc: Document, file: File, opts: DumpsOptions) {
        val text = dumpsWithOptions(doc, opts)
        file.writeText(text)
    }

    /**
     * serializes a Document with specified options
     */
    @JvmStatic
    fun dumpsWithOptions(doc: Document, opts: DumpsOptions): String {
        val sb = StringBuilder()
        var delim = opts.delimiter
        if (delim == "") {
            delim = " "
        }

        doc.order.forEachIndexed { i, name ->
            if (i > 0) {
                sb.append("\n")
            }
            val block = doc.blocks[name]!!
            sb.append("${block.kind}.${block.name}\n")
            // Write field headers
            block.fields.forEachIndexed { j, field ->
                if (j > 0) {
                    sb.append(delim)
                }
                if (field.typeHint.isNotBlank()) {
                    sb.append("${field.name}:${field.typeHint}")
                } else {
                    sb.append(field.name)
                }
            }
            sb.append("\n")
            // Calculate column widths for alignment
            val widths = block.fields.map { field ->
                var w = field.name.length
                if (field.typeHint.isNotBlank()) {
                    w += field.typeHint.length + 1
                }
                w
            }.toMutableList()
            block.rows.forEach { row ->
                block.fields.forEachIndexed { j, field ->
                    val v = row[field.name]
                    if (v != null) {
                        val w = v.toIson().length
                        if (w > widths[j]) {
                            widths[j] = w
                        }
                    }
                }
            }
            // Write rows
            block.rows.forEach { row ->
                block.fields.forEachIndexed { j, field ->
                    if (j > 0) {
                        sb.append(delim)
                    }
                    val v = row[field.name]
                    if (v != null) {
                        sb.append(v.toIson())
                    } else {
                        sb.append("~")
                    }
                }
                sb.append("\n")
            }

            // Write summary if present
            if (block.summaryRow != null) {
                sb.append("---\\n")
                block.fields.forEachIndexed { j, field ->
                    if (j > 0) {
                        sb.append(delim)
                    }
                    val v = block.summaryRow!![field.name]
                    if (v != null) {
                        sb.append(v.toIson())
                    } else {
                        sb.append("~")
                    }
                }
                sb.append("\n")
            }

        }
        return sb.toString()
    }

    /**
     * serializes a Document to ISONL (line-based streaming format)
     */
    @JvmStatic
    fun dumpsISONL(doc: Document): String {
        val sb = StringBuilder()
        doc.order.forEach { name ->
            val block = doc.blocks[name]!!
            // Build field header
            val fieldHeader = StringBuilder()
            block.fields.forEachIndexed { i, field ->
                if (i > 0) {
                    fieldHeader.append(" ")
                }
                if (field.typeHint.isNotBlank()) {
                    fieldHeader.append("${field.name}:${field.typeHint}")
                } else {
                    fieldHeader.append(field.name)
                }
            }
            val fields = fieldHeader.toString()

            // Write each row as a separate line
            block.rows.forEach { row ->
                sb.append("${block.kind}.${block.name}|${fields}|")
                block.fields.forEachIndexed { i, field ->
                    if (i > 0) {
                        sb.append(" ")
                    }
                    val v = row[field.name]
                    if (v != null) {
                        sb.append(v.toIson())
                    } else {
                        sb.append("~")
                    }
                }
                sb.append("\n")
            }
        }
        return sb.toString()
    }

    @JvmStatic
    fun dumpISONL(doc: Document, path: String) {
        dumpISONL(doc, File(path))
    }

    /**
     * serializes a Document and writes it to an ISONL file
     */
    @JvmStatic
    fun dumpISONL(doc: Document, file: File) {
        val text = dumpsISONL(doc)
        file.writeText(text)
    }

}
