package com.rarnu.ison.test

import com.rarnu.ison.toIson
import com.rarnu.ison.toIsonObj
import com.rarnu.ison.toIsonTable
import org.junit.Test

class TestObjConvert {

    data class User(
        var id: Long = 0L,
        var name: String = "",
        var email: String = ""
    )

    @Test
    fun testTableToISON() {
        val list = listOf(
            User(1, "Alice", "alice@example.com"),
            User(2, "Bob", "bob@example.com")
        )
        val isonText = list.toIson("user")
        println(isonText)
    }

    @Test
    fun testISONToTable() {
        val isonText = """
table.user
id name email
1 Alice alice@example.com
2 Bob bob@example.com
"""
        val list = isonText.toIsonTable<User>("user")
        println(list)
    }

    @Test
    fun testObjectToISON() {
        val user = User(1, "Alice", "alice@example.com")
        val isonText = user.toIson("user")
        println(isonText)
    }

    @Test
    fun testISONToObject() {
        val isonText = """
object.user
id name email
1 Alice alice@example.com
"""
        val user = isonText.toIsonObj<User>("user")
        println(user)
    }

}