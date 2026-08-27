package com.example.audio.replacement

import com.example.data.db.WordReplacementEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class TextReplacementProcessorTest {

    @Test
    fun testKnittingWordReplacements() {
        val rules = listOf(
            WordReplacementEntity(id = 1, targetPhrase = "yarn over", replacementPhrase = "yo", isEnabled = true),
            WordReplacementEntity(id = 2, targetPhrase = "knit 1", replacementPhrase = "k1", isEnabled = true),
            WordReplacementEntity(id = 3, targetPhrase = "knit one", replacementPhrase = "k1", isEnabled = true),
            WordReplacementEntity(id = 4, targetPhrase = "purl 2", replacementPhrase = "p2", isEnabled = true),
            WordReplacementEntity(id = 5, targetPhrase = "make 1", replacementPhrase = "m1", isEnabled = true),
            WordReplacementEntity(id = 6, targetPhrase = "slip slip knit", replacementPhrase = "ssk", isEnabled = true),
            WordReplacementEntity(id = 7, targetPhrase = "knit two together", replacementPhrase = "k2tog", isEnabled = true)
        )

        val input1 = "yarn over and knit 1 then purl 2"
        val output1 = TextReplacementProcessor.applyReplacements(input1, rules)
        assertEquals("yo and k1 then p2", output1)

        val input2 = "make 1, slip slip knit, then knit two together"
        val output2 = TextReplacementProcessor.applyReplacements(input2, rules)
        assertEquals("m1, ssk, then k2tog", output2)

        val input3 = "knit one then yarn over."
        val output3 = TextReplacementProcessor.applyReplacements(input3, rules)
        assertEquals("k1 then yo.", output3)
    }

    @Test
    fun testDisabledRulesIgnored() {
        val rules = listOf(
            WordReplacementEntity(id = 1, targetPhrase = "yarn over", replacementPhrase = "yo", isEnabled = false),
            WordReplacementEntity(id = 2, targetPhrase = "knit 1", replacementPhrase = "k1", isEnabled = true)
        )

        val input = "yarn over and knit 1"
        val output = TextReplacementProcessor.applyReplacements(input, rules)
        assertEquals("yarn over and k1", output)
    }
}
