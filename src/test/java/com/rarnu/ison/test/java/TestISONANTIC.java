package com.rarnu.ison.test.java;

import com.rarnu.ison.*;
import kotlin.text.Regex;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class TestISONANTIC {

    @Test
    public void testStringRequired() {
        var schema = new StringSchema();

        var err = schema.validate("hello");
        assertNull(err);

        err = schema.validate(null);
        assertNotNull(err);
        assert (err.getMessage().contains("required"));
    }

    @Test
    public void testStringOptional() {
        var schema = new StringSchema().optional();

        var err = schema.validate(null);
        assertNull(err);

        err = schema.validate("hello");
        assertNull(err);
    }

    @Test
    public void testStringMinLength() {
        var schema = new StringSchema().min(5);

        var err = schema.validate("hello");
        assertNull(err);

        err = schema.validate("hi");
        assertNotNull(err);
        assert (err.getMessage().contains("at least 5"));
    }

    @Test
    public void testStringMaxLength() {
        var schema = new StringSchema().max(5);

        var err = schema.validate("hello");
        assertNull(err);

        err = schema.validate("hello world");
        assertNotNull(err);
        assert (err.getMessage().contains("at most 5"));
    }

    @Test
    public void testStringExactLength() {
        var schema = new StringSchema().length(5);

        var err = schema.validate("hello");
        assertNull(err);

        err = schema.validate("hi");
        assertNotNull(err);
        assert (err.getMessage().contains("exactly 5"));
    }

    @Test
    public void testStringEmail() {
        var schema = new StringSchema().email();

        var err = schema.validate("test@example.com");
        assertNull(err);

        err = schema.validate("invalid-email");
        assertNotNull(err);
        assert (err.getMessage().contains("invalid email"));
    }

    @Test
    public void testStringURL() {
        var schema = new StringSchema().url();

        var err = schema.validate("https://example.com");
        assertNull(err);

        err = schema.validate("http://example.com/path");
        assertNull(err);

        err = schema.validate("not-a-url");
        assertNotNull(err);
        assert (err.getMessage().contains("invalid URL"));
    }

    @Test
    public void testStringRegex() {
        var pattern = new Regex("^[A-Z]{2,3}$");
        var schema = new StringSchema().regex(pattern);

        var err = schema.validate("AB");
        assertNull(err);

        err = schema.validate("ABC");
        assertNull(err);

        err = schema.validate("A");
        assertNotNull(err);

        err = schema.validate("ABCD");
        assertNotNull(err);
    }

    @Test
    public void testStringDescribe() {
        var schema = new StringSchema().describe("User's name");

        assertEquals("User's name", schema.getDescription());
    }

    @Test
    public void testStringRefine() {
        var schema = new StringSchema().refine(s -> Character.isUpperCase(s.charAt(0)), "must start with uppercase");

        var err = schema.validate("Hello");
        assertNull(err);

        err = schema.validate("hello");
        assertNotNull(err);
        assert (err.getMessage().contains("must start with uppercase"));
    }

    // Number Schema Tests

    @Test
    public void testNumberRequired() {
        var schema = new NumberSchema();

        var err = schema.validate(42.5);
        assertNull(err);

        err = schema.validate(null);
        assertNotNull(err);
    }

    @Test
    public void testNumberOptional() {
        var schema = new NumberSchema().optional();

        var err = schema.validate(null);
        assertNull(err);
    }

    @Test
    public void testIntSchema() {
        var schema = NumberSchema.INT();

        var err = schema.validate(42L);
        assertNull(err);

        err = schema.validate(42.5);
        assertNotNull(err);
        assert (err.getMessage().contains("expected integer"));
    }

    @Test
    public void testNumberMin() {
        var schema = new NumberSchema().min(10.0);

        var err = schema.validate(10.0);
        assertNull(err);

        err = schema.validate(5.0);
        assertNotNull(err);
        assert (err.getMessage().contains("at least"));
    }

    @Test
    public void testNumberMax() {
        var schema = new NumberSchema().max(10.0);

        var err = schema.validate(10.0);
        assertNull(err);

        err = schema.validate(15.0);
        assertNotNull(err);
        assert (err.getMessage().contains("at most"));
    }

    @Test
    public void testNumberPositive() {
        var schema = new NumberSchema().positive();

        var err = schema.validate(5.0);
        assertNull(err);

        err = schema.validate(0.0);
        assertNotNull(err);

        err = schema.validate(-5.0);
        assertNotNull(err);
        assert (err.getMessage().contains("positive"));
    }

    @Test
    public void testNumberNegative() {
        var schema = new NumberSchema().negative();

        var err = schema.validate(-5.0);
        assertNull(err);

        err = schema.validate(0.0);
        assertNotNull(err);

        err = schema.validate(5.0);
        assertNotNull(err);
        assert (err.getMessage().contains("negative"));
    }

    @Test
    public void testNumberRefine() {
        var schema = new NumberSchema().refine(n -> n.intValue() % 2 == 0, "must be even");

        var err = schema.validate(4.0);
        assertNull(err);

        err = schema.validate(3.0);
        assertNotNull(err);
        assert (err.getMessage().contains("must be even"));
    }

    // Boolean Schema Tests

    @Test
    public void testBooleanRequired() {
        var schema = new BooleanSchema();

        var err = schema.validate(true);
        assertNull(err);

        err = schema.validate(false);
        assertNull(err);

        err = schema.validate(null);
        assertNotNull(err);
    }

    @Test
    public void testBooleanOptional() {
        var schema = new BooleanSchema().optional();

        var err = schema.validate(null);
        assertNull(err);
    }

    @Test
    public void testNullSchema() {
        var schema = new NullSchema();

        var err = schema.validate(null);
        assertNull(err);

        err = schema.validate("not null");
        assertNotNull(err);
        assert (err.getMessage().contains("expected null"));
    }

    @Test
    public void testRefRequired() {
        var schema = new RefSchema();

        var err = schema.validate(":1");
        assertNull(err);

        err = schema.validate(Map.of("_ref", "1"));
        assertNull(err);

        err = schema.validate(null);
        assertNotNull(err);
    }

    @Test
    public void testRefOptional() {
        var schema = new RefSchema().optional();

        var err = schema.validate(null);
        assertNull(err);
    }

    @Test
    public void testRefNamespace() {
        var schema = new RefSchema().namespace("user");

        var err = schema.validate(Map.of(
                "_ref", "1",
                "_namespace", "user"
        ));
        assertNull(err);

        err = schema.validate(Map.of(
                "_ref", "1",
                "_namespace", "other"
        ));
        assertNotNull(err);
    }

    @Test
    public void testRefRelationship() {
        var schema = new RefSchema().relationship("OWNS");

        var err = schema.validate(Map.of(
                "_ref", "1",
                "_relationship", "OWNS"
        ));
        assertNull(err);

        err = schema.validate(Map.of(
                "_ref", "1",
                "_relationship", "OTHER"
        ));
        assertNotNull(err);
    }

    @Test
    public void testRefStringFormat() {
        var schema = new RefSchema();

        var err = schema.validate(":1");
        assertNull(err);

        err = schema.validate(":user:42");
        assertNull(err);

        err = schema.validate("not-a-ref");
        assertNotNull(err);
    }

    @Test
    public void testObjectRequired() {
        var schema = ObjectSchema.OBJECT(new HashMap<>(Map.of(
                "name", new StringSchema(),
                "age", NumberSchema.INT()
        )));

        var err = schema.validate(Map.of(
                "name", "Alice",
                "age", 30L
        ));
        assertNull(err);

        err = schema.validate(null);
        assertNotNull(err);
    }

    @Test
    public void testObjectFieldValidation() {
        var schema = ObjectSchema.OBJECT(new HashMap<>(Map.of(
                "name", new StringSchema().min(1),
                "email", new StringSchema().email()
        )));

        var err = schema.validate(Map.of(
                "name", "Alice",
                "email", "alice@example.com"
        ));
        assertNull(err);

        err = schema.validate(Map.of(
                "name", "",
                "email", "invalid"
        ));
        assertNotNull(err);
        var verrs = (ValidationErrors) err;
        assertNotNull(verrs);
        assertEquals(2, verrs.getErrors().size());
    }

    @Test
    public void testObjectOptionalField() {
        var schema = ObjectSchema.OBJECT(new HashMap<>(Map.of(
                "name", new StringSchema(),
                "email", new StringSchema().optional()
        )));

        var err = schema.validate(Map.of(
                "name", "Alice"
        ));
        assertNull(err);
    }

    @Test
    public void testObjectExtend() {
        var baseSchema = ObjectSchema.OBJECT(new HashMap<>(Map.of(
                "id", NumberSchema.INT(),
                "name", new StringSchema()
        )));

        var extendedSchema = baseSchema.extend(Map.of(
                "email", new StringSchema().email()
        ));

        var err = extendedSchema.validate(Map.of(
                "id", 1L,
                "name", "Alice",
                "email", "alice@example.com"
        ));
        assertNull(err);
    }

    @Test
    public void testObjectPick() {
        var schema = ObjectSchema.OBJECT(new HashMap<>(Map.of(
                "id", NumberSchema.INT(),
                "name", new StringSchema(),
                "email", new StringSchema()
        )));

        var pickedSchema = schema.pick("id", "name");

        var err = pickedSchema.validate(Map.of(
                "id", 1L,
                "name", "Alice"
        ));
        assertNull(err);
    }

    @Test
    public void testObjectOmit() {
        var schema = ObjectSchema.OBJECT(new HashMap<>(Map.of(
                "id", NumberSchema.INT(),
                "name", new StringSchema(),
                "email", new StringSchema()
        )));

        var omittedSchema = schema.omit("email");

        var err = omittedSchema.validate(Map.of(
                "id", 1L,
                "name", "Alice"
        ));
        assertNull(err);
    }

    // Array Schema Tests

    @Test
    public void testArrayRequired() {
        var schema = new ArraySchema(new StringSchema());

        var err = schema.validate(List.of("a", "b", "c"));
        assertNull(err);

        err = schema.validate(null);
        assertNotNull(err);
    }

    @Test
    public void testArrayOptional() {
        var schema = new ArraySchema(new StringSchema()).optional();

        var err = schema.validate(null);
        assertNull(err);
    }

    @Test
    public void testArrayMinLength() {
        var schema = new ArraySchema(new StringSchema()).min(2);

        var err = schema.validate(List.of("a", "b"));
        assertNull(err);

        err = schema.validate(List.of("a"));
        assertNotNull(err);
        assert (err.getMessage().contains("at least 2"));
    }

    @Test
    public void testArrayMaxLength() {
        var schema = new ArraySchema(new StringSchema()).max(2);

        var err = schema.validate(List.of("a", "b"));
        assertNull(err);

        err = schema.validate(List.of("a", "b", "c"));
        assertNotNull(err);
        assert (err.getMessage().contains("at most 2"));
    }

    @Test
    public void testArrayItemValidation() {
        var schema = new ArraySchema(NumberSchema.INT());

        var err = schema.validate(List.of(1L, 2L, 3L));
        assertNull(err);

        err = schema.validate(List.of(1L, "not an int", 3L));
        assertNotNull(err);
        var verrs = (ValidationErrors) err;
        assertNotNull(verrs);
        assertEquals(1, verrs.getErrors().size());
        assertEquals("[1]", verrs.getErrors().get(0).getField());
    }

    @Test
    public void testTableRequired() {
        var schema = new TableSchema("users", new HashMap<>(Map.of(
                "id", NumberSchema.INT(),
                "name", new StringSchema()
        )));

        var err = schema.validate(List.of(
                Map.of("id", 1L, "name", "Alice"),
                Map.of("id", 2L, "name", "Bob")
        ));
        assertNull(err);

        err = schema.validate(null);
        assertNotNull(err);
    }

    @Test
    public void testTableOptional() {
        var schema = new TableSchema("users", new HashMap<>(Map.of(
                "id", NumberSchema.INT(),
                "name", new StringSchema()
        ))).optional();

        var err = schema.validate(null);
        assertNull(err);
    }

    @Test
    public void testTableRowValidation() {
        var schema = new TableSchema("users", new HashMap<>(Map.of(
                "id", NumberSchema.INT(),
                "email", new StringSchema().email()
        )));

        var err = schema.validate(List.of(
                Map.of("id", 1L, "email", "alice@example.com"),
                Map.of("id", 2L, "email", "invalid")
        ));
        assertNotNull(err);
        var verrs = (ValidationErrors) err;
        assertNotNull(verrs);
        assertEquals(1, verrs.getErrors().size());
        assert (verrs.getErrors().get(0).getField().contains("row[1]"));
    }

    @Test
    public void testTableBlockFormat() {
        var schema = new TableSchema("users", new HashMap<>(Map.of(
                "id", NumberSchema.INT(),
                "name", new StringSchema()
        )));

        var err = schema.validate(Map.of(
                "kind", "table",
                "name", "users",
                "rows", List.of(
                        Map.of("id", 1L, "name", "Alice")
                )
        ));
        assertNull(err);
    }

    @Test
    public void testTableGetName() {
        var schema = new TableSchema("users", new HashMap<>());
        assertEquals("users", schema.getName());
    }

    @Test
    public void testDocumentParse() {
        var schema = new DocumentSchema(new HashMap<>(Map.of(
                "users", new TableSchema("users", new HashMap<>(Map.of(
                        "id", NumberSchema.INT(),
                        "name", new StringSchema()
                ))),
                "config", ObjectSchema.OBJECT(new HashMap<>(Map.of(
                        "debug", new BooleanSchema()
                )))
        )));

        var doc = Map.of(
                "users", List.of(
                        Map.of("id", 1L, "name", "Alice")
                ),
                "config", Map.of(
                        "debug", true
                )
        );

        var ret = schema.parse(doc);
        assertNotNull(ret.getFirst());
        assertNull(ret.getSecond());
    }

    @Test
    public void testDocumentParseErrors() {
        var schema = new DocumentSchema(new HashMap<>(Map.of(
                "users", new TableSchema("users", new HashMap<>(Map.of(
                        "id", NumberSchema.INT(),
                        "email", new StringSchema().email()
                )))
        )));

        var doc = Map.of(
                "users", List.of(
                        Map.of("id", 1L, "email", "invalid")
                )
        );

        var ret = schema.parse(doc);
        var err = ret.getSecond();
        assertNotNull(err);
        var verrs = (ValidationErrors) err;
        assertNotNull(verrs);
        assertEquals(1, verrs.getErrors().size());
    }

    @Test
    public void testDocumentSafeParse() {
        var schema = new DocumentSchema(new HashMap<>(Map.of(
                "users", new TableSchema("users", new HashMap<>(Map.of(
                        "id", NumberSchema.INT(),
                        "name", new StringSchema()
                )))
        )));

        // Valid document
        var doc = Map.of(
                "users", List.of(
                        Map.of("id", 1L, "name", "Alice")
                )
        );

        var result = schema.safeParse(doc);
        assertTrue(result.getSuccess());
        assertNotNull(result.getData());
        assertNull(result.getError());

        // Invalid document
        var invalidDoc = Map.of(
                "users", List.of(
                        Map.of("id", "not-an-int", "name", "Alice")
                )
        );

        result = schema.safeParse(invalidDoc);
        assertFalse(result.getSuccess());
        assertNull(result.getData());
        assertNotNull(result.getError());
    }

    // I Namespace Tests

    @Test
    public void testINamespace() {
        assertNotNull(I.STRING());
        assertNotNull(I.NUMBER());
        assertNotNull(I.INT());
        assertNotNull(I.FLOAT());
        assertNotNull(I.BOOLEAN());
        assertNotNull(I.BOOL());
        assertNotNull(I.NULL());
        assertNotNull(I.REF());
        assertNotNull(I.REFERENCE());
        assertNotNull(I.OBJECT(new HashMap<>()));
        assertNotNull(I.ARRAY(I.STRING()));
        assertNotNull(I.TABLE("test", new HashMap<>()));
    }

    @Test
    public void testINamespaceUsage() {
        // Example of using I namespace like Zod's z
        var userSchema = I.TABLE("users", Map.of(
                "id", I.INT(),
                "name", I.STRING().min(1),
                "email", I.STRING().email(),
                "active", I.BOOL()
        ));

        var err = userSchema.validate(List.of(
                Map.of(
                        "id", 1L,
                        "name", "Alice",
                        "email", "alice@example.com",
                        "active", true
                )
        ));
        assertNull(err);
    }

    // ValidationError Tests

    @Test
    public void testValidationErrorString() {
        var err = new ValidationError(
                "email",
                "invalid email format",
                "not-an-email"
        );

        assert(err.error().contains("email: invalid email format"));
    }

    @Test
    public void testValidationErrorsString() {
        var errs = new ValidationErrors();
        errs.getErrors().addAll(List.of(
                new ValidationError("email", "invalid email", null),
                new ValidationError("name", "required", null)
        ));

        assert (errs.error().contains("email: invalid email"));
        assert (errs.error().contains("name: required"));
    }

    @Test
    public void testValidationErrorsHasErrors() {
        var empty = new ValidationErrors();
        assertFalse(empty.hasErrors());

        var withErrors = new ValidationErrors();
        withErrors.getErrors().add(new ValidationError("test", "error", null));
        assertTrue(withErrors.hasErrors());
    }

}
