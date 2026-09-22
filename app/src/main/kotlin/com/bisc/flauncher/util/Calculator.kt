package com.bisc.flauncher.util

private val OPERATOR_CHARS = charArrayOf('+', '-', '×', '÷', '*', '/', '%')

/** Whether [query] is worth attempting to evaluate at all. */
fun looksLikeCalculatorExpression(query: String): Boolean {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return false
    if (trimmed.none { it.isDigit() }) return false
    if (trimmed.none { it in OPERATOR_CHARS }) return false
    return trimmed.all {
        it.isDigit() || it.isWhitespace() || it in OPERATOR_CHARS || it == '.' || it == ',' || it == '(' || it == ')'
    }
}

/** Evaluates an arithmetic expression; null if it is malformed. */
fun evaluateCalculatorExpression(query: String): Double? {
    val normalized = query.trim()
        .replace(',', '.')
        .replace('×', '*')
        .replace('÷', '/')
    return try {
        val parser = ExpressionParser(normalized)
        val result = parser.parseExpression()
        if (!parser.isAtEnd()) null else result
    } catch (e: ArithmeticException) {
        null
    } catch (e: IllegalStateException) {
        null
    }
}

/** Trims a calculator result to something readable. */
fun formatCalculatorResult(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "—"
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        val rounded = kotlin.math.round(value * 1_000_000.0) / 1_000_000.0
        rounded.toString().trimEnd('0').trimEnd('.')
    }
}

private class ExpressionParser(private val text: String) {
    private var pos = 0

    fun isAtEnd(): Boolean {
        skipWhitespace()
        return pos >= text.length
    }

    fun parseExpression(): Double {
        var value = parseTerm()
        while (true) {
            skipWhitespace()
            when (peek()) {
                '+' -> { pos++; value += parseTerm() }
                '-' -> { pos++; value -= parseTerm() }
                else -> return value
            }
        }
    }

    private fun parseTerm(): Double {
        var value = parseFactor()
        while (true) {
            skipWhitespace()
            when (peek()) {
                '*' -> { pos++; value *= parseFactor() }
                '/' -> {
                    pos++
                    val divisor = parseFactor()
                    if (divisor == 0.0) throw ArithmeticException("Division by zero")
                    value /= divisor
                }
                '%' -> { pos++; value %= parseFactor() }
                else -> return value
            }
        }
    }

    private fun parseFactor(): Double {
        skipWhitespace()
        return when (peek()) {
            '-' -> { pos++; -parseFactor() }
            '+' -> { pos++; parseFactor() }
            '(' -> {
                pos++
                val value = parseExpression()
                skipWhitespace()
                if (peek() != ')') throw IllegalStateException("Expected ) at $pos")
                pos++
                value
            }
            else -> parseNumber()
        }
    }

    private fun parseNumber(): Double {
        skipWhitespace()
        val start = pos
        while (pos < text.length && (text[pos].isDigit() || text[pos] == '.')) pos++
        if (start == pos) throw IllegalStateException("Expected number at $pos")
        return text.substring(start, pos).toDouble()
    }

    private fun peek(): Char? = if (pos < text.length) text[pos] else null

    private fun skipWhitespace() {
        while (pos < text.length && text[pos].isWhitespace()) pos++
    }
}
