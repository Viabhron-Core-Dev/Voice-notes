package com.example.audio.replacement

import com.example.data.db.WordReplacementEntity
import java.util.regex.Pattern

/**
 * High-performance text replacement processor.
 * Transforms recognized voice-to-text outputs using user-defined dictionary rules.
 * Handles boundary checking, phrase priority ordering, and case sensitivity.
 */
object TextReplacementProcessor {

    /**
     * Applies enabled word replacement rules to the input transcription text.
     * Longer phrases are matched first to prevent substring collision (e.g. "knit two together" before "knit").
     */
    fun applyReplacements(rawText: String, rules: List<WordReplacementEntity>): String {
        if (rawText.isBlank() || rules.isEmpty()) return rawText

        var processed = rawText
        // Filter enabled rules and sort by descending target phrase length
        val sortedRules = rules
            .filter { it.isEnabled && it.targetPhrase.isNotBlank() }
            .sortedByDescending { it.targetPhrase.length }

        for (rule in sortedRules) {
            val target = rule.targetPhrase.trim()
            val replacement = rule.replacementPhrase

            try {
                // Whole-word boundary regex with optional punctuation support
                val regexFlags = if (rule.isMatchCase) 0 else Pattern.CASE_INSENSITIVE
                // Escape special regex characters in the target phrase
                val escapedTarget = Pattern.quote(target)
                // Use word boundaries (?:\b|^|(?<=\s)) and (?:\b|$|(?=\s|[.,!?]))
                val regexPattern = Pattern.compile("(?i)(?<=\\b|^|\\s)$escapedTarget(?=\\b|$|\\s|[.,!?;:])", regexFlags)
                
                processed = regexPattern.matcher(processed).replaceAll(replacement)
            } catch (e: Exception) {
                // Fallback to simple replace if regex parsing fails
                processed = if (rule.isMatchCase) {
                    processed.replace(target, replacement)
                } else {
                    processed.replace(target, replacement, ignoreCase = true)
                }
            }
        }

        // Clean up redundant whitespaces created during replacements
        return processed.replace(Regex(" +"), " ").trim()
    }
}
