package com.rarnu.ison

import com.isyscore.kotlin.common.toJson

/**
 * represents an ISON value which can be null, bool, int, float, string, or reference
 */
data class Value @JvmOverloads constructor(
    var type: ValueType,
    var boolVal: Boolean = false,
    var intVal: Long = 0L,
    var floatVal: Double = 0.0,
    var stringVal: String = "",
    var refVal: Reference = Reference()
) {

    companion object {
        /**
         * creates a null Value
         */
        @JvmStatic
        fun NULL(): Value = Value(type = ValueType.TypeNull)

        /**
         * creates a boolean Value
         */
        @JvmStatic
        fun BOOL(v: Boolean): Value = Value(type = ValueType.TypeBool, boolVal = v)

        /**
         * creates an integer Value
         */
        @JvmStatic
        fun INT(v: Long): Value = Value(type = ValueType.TypeInt, intVal = v)

        /**
         * creates a float Value
         */
        @JvmStatic
        fun FLOAT(v: Double): Value = Value(type = ValueType.TypeFloat, floatVal = v)

        /**
         * creates a string Value
         */
        @JvmStatic
        fun STRING(v: String): Value = Value(type = ValueType.TypeString, stringVal = v)

        /**
         * creates a reference Value
         */
        @JvmStatic
        fun REF(v: Reference): Value = Value(type = ValueType.TypeReference, refVal = v)

    }

    /**
     * returns true if the value is null
     */
    fun isNull(): Boolean = type == ValueType.TypeNull

    /**
     * returns the boolean value
     */
    fun asBool(): Boolean? = if (type == ValueType.TypeBool) boolVal else null

    /**
     * returns the integer value
     */
    fun asInt(): Long? = if (type == ValueType.TypeInt) intVal else null

    /**
     * returns the float value
     */
    fun asFloat(): Double? = if (type == ValueType.TypeFloat) floatVal else if (type == ValueType.TypeInt) intVal.toDouble() else null

    /**
     * returns the string value
     */
    fun asString(): String? = if (type == ValueType.TypeString) stringVal else null

    /**
     * returns the reference value
     */
    fun asRef(): Reference? = if (type == ValueType.TypeReference) refVal else null

    /**
     * returns the ANY representation of the value
     */
    fun intf(): Any? = when (type) {
        ValueType.TypeBool -> boolVal
        ValueType.TypeInt -> intVal
        ValueType.TypeFloat -> floatVal
        ValueType.TypeString -> stringVal
        ValueType.TypeReference -> refVal
        else -> null
    }

    /**
     * converts the value to its ISON string representation
     */
    fun toIson(): String = when (type) {
        ValueType.TypeNull -> "~"
        ValueType.TypeBool -> if (boolVal) "true" else "false"
        ValueType.TypeInt -> "$intVal"
        ValueType.TypeFloat -> "$floatVal"
        ValueType.TypeString -> if (stringVal.containsAny(" ", "\t", "\n", "\"") || stringVal.isBlank()) {
            var escaped = stringVal.replace("\\", "\\\\")
            escaped = escaped.replace("\"", "\\\"")
            escaped = escaped.replace("\n", "\\n")
            escaped = escaped.replace("\t", "\\t")
            "\"${escaped}\""
        } else stringVal

        ValueType.TypeReference -> refVal.toIson()
    }

    fun json(): String = intf().toJson()

}