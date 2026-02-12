package com.rarnu.ison

interface Schema {
    fun validate(v: Any?): Exception?
    fun isOptional(): Boolean
    fun getDefault(): Pair<Any?, Boolean>
    fun getDescription(): String
}

/**
 * provides common functionality for all schemas
 */
abstract class BaseSchema : Schema {

    private var optional: Boolean = false
    private var defaultValue: Any? = null
    private var hasDefault: Boolean = false
    private var description: String = ""

    private val refinements: MutableList<(Any?) -> Exception?> = mutableListOf()

    protected fun setOptional() {
        optional = true
    }

    protected fun setDefault(v: Any?) {
        defaultValue = v
        hasDefault = true
    }

    protected fun setDescription(desc: String) {
        description = desc
    }

    protected fun addRefinement(f: (Any?) -> Exception?) {
        refinements.add(f)
    }

    override fun isOptional(): Boolean = optional

    override fun getDefault(): Pair<Any?, Boolean> = defaultValue to hasDefault

    override fun getDescription(): String = description

    protected fun runRefinements(v: Any?): Exception? {
        for (f in refinements) {
            val err = f(v)
            if (err != null) {
                return err
            }
        }
        return null
    }
}

class StringSchema : BaseSchema() {

    private var minLen: Int? = null
    private var maxLen: Int? = null
    private var exactLen: Int? = null
    private var pattern: Regex? = null
    private var isEmail: Boolean = false
    private var isURL: Boolean = false

    fun min(n: Int): StringSchema {
        this.minLen = n
        return this
    }

    fun max(n: Int): StringSchema {
        this.maxLen = n
        return this
    }

    fun length(n: Int): StringSchema {
        this.exactLen = n
        return this
    }

    fun email(): StringSchema {
        this.isEmail = true
        return this
    }

    fun url(): StringSchema {
        this.isURL = true
        return this
    }

    fun regex(pattern: Regex): StringSchema {
        this.pattern = pattern
        return this
    }

    fun optional(): StringSchema {
        setOptional()
        return this
    }

    fun default(v: String): StringSchema {
        setDefault(v)
        return this
    }

    fun describe(desc: String): StringSchema {
        setDescription(desc)
        return this
    }

    fun refine(fn: (String) -> Boolean, msg: String): StringSchema {
        addRefinement { v ->
            if (v is String) {
                if (!fn(v)) return@addRefinement Exception(msg)
            }
            null
        }
        return this
    }

    override fun validate(v: Any?): Exception? {
        if (v == null) {
            return if (isOptional()) null else Exception("required field is missing")
        }

        val str = v as? String ?: return Exception("expected string, got ${v::class.simpleName}")

        if (minLen != null && str.length < minLen!!) {
            return Exception("string must be at least $minLen characters")
        }

        if (maxLen != null && str.length > maxLen!!) {
            return Exception("string must be at most $maxLen characters")
        }

        if (exactLen != null && str.length != exactLen!!) {
            return Exception("string must be exactly $exactLen characters")
        }

        if (isEmail) {
            val emailPattern = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
            if (!emailPattern.matches(str)) {
                return Exception("invalid email format")
            }
        }

        if (isURL) {
            val urlPattern = Regex("^https?://[^\\s/$.?#].[^\\s]*$")
            if (!urlPattern.matches(str)) {
                return Exception("invalid URL format")
            }
        }

        if (pattern != null && !pattern!!.matches(str)) {
            return Exception("string does not match required pattern")
        }

        return runRefinements(v)
    }

}


class NumberSchema : BaseSchema() {

    private var minVal: Double? = null
    private var maxVal: Double? = null
    private var isInt: Boolean = false
    private var isPositive: Boolean = false
    private var isNegative: Boolean = false

    companion object {
        @JvmStatic
        fun INT(): NumberSchema = NumberSchema().apply { isInt = true }

        @JvmStatic
        fun FLOAT(): NumberSchema = NumberSchema()
    }

    fun min(n: Double): NumberSchema {
        this.minVal = n
        return this
    }

    fun max(n: Double): NumberSchema {
        this.maxVal = n
        return this
    }

    fun positive(): NumberSchema {
        this.isPositive = true
        return this
    }

    fun negative(): NumberSchema {
        this.isNegative = true
        return this
    }

    fun optional(): NumberSchema {
        setOptional()
        return this
    }

    fun default(v: Double): NumberSchema {
        setDefault(v)
        return this
    }

    fun describe(desc: String): NumberSchema {
        setDescription(desc)
        return this
    }

    fun refine(fn: (Double) -> Boolean, msg: String): NumberSchema {
        addRefinement { v ->
            val num = when (v) {
                is Double -> v
                is Float -> v.toDouble()
                is Long -> v.toDouble()
                is Int -> v.toDouble()
                else -> return@addRefinement null
            }
            if (!fn(num)) return@addRefinement Exception(msg)
            null
        }
        return this
    }

    override fun validate(v: Any?): Exception? {
        if (v == null) {
            return if (isOptional()) null else Exception("required field is missing")
        }

        val num = when (v) {
            is Double -> v
            is Float -> v.toDouble()
            is Long -> v.toDouble()
            is Int -> v.toDouble()
            else -> return Exception("expected number, got ${v::class.simpleName}")
        }

        if (isInt) {
            if (num != num.toLong().toDouble()) {
                return Exception("expected integer, got float")
            }
        }

        if (minVal != null && num < minVal!!) {
            return Exception("number must be at least $minVal")
        }

        if (maxVal != null && num > maxVal!!) {
            return Exception("number must be at most $maxVal")
        }

        if (isPositive && num <= 0) {
            return Exception("number must be positive")
        }

        if (isNegative && num >= 0) {
            return Exception("number must be negative")
        }

        return runRefinements(v)
    }

}

class BooleanSchema : BaseSchema() {

    fun optional(): BooleanSchema {
        setOptional()
        return this
    }

    fun default(v: Boolean): BooleanSchema {
        setDefault(v)
        return this
    }

    fun describe(desc: String): BooleanSchema {
        setDescription(desc)
        return this
    }

    override fun validate(v: Any?): Exception? {
        if (v == null) {
            return if (isOptional()) null else Exception("required field is missing")
        }

        if (v !is Boolean) {
            return Exception("expected boolean, got ${v::class.simpleName}")
        }

        return runRefinements(v)
    }

}

class NullSchema : BaseSchema() {

    override fun validate(v: Any?): Exception? = if (v != null) {
        Exception("expected null, got ${v::class.simpleName}")
    } else {
        null
    }

}

class RefSchema : BaseSchema() {

    private var namespace: String? = null
    private var relationship: String? = null

    fun namespace(ns: String): RefSchema {
        this.namespace = ns
        return this
    }

    fun relationship(rel: String): RefSchema {
        this.relationship = rel
        return this
    }

    fun optional(): RefSchema {
        setOptional()
        return this
    }

    fun describe(desc: String): RefSchema {
        setDescription(desc)
        return this
    }

    override fun validate(v: Any?): Exception? {
        if (v == null) {
            return if (isOptional()) null else Exception("required field is missing")
        }

        when (v) {
            is Map<*, *> -> {
                // Reference object format
                if (!v.containsKey("_ref")) {
                    return Exception("expected reference object with _ref field")
                }
                if (namespace != null) {
                    val ns = v["_namespace"] as? String
                    if (ns != namespace) {
                        return Exception("expected namespace $namespace")
                    }
                }
                if (relationship != null) {
                    val rel = v["_relationship"] as? String
                    if (rel != relationship) {
                        return Exception("expected relationship $relationship")
                    }
                }
            }

            is String -> {
                // String reference format (:id, :ns:id, :REL:id)
                if (!v.startsWith(":")) {
                    return Exception("expected reference string starting with ':'")
                }
                null
            }

            else -> return Exception("expected reference, got ${v::class.simpleName}")
        }

        return runRefinements(v)
    }
}


class ObjectSchema : BaseSchema() {

    private var fields: MutableMap<String, Schema> = mutableMapOf()

    companion object {
        @JvmStatic
        fun OBJECT(f: MutableMap<String, Schema>): ObjectSchema = ObjectSchema().apply { fields = f }
    }

    fun optional(): ObjectSchema {
        setOptional()
        return this
    }

    fun describe(desc: String): ObjectSchema {
        setDescription(desc)
        return this
    }

    fun extend(f: Map<String, Schema>): ObjectSchema = OBJECT((fields + f).toMutableMap())

    fun pick(vararg keys: String): ObjectSchema = OBJECT(fields.filterKeys { keys.contains(it) }.toMutableMap())

    fun omit(vararg keys: String): ObjectSchema = OBJECT(fields.filterKeys { !keys.contains(it) }.toMutableMap())

    override fun validate(v: Any?): Exception? {
        if (v == null) {
            return if (isOptional()) null else Exception("required field is missing")
        }

        val obj = (v as? Map<*, *> ?: return Exception("expected object, got ${v::class.simpleName}")).toMutableMap()

        val errs = ValidationErrors()
        for ((name, schema) in fields) {
            val fieldValue = obj[name]
            if (fieldValue == null && !schema.isOptional()) {
                val (def, hasDefault) = schema.getDefault()
                if (hasDefault) {
                    obj[name] = def
                    continue
                }
            }
            val err = schema.validate(fieldValue)
            if (err != null) {
                errs.errors.add(
                    ValidationError(
                        field = name,
                        message = err.message ?: "unknown error",
                        value = fieldValue
                    )
                )
            }
        }

        if (errs.hasErrors()) {
            return errs
        }

        return runRefinements(v)
    }

}


class ArraySchema(private var itemSchema: Schema): BaseSchema() {

    private var minLen: Int? = null
    private var maxLen: Int? = null

    fun min(n: Int): ArraySchema {
        this.minLen = n
        return this
    }

    fun max(n: Int): ArraySchema {
        this.maxLen = n
        return this
    }

    fun optional(): ArraySchema {
        setOptional()
        return this
    }

    fun describe(desc: String): ArraySchema {
        setDescription(desc)
        return this
    }

    override fun validate(v: Any?): Exception? {
        if (v == null) {
            return if (isOptional()) null else Exception("required field is missing")
        }

        val arr = v as? List<*> ?: return Exception("expected array, got ${v::class.simpleName}")

        if (minLen != null && arr.size < minLen!!) {
            return Exception("array must have at least $minLen items")
        }

        if (maxLen != null && arr.size > maxLen!!) {
            return Exception("array must have at most $maxLen items")
        }

        val errs = ValidationErrors()
        for ((i, item) in arr.withIndex()) {
            val err = itemSchema.validate(item)
            if (err != null) {
                errs.errors.add(ValidationError(
                    field = "[$i]",
                    message = err.message ?: "unknown error",
                    value = item
                ))
            }
        }

        if (errs.hasErrors()) {
            return errs
        }

        return runRefinements(v)
    }

}

class TableSchema(
    private var name: String,
    private val fields: MutableMap<String, Schema>
): BaseSchema() {

    private val rowSchema = ObjectSchema.OBJECT(fields)

    fun optional(): TableSchema {
        setOptional()
        return this
    }

    fun describe(desc: String): TableSchema {
        setDescription(desc)
        return this
    }

    fun getName(): String = name

    override fun validate(v: Any?): Exception? {
        if (v == null) {
            return if (isOptional()) null else Exception("required table is missing")
        }

        return when (v) {
            is Map<*, *> -> {
                // Block format with rows array
                val rows = v["rows"] as? List<*> ?: return Exception("expected table with rows array")
                validateRows(rows)
            }
            is List<*> -> {
                // Direct array of rows
                validateRows(v)
            }
            else -> Exception("expected table, got ${v::class.simpleName}")
        }
    }

    private fun validateRows(rows: List<*>): Exception? {
        val errs = ValidationErrors()
        for ((i, row) in rows.withIndex()) {
            val rowMap = row as? Map<*, *>
            if (rowMap == null) {
                errs.errors.add(ValidationError(
                    field = "row[$i]",
                    message = "expected row object",
                    value = row
                ))
                continue
            }
            val err = rowSchema.validate(rowMap)
            if (err != null) {
                if (err is ValidationErrors) {
                    for (e in err.errors) {
                        errs.errors.add(ValidationError(
                            field = "row[$i].${e.field}",
                            message = e.error(),
                            value = e.value
                        ))
                    }
                } else {
                    errs.errors.add(ValidationError(
                        field = "row[$i]",
                        message = err.message ?: "unknown error",
                        value = row
                    ))
                }
            }
        }

        if (errs.hasErrors()) {
            return errs
        }

        return runRefinements(rows)
    }

}

data class SafeParseResult(
    val success: Boolean = false,
    val data: Map<String, Any>? = null,
    val error: Exception? = null
)

class DocumentSchema(private val blocks: Map<String, Schema>) {

    fun parse(value: Map<String, Any>): Pair<Map<String, Any>?, Exception?> {
        val errs = ValidationErrors()

        for ((name, schema) in blocks) {
            val blockValue = value[name]
            val err = schema.validate(blockValue)
            if (err != null) {
                if (err is ValidationErrors) {
                    for (e in err.errors) {
                        errs.errors.add(ValidationError(
                            field = "$name.${e.field}",
                            message = e.error(),
                            value = e.value
                        ))
                    }
                } else {
                    errs.errors.add(ValidationError(
                        field = name,
                        message = err.message ?: "unknown error",
                        value = blockValue
                    ))
                }
            }
        }

        if (errs.hasErrors()) {
            return null to errs
        }

        return value to null
    }

    fun safeParse(value: Map<String, Any>): SafeParseResult {
        val (data, err) = parse(value)
        return if (err != null) {
            SafeParseResult(success = false, data = null, error = err)
        } else {
            SafeParseResult(success = true, data = data, error = null)
        }
    }

}