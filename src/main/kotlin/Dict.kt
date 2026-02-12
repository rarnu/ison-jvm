package com.rarnu.ison

/**
 * configures FromDict behavior
 */
data class FromDictOptions @JvmOverloads constructor(
    /**
     * Auto-detect and convert foreign keys to References
     */
    var autoRefs: Boolean = false,
    /**
     * Reorder columns for optimal LLM comprehension
     */
    var smartOrder: Boolean = false
)