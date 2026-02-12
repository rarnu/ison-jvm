package com.rarnu.ison.test.java;

import com.rarnu.ison.*;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class TestISON {

    @Test
    public void testBasic() {
        var doc = ISON.parse("""
table.users
id:int name:string active:bool
1 Alice true
2 Bob false
""");

        var users = doc.get("users");
        for (var row: users.getRows()) {
            var name = row.get("name").asString();
            System.out.println(name);
        }
    }

    @Test
    public void testParseSimpleTable() {
        var input = """
table.users
id name email
1 Alice alice@example.com
2 Bob bob@example.com
""";
        var doc = ISON.parse(input);
        var block = doc.get("users");
        assert(block != null);
        assertEquals("table", block.getKind());
        assertEquals("users", block.getName());
        assertEquals(3, block.getFields().size());
        assertEquals(2, block.getRows().size());
        // Check first row
        var row1 = block.getRows().get(0);
        var id = row1.get("id").asInt();
        assert(id != null);
        assertEquals(1L, id.longValue());
        var name = row1.get("name").asString();
        assertEquals("Alice", name);
    }

    @Test
    public void testParseTypedFields() {
        var input = """
table.users
id:int name:string active:bool score:float
1 Alice true 95.5
2 Bob false 82.0
""";
        var doc = ISON.parse(input);
        var block = doc.get("users");
        assert(block != null);

        // Check field type hints
        assertEquals("int", block.getFields().get(0).getTypeHint());
        assertEquals("string", block.getFields().get(1).getTypeHint());
        assertEquals("bool", block.getFields().get(2).getTypeHint());
        assertEquals("float", block.getFields().get(3).getTypeHint());
        // Check values
        var row = block.getRows().get(0);
        var id = row.get("id").asInt();
        assert(id != null);
        assertEquals(1L, id.longValue());
        var active = row.get("active").asBool();
        assertEquals(true, active);
        var score = row.get("score").asFloat();
        assert(score != null);
        assertEquals(95.5, score, 0.00001);
    }

    @Test
    public void testParseQuotedStrings() {
        var input = """
table.users
id name email
1 "Alice Smith" alice@example.com
2 "Bob \\"The Builder\\" Jones" bob@example.com
""";
        var doc = ISON.parse(input);
        var block = doc.get("users");
        assert (block != null);
        var name1 = block.getRows().get(0).get("name").asString();
        assertEquals("Alice Smith", name1);
        var name2 = block.getRows().get(1).get("name").asString();
        assertEquals("Bob \"The Builder\" Jones", name2);
    }


    @Test
    public void testParseNullValues() {
        var input = """
table.users
id name email
1 Alice ~
2 ~ null
3 Charlie NULL
""";
        var doc = ISON.parse(input);
        var block = doc.get("users");
        assert(block != null);
        assertTrue(block.getRows().get(0).get("email").isNull());
        assertTrue(block.getRows().get(1).get("name").isNull());
        assertTrue(block.getRows().get(1).get("email").isNull());
        assertTrue(block.getRows().get(2).get("email").isNull());
    }

    @Test
    public void testParseReferences() {
        var input = """
table.orders
id user_id product
1 :1 Widget
2 :user:42 Gadget
3 :OWNS:5 Gizmo
""";
        var doc = ISON.parse(input);
        var block = doc.get("orders");
        assert(block != null);
        // Simple reference
        var ref1 = block.getRows().get(0).get("user_id").asRef();
        assert (ref1 != null);
        assertEquals("1", ref1.getId());
        assertEquals("", ref1.getNamespace());
        assertEquals("", ref1.getRelationship());
        assertEquals(":1", ref1.toIson());

        // Namespaced reference
        var ref2 = block.getRows().get(1).get("user_id").asRef();
        assert (ref2 != null);
        assertEquals("42", ref2.getId());
        assertEquals("user", ref2.getNamespace());
        assertEquals("", ref2.getRelationship());
        assertEquals(":user:42", ref2.toIson());

        // Relationship reference
        var ref3 = block.getRows().get(2).get("user_id").asRef();
        assert (ref3 != null);
        assertEquals("5", ref3.getId());
        assertEquals("OWNS", ref3.getRelationship());
        assertTrue(ref3.isRelationship());
        assertEquals(":OWNS:5", ref3.toIson());
    }

    @Test
    public void testParseObjectBlock() {
        var input = """
object.config
key value
debug true
timeout 30
""";
        var doc = ISON.parse(input);
        var block = doc.get("config");
        assert (block != null);
        assertEquals("object", block.getKind());
        assertEquals(2, block.getRows().size());
    }

    @Test
    public void testParseMultipleBlocks() {
        var input = """
table.users
id name
1 Alice

table.orders
id user_id
O1 :1

object.meta
version 1.0
""";
        var doc = ISON.parse(input);
        assertEquals(3, doc.getBlocks().size());
        assertEquals(List.of("users", "orders", "meta"), doc.getOrder());
        var users = doc.get("users");
        assert(users != null);
        var orders = doc.get("orders");
        assert(orders != null);
        var meta = doc.get("meta");
        assert(meta != null);
    }


    @Test
    public void testParseSummaryRow() {
        var input = """
table.sales
product amount
Widget 100
Gadget 200
---
total 300
""";
        var doc = ISON.parse(input);
        var block = doc.get("sales");
        assert (block != null);
        assertEquals(2, block.getRows().size());
        assert(block.getSummaryRow() != null);
        var total = block.getSummaryRow().get("amount").asInt();
        assert (total != null);
        assertEquals(300L, total.longValue());
    }

    @Test
    public void testParseComments() {
        var input = """
# This is a comment
table.users
# Field definitions
id name
# Row 1
1 Alice
# Row 2
2 Bob
""";
        var doc = ISON.parse(input);
        var block = doc.get("users");
        assert (block != null);
        assertEquals(2, block.getRows().size());
    }

    @Test
    public void testDumps() {
        var doc = new Document();
        var block = new Block("table", "users");

        block.addField("id", "int");
        block.addField("name", "string");
        block.addRow(Map.of("id" , Value.INT(1), "name" , Value.STRING("Alice")));
        block.addRow(Map.of("id" , Value.INT(2), "name" , Value.STRING("Bob")));
        doc.addBlock(block);

        var output = Dump.dumps(doc);
        assert(output.contains("table.users"));
        assert(output.contains("id:int"));
        assert(output.contains("name:string"));
        assert(output.contains("1 Alice"));
        assert(output.contains("2 Bob"));
    }

    @Test
    public void testRoundtrip() {
        var input = """
table.users
id:int name:string active:bool
1 Alice true
2 Bob false
""";
        var doc = ISON.parse(input);
        var output = Dump.dumps(doc);
        var doc2 = ISON.parse(output);
        var block1 = doc.get("users");
        assert (block1 != null);
        var block2 = doc2.get("users");
        assert (block2 != null);
        assertEquals(block1.getRows().size(), block2.getRows().size());
        for (var i = 0; i < block1.getRows().size(); i++) {
            var row = block1.getRows().get(i);
            for (var entry: row.entrySet()) {
                assertEquals(entry.getValue().intf(), block2.getRows().get(i).get(entry.getKey()).intf());
            }
        }
    }

    @Test
    public void testDumpsISONL() {
        var doc = new Document();
        var block = new Block("table", "users");
        block.addField("id", "int");
        block.addField("name", "string");
        block.addRow(Map.of("id", Value.INT(1), "name", Value.STRING("Alice")));
        block.addRow(Map.of("id", Value.INT(2), "name", Value.STRING("Bob")));
        doc.addBlock(block);

        var output = Dump.dumpsISONL(doc);
        var lines = output.trim().split("\n");
        assertEquals(2, lines.length);
        assert(lines[0].contains("table.users|id:int name:string|1 Alice"));
        assert(lines[1].contains("table.users|id:int name:string|2 Bob"));
    }

    @Test
    public void testParseISONL() {
        var input = """
table.users|id:int name:string|1 Alice
table.users|id:int name:string|2 Bob
table.orders|id product|O1 Widget
""";
        var doc = ISON.parseISONL(input);
        var users = doc.get("users");
        assert (users != null);
        assertEquals(2, users.getRows().size());
        var orders = doc.get("orders");
        assert (orders != null);
        assertEquals(1, orders.getRows().size());
    }

    @Test
    public void testFromJSON() {
        var jsonStr = """
{
    "users": [
		{"id": 1, "name": "Alice", "active": true},
		{"id": 2, "name": "Bob", "active": false}
	]
}
""";
        var doc = ISON.fromJson(jsonStr);
        var block = doc.get("users");
        assert(block != null);
        assertEquals(2, block.getRows().size());
        var id = block.getRows().get(0).get("id").asInt();
        assert (id != null);
        assertEquals(1L, id.longValue());
    }

    @Test
    public void testValueTypes() {
        // Test Null
        var nullVal = Value.NULL();
        assertEquals(ValueType.TypeNull, nullVal.getType());
        assertTrue(nullVal.isNull());
        assert(nullVal.intf() == null);

        // Test Bool
        var boolVal = Value.BOOL(true);
        assertEquals(ValueType.TypeBool, boolVal.getType());
        var b = boolVal.asBool();
        assertEquals(true, b);

        // Test Int
        var intVal = Value.INT(42);
        assertEquals(ValueType.TypeInt, intVal.getType());
        var i = intVal.asInt();
        assert (i != null);
        assertEquals(42L, i.longValue());

        // Test Float
        var floatVal = Value.FLOAT(3.14);
        assertEquals(ValueType.TypeFloat, floatVal.getType());
        var f = floatVal.asFloat();
        assert (f != null);
        assertEquals(3.14, f, 0.00001);

        // Int can be converted to float
        var f2 = intVal.asFloat();
        assert (f2 != null);
        assertEquals(42.0, f2, 0.00001);

        // Test String
        var strVal = Value.STRING("hello");
        assertEquals(ValueType.TypeString, strVal.getType());
        var s = strVal.asString();
        assertEquals("hello", s);

        // Test Reference
        var refVal = Value.REF(new Reference("1", "user"));
        assertEquals(ValueType.TypeReference, refVal.getType());
        var r = refVal.asRef();
        assert (r != null);
        assertEquals("1", r.getId());
        assertEquals("user", r.getNamespace());
    }


    @Test
    public void testValueToISON() {
        assertEquals("~", Value.NULL().toIson());
        assertEquals("true", Value.BOOL(true).toIson());
        assertEquals("false", Value.BOOL(false).toIson());
        assertEquals("42", Value.INT(42).toIson());
        assertEquals("3.14", Value.FLOAT(3.14).toIson());
        assertEquals("hello", Value.STRING("hello").toIson());
        assertEquals("\"hello world\"", Value.STRING("hello world").toIson());
        assertEquals("\"with \\\"quotes\\\"\"", Value.STRING("with \"quotes\"").toIson());
        assertEquals(":1", Value.REF(new Reference("1")).toIson());
        assertEquals(":user:1", Value.REF(new Reference("1", "user")).toIson());
        assertEquals(":OWNS:1", Value.REF(new Reference("1","","OWNS")).toIson());
    }

    @Test
    public void testReferenceGetNamespace() {
        var ref1 = new Reference("1");
        assertEquals("", ref1.getNsOrRel());

        var ref2 = new Reference("1",  "user");
        assertEquals("user", ref2.getNsOrRel());

        var ref3 = new Reference("1", "", "OWNS");
        assertEquals("OWNS", ref3.getNsOrRel());
    }

    @Test
    public void testBlockToDict() {
        var block = new Block("table", "users");
        block.addField("id", "int");
        block.addField("name", "string");
        block.addRow(Map.of("id", Value.INT(1), "name", Value.STRING("Alice")));

        var dict = block.toDict();
        assertEquals("table", dict.get("kind"));
        assertEquals("users", dict.get("name"));

        var fields = (List<Map<String, ?>>) dict.get("fields");
        assert(fields != null);
        assertEquals(2, fields.size());
        assertEquals("id", fields.get(0).get("name"));
        assertEquals("int", fields.get(0).get("typeHint"));

        var rows = (List<Map<String, ?>>) dict.get("rows");
        assert(rows != null);
        assertEquals(1, rows.size());
        assertEquals(1L, rows.get(0).get("id"));
    }

    @Test
    public void testDocumentToDict() {
        var doc = new Document();
        var block = new Block("table", "users");
        block.addField("id", "int");
        block.addRow(Map.of("id", Value.INT(1)));
        doc.addBlock(block);

        var dict = doc.toDict();
        var users = (Map<String, ?>) dict.get("users");
        assert(users != null);
        assertEquals("table", users.get("kind"));
    }

    @Test
    public void testEscapeSequences() {
        var input = """
table.data
id text
1 "line1\\nline2"
2 "tab\there"
""";
        var doc = ISON.parse(input);
        var block = doc.get("data");
        assert (block != null);
        var text1 = block.getRows().get(0).get("text").asString();
        assertEquals("line1\nline2", text1);
        var text2 = block.getRows().get(1).get("text").asString();
        assertEquals("tab\there", text2);
    }

    @Test
    public void testEmptyDocument() {
        var doc = ISON.parse("");
        assertEquals(0, doc.getBlocks().size());
    }

    @Test
    public void testOnlyComments() {
        var input = """
# Comment 1
# Comment 2
""";
        var doc = ISON.parse(input);
        assertEquals(0, doc.getBlocks().size());
    }

    @Test
    public void testTypeInference() {
        var input = """
table.data
a b c d e
42 3.14 true false hello
""";
        var doc = ISON.parse(input);
        var block = doc.get("data");
        assert (block != null);
        var row = block.getRows().get(0);

        // Integer
        assertEquals(ValueType.TypeInt, row.get("a").getType());

        // Float
        assertEquals(ValueType.TypeFloat, row.get("b").getType());

        // Booleans
        assertEquals(ValueType.TypeBool, row.get("c").getType());
        assertEquals(ValueType.TypeBool, row.get("d").getType());

        // String
        assertEquals(ValueType.TypeString, row.get("e").getType());
    }

    @Test
    public void testValueMarshalJSON() {
        // Test various value types
        var tests = Map.of(
            Value.NULL(), "null",
            Value.BOOL(true), "true",
            Value.BOOL(false), "false",
            Value.INT(42), "42",
            Value.FLOAT(3.14), "3.14",
            Value.STRING("hello"), "\"hello\""
        );
        for (var entry: tests.entrySet()) {
            var data = entry.getKey().json();
            assertEquals(entry.getValue(), data);
        }
    }

    @Test
    public void testGetFieldNames() {
        var block = new Block("table", "test");
        block.addField("id", "int");
        block.addField("name", "string");
        block.addField("active", "bool");

        var names = block.getFieldNames();
        assertEquals(List.of("id", "name", "active"), names);
    }

    @Test
    public void testLoadDump() throws IOException {
        var tmpfile = File.createTempFile("test_", ".ison");

        var doc = new Document();
        var block = new Block("table", "users");
        block.addField("id", "int");
        block.addField("name", "string");
        block.addRow(Map.of("id", Value.INT(1), "name", Value.STRING("Alice")));
        doc.addBlock(block);
        Dump.dump(doc, tmpfile);

        var loaded = ISON.load(tmpfile);
        var users = loaded.get("users");
        assert (users != null);
        assertEquals(1, users.getRows().size());
        var name = users.getRows().get(0).get("name").asString();
        assertEquals("Alice", name);
    }

    @Test
    public void testLoadDumpISONL() throws IOException {
        var tmpfile = File.createTempFile("test_", ".isonl");
        var doc = new Document();
        var block = new Block("table", "users");
        block.addField("id", "int");
        block.addField("name", "string");
        block.addRow(Map.of("id",Value.INT(1), "name", Value.STRING("Alice")));
        block.addRow(Map.of("id", Value.INT(2), "name", Value.STRING("Bob")));
        doc.addBlock(block);
        Dump.dumpISONL(doc, tmpfile);

        var loaded = ISON.loadISONL(tmpfile);
        var users = loaded.get("users");
        assert (users != null);
        assertEquals(2, users.getRows().size());
    }

    @Test
    public void testISONToISONL() {
        var isonText = "table.users\nid:int name:string\n1 Alice\n2 Bob";
        var isonlText = ISON.ISONToISONL(isonText);

        var lines = isonlText.trim().split("\n");
        assertEquals(2, lines.length);
        assert(lines[0].contains("table.users|"));
        assert(lines[0].contains("1 Alice"));
        assert(lines[1].contains("2 Bob"));
    }

    @Test
    public void testISONLToISON() {
        var isonlText = "table.users|id:int name:string|1 Alice\ntable.users|id:int name:string|2 Bob";
        var isonText = ISON.ISONLToISON(isonlText);

        assert(isonText.contains("table.users"));
        assert(isonText.contains("id:int name:string"));
        assert(isonText.contains("1 Alice"));
        assert(isonText.contains("2 Bob"));
    }

    @Test
    public void testDumpsWithOptions() {

        var doc = new Document();
        var block = new Block("table", "users");
        block.addField("id", "int");
        block.addField("name", "string");
        block.addRow(Map.of("id",Value.INT(1), "name", Value.STRING("Alice")));
        block.addRow(Map.of("id", Value.INT(2), "name", Value.STRING("Bob")));
        doc.addBlock(block);

        var opts = new DumpsOptions(false, "\t");
        var output = Dump.dumpsWithOptions(doc, opts);
        assert(output.contains("id:int\tname:string"));
        assert(output.contains("1\tAlice"));
    }

    @Test
    public void testFromDict() {
        var data = Map.of(
            "users", List.of(
                Map.of("id", 1, "name","Alice", "active", true),
                Map.of("id", 2, "name", "Bob", "active", false)
            )
        );

        var doc = ISON.fromDict(data);
        var users = doc.get("users");
        assert (users != null);
        assertEquals("table", users.getKind());
        assertEquals(2, users.getRows().size());
    }

    @Test
    public void testFromDictWithAutoRefs() {
        var data = Map.of(
            "orders", List.of(
                Map.of("id", 1, "customer_id", 42, "product", "Widget")
            ),
            "customers", List.of(
                Map.of("id", 42, "name", "Alice")
            )
        );

        var opts = new FromDictOptions(true, true);
        var doc = ISON.fromDictWithOptions(data, opts);

        var orders = doc.get("orders");
        assert (orders != null);
        var custId = orders.getRows().get(0).get("customer_id");
        var ref = custId.asRef();
        assert (ref != null);
        assertEquals("42", ref.getId());
    }

    @Test
    public void testSmartOrderFields() {
        var fields = List.of("email", "customer_id", "name", "id", "status");
        var ordered = ISON.INSTANCE.smartOrderFields(fields);

        assertEquals("id", ordered.get(0));
        assertEquals("name", ordered.get(1));
        assertEquals("customer_id", ordered.get(ordered.size() - 1));
    }

    @Test
    public void testDefaultDumpsOptions() {
        var opts = Dump.defaultDumpsOptions();
        assertFalse(opts.getAlignColumns());
        assertEquals(" ", opts.getDelimiter());
    }

    @Test
    public void testDefaultFromDictOptions() {
        var opts = ISON.defaultFromDictOptions();
        assertFalse(opts.getAutoRefs());
        assertFalse(opts.getSmartOrder());
    }

}

