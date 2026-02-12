package com.rarnu.ison

fun String.containsAny(vararg v: String): Boolean = v.any { this.contains(it) }

fun String.splitN(delimiter: String, n: Int): List<String> = when {
    n == 0 -> emptyList()
    n > 0 -> this.split(delimiter, limit = n)
    else -> this.split(delimiter)  // n < 0，全部分割
}
