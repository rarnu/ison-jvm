@file:Suppress("UNCHECKED_CAST")

package com.rarnu.ison.test

import com.isyscore.kotlin.common.toObj
import com.rarnu.ison.Block
import com.rarnu.ison.Document
import com.rarnu.ison.Dump
import com.rarnu.ison.DumpsOptions
import com.rarnu.ison.FromDictOptions
import com.rarnu.ison.ISON
import com.rarnu.ison.Reference
import com.rarnu.ison.Value
import com.rarnu.ison.ValueType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class TestISON {

    @Test
    fun testBasic() {
        val doc = ISON.parse("""
table.users
id:int name:string active:bool
1 Alice true
2 Bob false
""")

        val users = doc.get("users")
        for (row in users!!.rows) {
            val name = row["name"]?.asString()
            println(name)
        }
    }

    @Test
    fun testParseSimpleTable() {
        val input = """
table.users
id name email
1 Alice alice@example.com
2 Bob bob@example.com
"""
        val doc = ISON.parse(input)
        val block = doc.get("users")
        assert(block != null)
        block!!
        assertEquals("table", block.kind)
        assertEquals("users", block.name)
        assertEquals(3, block.fields.size)
        assertEquals(2, block.rows.size)
        // Check first row
        val row1 = block.rows[0]
        val id = row1["id"]?.asInt()
        assertEquals(1L, id)
        val name = row1["name"]?.asString()
        assertEquals("Alice", name)
    }

    @Test
    fun testParseTypedFields() {
        val input = """
table.users
id:int name:string active:bool score:float
1 Alice true 95.5
2 Bob false 82.0
"""
        val doc = ISON.parse(input)
        val block = doc.get("users")
        assert(block != null)
        block!!
        // Check field type hints
        assertEquals("int", block.fields[0].typeHint)
        assertEquals("string", block.fields[1].typeHint)
        assertEquals("bool", block.fields[2].typeHint)
        assertEquals("float", block.fields[3].typeHint)
        // Check values
        val row = block.rows[0]
        val id = row["id"]?.asInt()
        assertEquals(1L, id)
        val active = row["active"]?.asBool()
        assertEquals(true, active)
        val score = row["score"]?.asFloat()
        assertEquals(95.5, score)

    }

    @Test
    fun testParseQuotedStrings() {
        val input = """
table.users
id name email
1 "Alice Smith" alice@example.com
2 "Bob \"The Builder\" Jones" bob@example.com            
"""
        val doc = ISON.parse(input)
        val block = doc.get("users")!!
        val name1 = block.rows[0]["name"]?.asString()
        assertEquals("Alice Smith", name1)
        val name2 = block.rows[1]["name"]?.asString()
        assertEquals("""Bob "The Builder" Jones""", name2)
    }

    @Test
    fun testParseNullValues() {
        val input = """
table.users
id name email
1 Alice ~
2 ~ null
3 Charlie NULL
"""
        val doc = ISON.parse(input)
        val block = doc.get("users")!!
        assertEquals(true, block.rows[0]["email"]?.isNull())
        assertEquals(true, block.rows[1]["name"]?.isNull())
        assertEquals(true, block.rows[1]["email"]?.isNull())
        assertEquals(true, block.rows[2]["email"]?.isNull())
    }

    @Test
    fun testParseReferences() {
        val input = """
table.orders
id user_id product
1 :1 Widget
2 :user:42 Gadget
3 :OWNS:5 Gizmo
"""
        val doc = ISON.parse(input)
        val block = doc.get("orders")!!

        // Simple reference
        val ref1 = block.rows[0]["user_id"]?.asRef()
        assertEquals("1", ref1?.id)
        assertEquals("", ref1?.namespace)
        assertEquals("", ref1?.relationship)
        assertEquals(":1", ref1?.toIson())

        // Namespaced reference
        val ref2 = block.rows[1]["user_id"]?.asRef()
        assertEquals("42", ref2?.id)
        assertEquals("user", ref2?.namespace)
        assertEquals("", ref2?.relationship)
        assertEquals(":user:42", ref2?.toIson())

        // Relationship reference
        val ref3 = block.rows[2]["user_id"]?.asRef()
        assertEquals("5", ref3?.id)
        assertEquals("OWNS", ref3?.relationship)
        assertEquals(true, ref3?.isRelationship())
        assertEquals(":OWNS:5", ref3?.toIson())
    }

    @Test
    fun testParseObjectBlock() {
        val input = """
object.config
key value
debug true
timeout 30
"""
        val doc = ISON.parse(input)
        val block = doc.get("config")!!
        assertEquals("object", block.kind)
        assertEquals(2, block.rows.size)
    }

    @Test
    fun testParseMultipleBlocks() {
        val input = """
table.users
id name
1 Alice

table.orders
id user_id
O1 :1

object.meta
version 1.0
"""
        val doc = ISON.parse(input)
        assertEquals(3, doc.blocks.size)
        assertEquals(listOf("users", "orders", "meta"), doc.order)
        val users = doc.get("users")
        assert(users != null)
        val orders = doc.get("orders")
        assert(orders != null)
        val meta = doc.get("meta")
        assert(meta != null)
    }

    @Test
    fun testParseSummaryRow() {
        val input = """
table.sales
product amount
Widget 100
Gadget 200
---
total 300
"""
        val doc = ISON.parse(input)
        val block = doc.get("sales")!!
        assertEquals(2, block.rows.size)
        assert(block.summaryRow != null)
        val total = block.summaryRow!!["amount"]?.asInt()
        assertEquals(300L, total)
    }

    @Test
    fun testParseComments() {
        val input = """
# This is a comment
table.users
# Field definitions
id name
# Row 1
1 Alice
# Row 2
2 Bob
"""
        val doc = ISON.parse(input)
        val block = doc.get("users")!!
        assertEquals(2, block.rows.size)
    }

    @Test
    fun testDumps() {
        val doc = Document()
        val block = Block("table", "users")
        block.addField("id", "int")
        block.addField("name", "string")
        block.addRow(mutableMapOf("id" to Value.INT(1), "name" to Value.STRING("Alice")))
        block.addRow(mutableMapOf("id" to Value.INT(2), "name" to Value.STRING("Bob")))
        doc.addBlock(block)

        val output = Dump.dumps(doc)
        assert(output.contains("table.users"))
        assert(output.contains("id:int"))
        assert(output.contains("name:string"))
        assert(output.contains("1 Alice"))
        assert(output.contains("2 Bob"))
    }

    @Test
    fun testRoundtrip() {
        val input = """table.users
id:int name:string active:bool
1 Alice true
2 Bob false 
"""
        val doc = ISON.parse(input)
        val output = Dump.dumps(doc)
        val doc2 = ISON.parse(output)
        val block1 = doc.get("users")!!
        val block2 = doc2.get("users")!!
        assertEquals(block1.rows.size, block2.rows.size)
        block1.rows.forEachIndexed { i, row ->
            row.forEach { (k, v) ->
                assertEquals(v.intf(), block2.rows[i][k]?.intf())
            }
        }
    }

    @Test
    fun testDumpsISONL() {
        val doc = Document()
        val block = Block("table", "users")
        block.addField("id", "int")
        block.addField("name", "string")
        block.addRow(mutableMapOf("id" to Value.INT(1), "name" to Value.STRING("Alice")))
        block.addRow(mutableMapOf("id" to Value.INT(2), "name" to Value.STRING("Bob")))
        doc.addBlock(block)

        val output = Dump.dumpsISONL(doc)
        val lines = output.trim().split("\n")
        assertEquals(2, lines.size)
        assert(lines[0].contains("table.users|id:int name:string|1 Alice"))
        assert(lines[1].contains("table.users|id:int name:string|2 Bob"))
    }

    @Test
    fun testParseISONL() {
        val input = """table.users|id:int name:string|1 Alice
table.users|id:int name:string|2 Bob
table.orders|id product|O1 Widget"""
        val doc = ISON.parseISONL(input)
        val users = doc.get("users")!!
        assertEquals(2, users.rows.size)
        val orders = doc.get("orders")!!
        assertEquals(1, orders.rows.size)
    }

    @Test
    fun testToJSON() {
        val input = """
table.users
id:int name:string active:bool
1 Alice true
2 Bob false
"""
        val jsonStr = ISON.toJson(input)
        val data = jsonStr.toObj<Map<String, Any?>>()
        val users = data["users"] as? List<Map<String, Any?>>
        assertEquals(2, users?.size)
        val user1 = users!![0]
        assertEquals(1, user1["id"])
        assertEquals("Alice", user1["name"])
        assertEquals(true, user1["active"])

    }

    @Test
    fun testFromJSON() {
        val jsonStr = """{
    "users": [
		{"id": 1, "name": "Alice", "active": true},
		{"id": 2, "name": "Bob", "active": false}
	]
}"""
        val doc = ISON.fromJson(jsonStr)
        val block = doc.get("users")
        assert(block != null)
        block!!
        assertEquals(2, block.rows.size)
        val id = block.rows[0]["id"]?.asInt()
        assertEquals(1L, id)
    }

    @Test
    fun testValueTypes() {
        // Test Null
        val nullVal = Value.NULL()
        assertEquals(ValueType.TypeNull, nullVal.type)
        assertEquals(true, nullVal.isNull())
        assert(nullVal.intf() == null)

        // Test Bool
        val boolVal = Value.BOOL(true)
        assertEquals(ValueType.TypeBool, boolVal.type)
        val b = boolVal.asBool()
        assertEquals(true, b)

        // Test Int
        val intVal = Value.INT(42)
        assertEquals(ValueType.TypeInt, intVal.type)
        val i = intVal.asInt()
        assertEquals(42L, i)

        // Test Float
        val floatVal = Value.FLOAT(3.14)
        assertEquals(ValueType.TypeFloat, floatVal.type)
        val f = floatVal.asFloat()
        assertEquals(3.14, f)

        // Int can be converted to float
        val f2 = intVal.asFloat()
        assertEquals(42.0, f2)

        // Test String
        val strVal = Value.STRING("hello")
        assertEquals(ValueType.TypeString, strVal.type)
        val s = strVal.asString()
        assertEquals("hello", s)

        // Test Reference
        val refVal = Value.REF(Reference(id = "1", namespace = "user"))
        assertEquals(ValueType.TypeReference, refVal.type)
        val r = refVal.asRef()
        assertEquals("1", r?.id)
        assertEquals("user", r?.namespace)
    }


    @Test
    fun testValueToISON() {
        assertEquals("~", Value.NULL().toIson())
        assertEquals("true", Value.BOOL(true).toIson())
        assertEquals("false", Value.BOOL(false).toIson())
        assertEquals("42", Value.INT(42).toIson())
        assertEquals("3.14", Value.FLOAT(3.14).toIson())
        assertEquals("hello", Value.STRING("hello").toIson())
        assertEquals(""""hello world"""", Value.STRING("hello world").toIson())
        assertEquals(""""with \"quotes\""""", Value.STRING("""with "quotes"""").toIson())
        assertEquals(":1", Value.REF(Reference(id = "1")).toIson())
        assertEquals(":user:1", Value.REF(Reference(id = "1", namespace = "user")).toIson())
        assertEquals(":OWNS:1", Value.REF(Reference(id = "1", relationship = "OWNS")).toIson())
    }

    @Test
    fun testReferenceGetNamespace() {
        val ref1 = Reference(id = "1")
        assertEquals("", ref1.getNsOrRel())

        val ref2 = Reference(id = "1", namespace = "user")
        assertEquals("user", ref2.getNsOrRel())

        val ref3 = Reference(id = "1", relationship = "OWNS")
        assertEquals("OWNS", ref3.getNsOrRel())
    }

    @Test
    fun testBlockToDict() {
        val block = Block("table", "users")
        block.addField("id", "int")
        block.addField("name", "string")
        block.addRow(mutableMapOf("id" to Value.INT(1), "name" to Value.STRING("Alice")))

        val dict = block.toDict()
        assertEquals("table", dict["kind"])
        assertEquals("users", dict["name"])

        val fields = dict["fields"] as? List<Map<String, Any?>>
        assert(fields != null)
        fields!!
        assertEquals(2, fields.size)
        assertEquals("id", fields[0]["name"])
        assertEquals("int", fields[0]["typeHint"])

        val rows = dict["rows"] as? List<Map<String, Any?>>
        assert(rows != null)
        rows!!
        assertEquals(1, rows.size)
        assertEquals(1L, rows[0]["id"])
    }

    @Test
    fun testDocumentToDict() {
        val doc = Document()
        val block = Block("table", "users")
        block.addField("id", "int")
        block.addRow(mutableMapOf("id" to Value.INT(1)))
        doc.addBlock(block)

        val dict = doc.toDict()
        val users = dict["users"] as? Map<String, Any?>
        assert(users != null)
        users!!
        assertEquals("table", users["kind"])
    }

    @Test
    fun testEscapeSequences() {
        val input = """
table.data
id text
1 "line1\nline2"
2 "tab\there" 
"""
        val doc = ISON.parse(input)
        val block = doc.get("data")!!
        val text1 = block.rows[0]["text"]?.asString()
        assertEquals("line1\nline2", text1)
        val text2 = block.rows[1]["text"]?.asString()
        assertEquals("tab\there", text2)
    }

    @Test
    fun testEmptyDocument() {
        val doc = ISON.parse("")
        assertEquals(0, doc.blocks.size)
    }

    @Test
    fun testOnlyComments() {
        val input = """
# Comment 1
# Comment 2
"""
        val doc = ISON.parse(input)
        assertEquals(0, doc.blocks.size)
    }

    @Test
    fun testTypeInference() {
        val input = """
table.data
a b c d e
42 3.14 true false hello
"""
        val doc = ISON.parse(input)
        val block = doc.get("data")!!
        val row = block.rows[0]

        // Integer
        assertEquals(ValueType.TypeInt, row["a"]?.type)

        // Float
        assertEquals(ValueType.TypeFloat, row["b"]?.type)

        // Booleans
        assertEquals(ValueType.TypeBool, row["c"]?.type)
        assertEquals(ValueType.TypeBool, row["d"]?.type)

        // String
        assertEquals(ValueType.TypeString, row["e"]?.type)
    }

    @Test
    fun testReferenceMarshalJSON() {
        val ref = Reference(id = "42", namespace = "user")
        val data = ref.json()
        val result = data.toObj<Map<String, Any?>>()
        assertEquals("42", result["_ref"])
        assertEquals("user", result["_namespace"])
    }

    @Test
    fun testValueMarshalJSON() {
        // Test various value types
        val tests = mapOf(
            Value.NULL() to "null",
            Value.BOOL(true) to "true",
            Value.BOOL(false) to "false",
            Value.INT(42) to "42",
            Value.FLOAT(3.14) to "3.14",
            Value.STRING("hello") to "\"hello\""
        )
        tests.forEach { (v, ex) ->
            val data = v.json()
            assertEquals(ex, data)
        }
    }

    @Test
    fun testGetFieldNames() {
        val block = Block("table", "test")
        block.addField("id", "int")
        block.addField("name", "string")
        block.addField("active", "bool")

        val names = block.getFieldNames()
        assertEquals(listOf("id", "name", "active"), names)
    }

    @Test
    fun testLoadDump() {
        val tmpfile = File.createTempFile("test_", ".ison")

        val doc = Document()
        val block = Block("table", "users")
        block.addField("id", "int")
        block.addField("name", "string")
        block.addRow(mutableMapOf("id" to Value.INT(1), "name" to Value.STRING("Alice")))
        doc.addBlock(block)
        Dump.dump(doc, tmpfile)

        val loaded = ISON.load(tmpfile)
        val users = loaded.get("users")!!
        assertEquals(1, users.rows.size)
        val name = users.rows[0]["name"]?.asString()
        assertEquals("Alice", name)
    }

    @Test
    fun testLoadDumpISONL() {
        val tmpfile = File.createTempFile("test_", ".isonl")
        val doc = Document()
        val block = Block("table", "users")
        block.addField("id", "int")
        block.addField("name", "string")
        block.addRow(mutableMapOf("id" to Value.INT(1), "name" to Value.STRING("Alice")))
        block.addRow(mutableMapOf("id" to Value.INT(2), "name" to Value.STRING("Bob")))
        doc.addBlock(block)
        Dump.dumpISONL(doc, tmpfile)

        val loaded = ISON.loadISONL(tmpfile)
        val users = loaded.get("users")!!
        assertEquals(2, users.rows.size)
    }

    @Test
    fun testISONToISONL() {
        val isonText = "table.users\nid:int name:string\n1 Alice\n2 Bob"
        val isonlText = ISON.ISONToISONL(isonText)

        val lines = isonlText.trim().split("\n")
        assertEquals(2, lines.size)
        assert(lines[0].contains("table.users|"))
        assert(lines[0].contains("1 Alice"))
        assert(lines[1].contains("2 Bob"))
    }

    @Test
    fun testISONLToISON() {
        val isonlText = "table.users|id:int name:string|1 Alice\ntable.users|id:int name:string|2 Bob"
        val isonText = ISON.ISONLToISON(isonlText)

        assert(isonText.contains("table.users"))
        assert(isonText.contains("id:int name:string"))
        assert(isonText.contains("1 Alice"))
        assert(isonText.contains("2 Bob"))
    }

    @Test
    fun testDumpsWithOptions() {

        val doc = Document()
        val block = Block("table", "users")
        block.addField("id", "int")
        block.addField("name", "string")
        block.addRow(mutableMapOf("id" to Value.INT(1), "name" to Value.STRING("Alice")))
        block.addRow(mutableMapOf("id" to Value.INT(2), "name" to Value.STRING("Bob")))
        doc.addBlock(block)

        val opts = DumpsOptions(false, "\t")
        val output = Dump.dumpsWithOptions(doc, opts)
        assert(output.contains("id:int\tname:string"))
        assert(output.contains("1\tAlice"))
    }

    @Test
    fun testFromDict() {
        val data = mapOf(
            "users" to listOf(
                mapOf("id" to 1, "name" to "Alice", "active" to true),
                mapOf("id" to 2, "name" to "Bob", "active" to false)
            )
        )

        val doc = ISON.fromDict(data)
        val users = doc.get("users")!!
        assertEquals("table", users.kind)
        assertEquals(2, users.rows.size)
    }

    @Test
    fun testFromDictWithAutoRefs() {
        val data = mapOf(
            "orders" to listOf(
                mapOf("id" to 1, "customer_id" to 42, "product" to "Widget")
            ),
            "customers" to listOf(
                mapOf("id" to 42, "name" to "Alice")
            )
        )

        val opts = FromDictOptions(true, smartOrder = true)
        val doc = ISON.fromDictWithOptions(data, opts)

        val orders = doc.get("orders")!!
        val custId = orders.rows[0]["customer_id"]
        val ref = custId?.asRef()
        assertEquals("42", ref?.id)
    }

    @Test
    fun testSmartOrderFields() {
        val fields = listOf("email", "customer_id", "name", "id", "status")
        val ordered = ISON.smartOrderFields(fields)

        assertEquals("id", ordered[0])
        assertEquals("name", ordered[1])
        assertEquals("customer_id", ordered[ordered.size - 1])
    }

    @Test
    fun testDefaultDumpsOptions() {
        val opts = Dump.defaultDumpsOptions()
        assertEquals(false, opts.alignColumns)
        assertEquals(" ", opts.delimiter)
    }

    @Test
    fun testDefaultFromDictOptions() {
        val opts = ISON.defaultFromDictOptions()
        assertEquals(false, opts.autoRefs)
        assertEquals(false, opts.smartOrder)
    }
}