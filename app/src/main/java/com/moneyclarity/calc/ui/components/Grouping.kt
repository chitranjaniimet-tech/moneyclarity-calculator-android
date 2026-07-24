package com.moneyclarity.calc.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Shows digits grouped the Indian way while the field holds plain digits.
 * Typing 5000000 reads as 50,00,000, which is the difference between a number
 * you can check at a glance and one you have to count.
 */
class IndianGroupingTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val dot = raw.indexOf('.')
        val intPart = if (dot >= 0) raw.substring(0, dot) else raw
        val rest = if (dot >= 0) raw.substring(dot) else ""
        val grouped = group(intPart) + rest

        // Map every original index onto its position in the grouped string by
        // walking the output and counting the characters that are not separators.
        val forward = IntArray(raw.length + 1)
        var original = 0
        for (i in grouped.indices) {
            if (grouped[i] != ',') {
                if (original <= raw.length) forward[original] = i
                original++
            }
        }
        forward[raw.length] = grouped.length

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                forward[offset.coerceIn(0, raw.length)]

            override fun transformedToOriginal(offset: Int): Int {
                val capped = offset.coerceIn(0, grouped.length)
                var count = 0
                for (i in 0 until capped) if (grouped[i] != ',') count++
                return count.coerceIn(0, raw.length)
            }
        }
        return TransformedText(AnnotatedString(grouped), mapping)
    }

    private fun group(s: String): String {
        if (s.length <= 3) return s
        val last3 = s.substring(s.length - 3)
        var head = s.substring(0, s.length - 3)
        val parts = mutableListOf<String>()
        while (head.length > 2) {
            parts.add(0, head.substring(head.length - 2))
            head = head.substring(0, head.length - 2)
        }
        if (head.isNotEmpty()) parts.add(0, head)
        return parts.joinToString(",") + "," + last3
    }
}
