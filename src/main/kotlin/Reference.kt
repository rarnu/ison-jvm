package com.rarnu.ison

import com.isyscore.kotlin.common.toJson

/**
 * represents an ISON reference (e.g., :1, :user:42, :OWNS:5)
 */
data class Reference @JvmOverloads constructor(
    var id: String = "",
    var namespace: String = "",
    var relationship: String = ""
) {

    /**
     * converts the reference back to ISON format
     */
    fun toIson(): String {
        if (relationship.isNotBlank()) {
            return ":${relationship}:${id}"
        }
        if (namespace.isNotBlank()) {
            return ":${namespace}:${id}"
        }
        return ":${id}"
    }

    /**
     * returns true if this is a relationship reference (uppercase namespace)
     */
    fun isRelationship(): Boolean = relationship.isNotBlank()

    /**
     * returns the namespace or relationship name
     */
    fun getNsOrRel(): String = relationship.ifBlank { namespace }

    /**
     * returns the string representation of the reference
     */
    override fun toString(): String = toIson()

    fun json(): String = mapOf(
        "_ref" to id,
        "_namespace" to namespace,
        "_relationship" to relationship
    ).toJson()

}