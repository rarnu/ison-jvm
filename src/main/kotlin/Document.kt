package com.rarnu.ison

import com.isyscore.kotlin.common.toJson

/**
 * represents a parsed ISON document containing multiple blocks
 */
data class Document @JvmOverloads constructor(
    var blocks: MutableMap<String, Block> = mutableMapOf(),
    /**
     * Block names in order of appearance
     */
    var order: MutableList<String> = mutableListOf()
) {

    /**
     * adds a block to the document
     */
    fun addBlock(block: Block) {
        if (!blocks.containsKey(block.name)) {
            order.add(block.name)
        }
        blocks[block.name] = block
    }

    /**
     * returns a block by name
     */
    fun get(name: String): Block? = blocks[name]

    /**
     * converts the document to a map representation
     */
    fun toDict(): Map<String, Any?> = blocks.mapValues { (_, block) -> block.toDict() }

    /**
     * converts the document to JSON
     */
    fun toJson(): String {
        val result = blocks.mapValues { (_, block) ->
            block.rows.map { r -> r.mapValues { (_, v) -> v.intf() } }
        }
        return result.toJson()
    }
}
