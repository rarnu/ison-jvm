package com.rarnu.ison

/**
 * represents a validation error with field path and message
 */
class ValidationError(val field: String = "", message: String = "", val value: Any?): Exception("${field}: $message") {

    fun error(): String = "${field}: $message"
}

/**
 * a collection of validation errors
 */
class ValidationErrors: Exception() {

    val errors: MutableList<ValidationError> = mutableListOf()

    fun error(): String = errors.joinToString("; ") {
        it.error()
    }

    /**
     * returns true if there are any validation errors
     */
    fun hasErrors(): Boolean = errors.isNotEmpty()

}