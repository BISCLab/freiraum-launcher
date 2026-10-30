package com.bisc.flauncher.util

/** Parses queries like "10 km to mi" / "5 kg in lb" / "100 c to f". Purely static/offline - no
 *  currency conversion here, that needs live exchange rates (a separate, later, API-backed phase). */
private val CONVERT_REGEX = Regex("""^\s*([-+]?[0-9]*[.,]?[0-9]+)\s*([a-zA-Z°]+)\s+(?:to|in)\s+([a-zA-Z°]+)\s*$""")

private val LENGTH_TO_METERS = mapOf(
    "mm" to 0.001, "millimeter" to 0.001, "millimeters" to 0.001,
    "cm" to 0.01, "centimeter" to 0.01, "centimeters" to 0.01,
    "m" to 1.0, "meter" to 1.0, "meters" to 1.0,
    "km" to 1000.0, "kilometer" to 1000.0, "kilometers" to 1000.0,
    "in" to 0.0254, "inch" to 0.0254, "inches" to 0.0254,
    "ft" to 0.3048, "foot" to 0.3048, "feet" to 0.3048,
    "yd" to 0.9144, "yard" to 0.9144, "yards" to 0.9144,
    "mi" to 1609.344, "mile" to 1609.344, "miles" to 1609.344
)

private val WEIGHT_TO_GRAMS = mapOf(
    "mg" to 0.001, "milligram" to 0.001, "milligrams" to 0.001,
    "g" to 1.0, "gram" to 1.0, "grams" to 1.0,
    "kg" to 1000.0, "kilogram" to 1000.0, "kilograms" to 1000.0,
    "oz" to 28.349523125, "ounce" to 28.349523125, "ounces" to 28.349523125,
    "lb" to 453.59237, "lbs" to 453.59237, "pound" to 453.59237, "pounds" to 453.59237
)

private val VOLUME_TO_LITERS = mapOf(
    "ml" to 0.001, "milliliter" to 0.001, "milliliters" to 0.001,
    "l" to 1.0, "liter" to 1.0, "liters" to 1.0, "litre" to 1.0, "litres" to 1.0,
    "gal" to 3.785411784, "gallon" to 3.785411784, "gallons" to 3.785411784,
    "qt" to 0.946352946, "quart" to 0.946352946, "quarts" to 0.946352946,
    "cup" to 0.2365882365, "cups" to 0.2365882365
)

private val TEMPERATURE_UNITS = setOf("c", "f", "k", "celsius", "fahrenheit", "kelvin")

/** Null if [query] doesn't parse as "<amount> <unit> to|in <unit>" or the units aren't a known,
 *  compatible pair (length, weight, volume or temperature - never mixed categories). */
fun tryConvert(query: String): String? {
    val match = CONVERT_REGEX.matchEntire(query.trim()) ?: return null
    val amount = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
    val from = match.groupValues[2].lowercase()
    val to = match.groupValues[3].lowercase()

    convertTemperature(amount, from, to)?.let { return formatConversion(it, match.groupValues[3]) }
    convertViaTable(amount, from, to, LENGTH_TO_METERS)?.let { return formatConversion(it, match.groupValues[3]) }
    convertViaTable(amount, from, to, WEIGHT_TO_GRAMS)?.let { return formatConversion(it, match.groupValues[3]) }
    convertViaTable(amount, from, to, VOLUME_TO_LITERS)?.let { return formatConversion(it, match.groupValues[3]) }
    return null
}

private fun convertViaTable(amount: Double, from: String, to: String, table: Map<String, Double>): Double? {
    val fromFactor = table[from] ?: return null
    val toFactor = table[to] ?: return null
    return amount * fromFactor / toFactor
}

private fun convertTemperature(amount: Double, from: String, to: String): Double? {
    if (from !in TEMPERATURE_UNITS || to !in TEMPERATURE_UNITS) return null
    val celsius = when (from) {
        "c", "celsius" -> amount
        "f", "fahrenheit" -> (amount - 32) * 5.0 / 9.0
        "k", "kelvin" -> amount - 273.15
        else -> return null
    }
    return when (to) {
        "c", "celsius" -> celsius
        "f", "fahrenheit" -> celsius * 9.0 / 5.0 + 32
        "k", "kelvin" -> celsius + 273.15
        else -> return null
    }
}

private fun formatConversion(value: Double, unitLabel: String): String {
    val rounded = kotlin.math.round(value * 1000.0) / 1000.0
    val text = if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString() else rounded.toString().trimEnd('0').trimEnd('.')
    return "$text $unitLabel"
}
