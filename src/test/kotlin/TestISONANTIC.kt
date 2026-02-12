package com.rarnu.ison.test


import com.rarnu.ison.ArraySchema
import com.rarnu.ison.BooleanSchema
import com.rarnu.ison.DocumentSchema
import com.rarnu.ison.I
import com.rarnu.ison.NullSchema
import com.rarnu.ison.NumberSchema
import com.rarnu.ison.ObjectSchema
import com.rarnu.ison.RefSchema
import com.rarnu.ison.StringSchema
import com.rarnu.ison.TableSchema
import com.rarnu.ison.ValidationError
import com.rarnu.ison.ValidationErrors
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

class TestISONANTIC {
    @Test
    fun testStringRequired() {
        val schema = StringSchema()

        var err = schema.validate("hello")
        assertNull(err)

        err = schema.validate(null)
        assertNotNull(err)
        assert(err?.message?.contains("required") == true)
    }

    @Test
    fun testStringOptional() {
        val schema = StringSchema().optional()

        var err = schema.validate(null)
        assertNull(err)

        err = schema.validate("hello")
        assertNull(err)
    }

    @Test
    fun testStringMinLength() {
        val schema = StringSchema().min(5)

        var err = schema.validate("hello")
        assertNull(err)

        err = schema.validate("hi")
        assertNotNull(err)
        assert(err?.message?.contains("at least 5") == true)
    }

    @Test
    fun testStringMaxLength() {
        val schema = StringSchema().max(5)

        var err = schema.validate("hello")
        assertNull(err)

        err = schema.validate("hello world")
        assertNotNull(err)
        assert(err?.message?.contains("at most 5") == true)
    }

    @Test
    fun testStringExactLength() {
        val schema = StringSchema().length(5)

        var err = schema.validate("hello")
        assertNull(err)

        err = schema.validate("hi")
        assertNotNull(err)
        assert(err?.message?.contains("exactly 5") == true)
    }

    @Test
    fun testStringEmail() {
        val schema = StringSchema().email()

        var err = schema.validate("test@example.com")
        assertNull(err)

        err = schema.validate("invalid-email")
        assertNotNull(err)
        assert(err?.message?.contains("invalid email") == true)
    }

    @Test
    fun testStringURL() {
        val schema = StringSchema().url()

        var err = schema.validate("https://example.com")
        assertNull(err)

        err = schema.validate("http://example.com/path")
        assertNull(err)

        err = schema.validate("not-a-url")
        assertNotNull(err)
        assert(err?.message?.contains("invalid URL") == true)
    }

    @Test
    fun testStringRegex() {
        val pattern = Regex("^[A-Z]{2,3}$")
        val schema = StringSchema().regex(pattern)

        var err = schema.validate("AB")
        assertNull(err)

        err = schema.validate("ABC")
        assertNull(err)

        err = schema.validate("A")
        assertNotNull(err)

        err = schema.validate("ABCD")
        assertNotNull(err)
    }

    @Test
    fun testStringDefault() {
        val schema = StringSchema().default("default")

        val (def, hasDefault) = schema.getDefault()
        assertTrue(hasDefault)
        assertEquals("default", def)
    }

    @Test
    fun testStringDescribe() {
        val schema = StringSchema().describe("User's name")

        assertEquals("User's name", schema.getDescription())
    }

    @Test
    fun testStringRefine() {
        val schema = StringSchema().refine({ s -> s[0].isUpperCase() }, "must start with uppercase")

        var err = schema.validate("Hello")
        assertNull(err)

        err = schema.validate("hello")
        assertNotNull(err)
        assert(err?.message?.contains("must start with uppercase") == true)
    }

    // Number Schema Tests

    @Test
    fun testNumberRequired() {
        val schema = NumberSchema()

        var err = schema.validate(42.5)
        assertNull(err)

        err = schema.validate(null)
        assertNotNull(err)
    }

    @Test
    fun testNumberOptional() {
        val schema = NumberSchema().optional()

        val err = schema.validate(null)
        assertNull(err)
    }

    @Test
    fun testIntSchema() {
        val schema = NumberSchema.INT()

        var err = schema.validate(42L)
        assertNull(err)

        err = schema.validate(42.5)
        assertNotNull(err)
        assert(err?.message?.contains("expected integer") == true)
    }

    @Test
    fun testNumberMin() {
        val schema = NumberSchema().min(10.0)

        var err = schema.validate(10.0)
        assertNull(err)

        err = schema.validate(5.0)
        assertNotNull(err)
        assert(err?.message?.contains("at least") == true)
    }

    @Test
    fun testNumberMax() {
        val schema = NumberSchema().max(10.0)

        var err = schema.validate(10.0)
        assertNull(err)

        err = schema.validate(15.0)
        assertNotNull(err)
        assert(err?.message?.contains("at most") == true)
    }

    @Test
    fun testNumberPositive() {
        val schema = NumberSchema().positive()

        var err = schema.validate(5.0)
        assertNull(err)

        err = schema.validate(0.0)
        assertNotNull(err)

        err = schema.validate(-5.0)
        assertNotNull(err)
        assert(err?.message?.contains("positive") == true)
    }

    @Test
    fun testNumberNegative() {
        val schema = NumberSchema().negative()

        var err = schema.validate(-5.0)
        assertNull(err)

        err = schema.validate(0.0)
        assertNotNull(err)

        err = schema.validate(5.0)
        assertNotNull(err)
        assert(err?.message?.contains("negative") == true)
    }

    @Test
    fun testNumberRefine() {
        val schema = NumberSchema().refine({ n -> n.toInt() % 2 == 0 }, "must be even")

        var err = schema.validate(4.0)
        assertNull(err)

        err = schema.validate(3.0)
        assertNotNull(err)
        assert(err?.message?.contains("must be even") == true)
    }

    // Boolean Schema Tests

    @Test
    fun testBooleanRequired() {
        val schema = BooleanSchema()

        var err = schema.validate(true)
        assertNull(err)

        err = schema.validate(false)
        assertNull(err)

        err = schema.validate(null)
        assertNotNull(err)
    }

    @Test
    fun testBooleanOptional() {
        val schema = BooleanSchema().optional()

        val err = schema.validate(null)
        assertNull(err)
    }

    @Test
    fun testBooleanDefault() {
        val schema = BooleanSchema().default(true)

        val (def, hasDefault) = schema.getDefault()
        assertTrue(hasDefault)
        assertEquals(true, def)
    }

    // Null Schema Tests

    @Test
    fun testNullSchema() {
        val schema = NullSchema()

        var err = schema.validate(null)
        assertNull(err)

        err = schema.validate("not null")
        assertNotNull(err)
        assert(err?.message?.contains("expected null") == true)
    }

    // Reference Schema Tests

    @Test
    fun testRefRequired() {
        val schema = RefSchema()

        var err = schema.validate(":1")
        assertNull(err)

        err = schema.validate(mapOf("_ref" to "1"))
        assertNull(err)

        err = schema.validate(null)
        assertNotNull(err)
    }

    @Test
    fun testRefOptional() {
        val schema = RefSchema().optional()

        val err = schema.validate(null)
        assertNull(err)
    }

    @Test
    fun testRefNamespace() {
        val schema = RefSchema().namespace("user")

        var err = schema.validate(mapOf(
            "_ref" to "1",
            "_namespace" to "user"
        ))
        assertNull(err)

        err = schema.validate(mapOf(
            "_ref" to "1",
            "_namespace" to "other"
        ))
        assertNotNull(err)
    }

    @Test
    fun testRefRelationship() {
        val schema = RefSchema().relationship("OWNS")

        var err = schema.validate(mapOf(
            "_ref" to "1",
            "_relationship" to "OWNS"
        ))
        assertNull(err)

        err = schema.validate(mapOf(
            "_ref" to "1",
            "_relationship" to "OTHER"
        ))
        assertNotNull(err)
    }

    @Test
    fun testRefStringFormat() {
        val schema = RefSchema()

        var err = schema.validate(":1")
        assertNull(err)

        err = schema.validate(":user:42")
        assertNull(err)

        err = schema.validate("not-a-ref")
        assertNotNull(err)
    }

    // Object Schema Tests

    @Test
    fun testObjectRequired() {
        val schema = ObjectSchema.OBJECT(mutableMapOf(
                "name" to StringSchema(),
                "age" to NumberSchema.INT()
            )
        )

        var err = schema.validate(mapOf(
            "name" to "Alice",
            "age" to 30L
        ))
        assertNull(err)

        err = schema.validate(null)
        assertNotNull(err)
    }

    @Test
    fun testObjectFieldValidation() {
        val schema = ObjectSchema.OBJECT(mutableMapOf(
                "name" to StringSchema().min(1),
                "email" to StringSchema().email()
            )
        )

        var err = schema.validate(mapOf(
            "name" to "Alice",
            "email" to "alice@example.com"
        ))
        assertNull(err)

        err = schema.validate(mapOf(
            "name" to "",
            "email" to "invalid"
        ))
        assertNotNull(err)
        val verrs = err as? ValidationErrors
        assertNotNull(verrs)
        assertEquals(2, verrs?.errors?.size)
    }

    @Test
    fun testObjectOptionalField() {
        val schema = ObjectSchema.OBJECT(mutableMapOf(
                "name" to StringSchema(),
                "email" to StringSchema().optional()
            )
        )

        val err = schema.validate(mapOf(
            "name" to "Alice"
        ))
        assertNull(err)
    }

    @Test
    fun testObjectExtend() {
        val baseSchema = ObjectSchema.OBJECT(mutableMapOf(
                "id" to NumberSchema.INT(),
                "name" to StringSchema()
            )
        )

        val extendedSchema = baseSchema.extend(mapOf(
            "email" to StringSchema().email()
        ))

        val err = extendedSchema.validate(mapOf(
            "id" to 1L,
            "name" to "Alice",
            "email" to "alice@example.com"
        ))
        assertNull(err)
    }

    @Test
    fun testObjectPick() {
        val schema = ObjectSchema.OBJECT(mutableMapOf(
                "id" to NumberSchema.INT(),
                "name" to StringSchema(),
                "email" to StringSchema()
            )
        )

        val pickedSchema = schema.pick("id", "name")

        val err = pickedSchema.validate(mapOf(
            "id" to 1L,
            "name" to "Alice"
        ))
        assertNull(err)
    }

    @Test
    fun testObjectOmit() {
        val schema = ObjectSchema.OBJECT(mutableMapOf(
                "id" to NumberSchema.INT(),
                "name" to StringSchema(),
                "email" to StringSchema()
            )
        )

        val omittedSchema = schema.omit("email")

        val err = omittedSchema.validate(mapOf(
            "id" to 1L,
            "name" to "Alice"
        ))
        assertNull(err)
    }

    // Array Schema Tests

    @Test
    fun testArrayRequired() {
        val schema = ArraySchema(StringSchema())

        var err = schema.validate(listOf("a", "b", "c"))
        assertNull(err)

        err = schema.validate(null)
        assertNotNull(err)
    }

    @Test
    fun testArrayOptional() {
        val schema = ArraySchema(StringSchema()).optional()

        val err = schema.validate(null)
        assertNull(err)
    }

    @Test
    fun testArrayMinLength() {
        val schema = ArraySchema(StringSchema()).min(2)

        var err = schema.validate(listOf("a", "b"))
        assertNull(err)

        err = schema.validate(listOf("a"))
        assertNotNull(err)
        assert(err?.message?.contains("at least 2") == true)
    }

    @Test
    fun testArrayMaxLength() {
        val schema = ArraySchema(StringSchema()).max(2)

        var err = schema.validate(listOf("a", "b"))
        assertNull(err)

        err = schema.validate(listOf("a", "b", "c"))
        assertNotNull(err)
        assert(err?.message?.contains("at most 2") == true)
    }

    @Test
    fun testArrayItemValidation() {
        val schema = ArraySchema(NumberSchema.INT())

        var err = schema.validate(listOf(1L, 2L, 3L))
        assertNull(err)

        err = schema.validate(listOf(1L, "not an int", 3L))
        assertNotNull(err)
        val verrs = err as? ValidationErrors
        assertNotNull(verrs)
        assertEquals(1, verrs?.errors?.size)
        assertEquals("[1]", verrs?.errors[0]?.field)
    }

    // Table Schema Tests

    @Test
    fun testTableRequired() {
        val schema = TableSchema("users", mutableMapOf(
            "id" to NumberSchema.INT(),
            "name" to StringSchema()
        ))

        var err = schema.validate(listOf(
            mapOf("id" to 1L, "name" to "Alice"),
            mapOf("id" to 2L, "name" to "Bob")
        ))
        assertNull(err)

        err = schema.validate(null)
        assertNotNull(err)
    }

    @Test
    fun testTableOptional() {
        val schema = TableSchema("users", mutableMapOf(
            "id" to NumberSchema.INT(),
            "name" to StringSchema()
        )).optional()

        val err = schema.validate(null)
        assertNull(err)
    }

    @Test
    fun testTableRowValidation() {
        val schema = TableSchema("users", mutableMapOf(
            "id" to NumberSchema.INT(),
            "email" to StringSchema().email()
        ))

        val err = schema.validate(listOf(
            mapOf("id" to 1L, "email" to "alice@example.com"),
            mapOf("id" to 2L, "email" to "invalid")
        ))
        assertNotNull(err)
        val verrs = err as? ValidationErrors
        assertNotNull(verrs)
        assertEquals(1, verrs?.errors?.size)
        assert(verrs?.errors[0]?.field?.contains("row[1]") == true)
    }

    @Test
    fun testTableBlockFormat() {
        val schema = TableSchema("users", mutableMapOf(
            "id" to NumberSchema.INT(),
            "name" to StringSchema()
        ))

        val err = schema.validate(mapOf(
            "kind" to "table",
            "name" to "users",
            "rows" to listOf(
                mapOf("id" to 1L, "name" to "Alice")
            )
        ))
        assertNull(err)
    }

    @Test
    fun testTableGetName() {
        val schema = TableSchema("users", mutableMapOf())
        assertEquals("users", schema.getName())
    }

    // Document Schema Tests

    @Test
    fun testDocumentParse() {
        val schema = DocumentSchema(mutableMapOf(
            "users" to TableSchema("users", mutableMapOf(
                "id" to NumberSchema.INT(),
                "name" to StringSchema()
            )),
            "config" to ObjectSchema.OBJECT(mutableMapOf(
                "debug" to BooleanSchema()
            ))
        ))

        val doc = mapOf(
            "users" to listOf(
                mapOf("id" to 1L, "name" to "Alice")
            ),
            "config" to mapOf(
                "debug" to true
            )
        )

        val (result, err) = schema.parse(doc)
        assertNull(err)
        assertNotNull(result)
    }

    @Test
    fun testDocumentParseErrors() {
        val schema = DocumentSchema(mutableMapOf(
            "users" to TableSchema("users", mutableMapOf(
                "id" to NumberSchema.INT(),
                "email" to StringSchema().email()
            ))
        ))

        val doc = mapOf(
            "users" to listOf(
                mapOf("id" to 1L, "email" to "invalid")
            )
        )

        val (_, err) = schema.parse(doc)
        assertNotNull(err)
        val verrs = err as? ValidationErrors
        assertNotNull(verrs)
        assertEquals(1, verrs?.errors?.size)
    }

    @Test
    fun testDocumentSafeParse() {
        val schema = DocumentSchema(mutableMapOf(
            "users" to TableSchema("users", mutableMapOf(
                "id" to NumberSchema.INT(),
                "name" to StringSchema()
            ))
        ))

        // Valid document
        val doc = mapOf(
            "users" to listOf(
                mapOf("id" to 1L, "name" to "Alice")
            )
        )

        var result = schema.safeParse(doc)
        assertTrue(result.success)
        assertNotNull(result.data)
        assertNull(result.error)

        // Invalid document
        val invalidDoc = mapOf(
            "users" to listOf(
                mapOf("id" to "not-an-int", "name" to "Alice")
            )
        )

        result = schema.safeParse(invalidDoc)
        assertFalse(result.success)
        assertTrue(result.data == null)
        assertNotNull(result.error)
    }

    // I Namespace Tests

    @Test
    fun testINamespace() {
        assertNotNull(I.STRING())
        assertNotNull(I.NUMBER())
        assertNotNull(I.INT())
        assertNotNull(I.FLOAT())
        assertNotNull(I.BOOLEAN())
        assertNotNull(I.BOOL())
        assertNotNull(I.NULL())
        assertNotNull(I.REF())
        assertNotNull(I.REFERENCE())
        assertNotNull(I.OBJECT(mapOf()))
        assertNotNull(I.ARRAY(I.STRING()))
        assertNotNull(I.TABLE("test", mapOf()))
    }

    @Test
    fun testINamespaceUsage() {
        // Example of using I namespace like Zod's z
        val userSchema = I.TABLE("users", mapOf(
            "id" to I.INT(),
            "name" to I.STRING().min(1),
            "email" to I.STRING().email(),
            "active" to I.BOOL().default(true)
        ))

        val err = userSchema.validate(listOf(
            mapOf(
                "id" to 1L,
                "name" to "Alice",
                "email" to "alice@example.com",
                "active" to true
            )
        ))
        assertNull(err)
    }

    // ValidationError Tests

    @Test
    fun testValidationErrorString() {
        val err = ValidationError(
            field = "email",
            message = "invalid email format",
            value = "not-an-email"
        )

        assertEquals("email: invalid email format", err.error())
    }

    @Test
    fun testValidationErrorsString() {
        val errs = ValidationErrors()
        errs.errors.addAll(listOf(
            ValidationError(field = "email", message = "invalid email", value = null),
            ValidationError(field = "name", message = "required", value = null)
        ))

        assert(errs.error().contains("email: invalid email"))
        assert(errs.error().contains("name: required"))
    }

    @Test
    fun testValidationErrorsHasErrors() {
        val empty = ValidationErrors()
        assertFalse(empty.hasErrors())

        val withErrors = ValidationErrors()
        withErrors.errors.add(ValidationError(field = "test", message = "error", value = null))
        assertTrue(withErrors.hasErrors())
    }
}