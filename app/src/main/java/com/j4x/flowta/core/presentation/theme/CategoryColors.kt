package com.j4x.flowta.core.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Colors for expense categories: 12 clearly different hues that also vary in lightness, readable on
 * light and dark backgrounds. Slots are handed out by position (see [assignColors]), never by hash,
 * so categories shown together never collide while there are at most [categoryPalette] entries.
 */
internal val categoryPalette = listOf(
    Color(0xFFE53935), // red
    Color(0xFFF57C00), // orange
    Color(0xFFFFC107), // gold
    Color(0xFFAFB42B), // olive
    Color(0xFF2E7D32), // green
    Color(0xFF26A69A), // teal
    Color(0xFF1E88E5), // blue
    Color(0xFF3949AB), // indigo
    Color(0xFF8E24AA), // purple
    Color(0xFFEC407A), // pink
    Color(0xFF795548), // brown
    Color(0xFF607D8B)  // slate
)

/** Greens for income sources, a separate palette so they read as "money in" next to the category colors. */
internal val incomeSourcePalette = listOf(
    Color(0xFF1B5E20), Color(0xFF4CAF50), Color(0xFFA5D6A7), Color(0xFF00796B),
    Color(0xFF4DB6AC), Color(0xFF8BC34A), Color(0xFFCDDC39), Color(0xFF827717)
)

/** Income that has no source (also the fallback for ids that no longer exist). */
internal val NoSourceColor = Color(0xFF95A5A6)

/**
 * Maps each id to the palette slot of its position in [orderedIds]. Distinct ids get distinct colors
 * while there are at most [palette] entries; beyond that the palette wraps around, so the (n + 1)-th id
 * shares the color of the first. A repeated id keeps the slot of its first occurrence. The mapping depends
 * only on the order of ids (not on amounts or periods), so it is stable as long as that order is.
 */
internal fun assignColors(orderedIds: List<String>, palette: List<Color>): Map<String, Color> {
    require(palette.isNotEmpty()) { "palette must not be empty" }
    val colors = LinkedHashMap<String, Color>()
    orderedIds.forEach { id ->
        if (id !in colors) colors[id] = palette[colors.size % palette.size]
    }
    return colors
}

/** Resolved colors for the Metrics screen; composables only look colors up. */
class MetricsColors internal constructor(
    private val categories: Map<String, Color>,
    private val sources: Map<String, Color>
) {
    /** Color of an expense category; neutral gray when the id is unknown (e.g. a deleted type). */
    fun category(id: String): Color = categories[id] ?: NoSourceColor

    /** Color of an income source; null (income without source) and unknown ids are neutral gray. */
    fun incomeSource(id: String?): Color = if (id == null) NoSourceColor else sources[id] ?: NoSourceColor
}

/** Builds [MetricsColors] from the ids of expense categories and income sources, each in stable order. */
fun metricsColors(expenseIds: List<String>, sourceIds: List<String>): MetricsColors =
    MetricsColors(
        categories = assignColors(expenseIds, categoryPalette),
        sources = assignColors(sourceIds, incomeSourcePalette)
    )
