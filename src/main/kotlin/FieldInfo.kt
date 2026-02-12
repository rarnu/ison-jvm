package com.rarnu.ison

/**
 * represents information about a field/column in an ISON block
 */
data class FieldInfo @JvmOverloads constructor(
    var name: String = "",
    /**
     * "int", "float", "bool", "string", "ref", "computed", or ""
     */
    var typeHint: String = ""
)