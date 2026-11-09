package app.orchard.tv.settings

import androidx.annotation.StringRes
import app.orchard.tv.R

/**
 * How a parent confirms it is them in Kids and Elders mode (Restricted): the PIN or a question
 * (Challenges). The PIN is the safe default. A question is a weaker check a child may pass; it is
 * here for families who want one, and it never replaces the PIN as the way into settings.
 */
enum class KidsGate(val key: String) {
    Pin("pin"),
    Math("math"),
    Pinyin("pinyin"),
    English("english"),
    Common("common");

    /** A question rather than the PIN. */
    val asks: Boolean get() = this != Pin

    companion object {
        fun of(key: String?): KidsGate = entries.firstOrNull { it.key == key } ?: Pin

        /**
         * The gates this interface can offer: pinyin is Chinese content, so it is left out of the
         * others (a question in Chinese on a Russian TV would be a puzzle of its own).
         */
        fun available(language: String): List<KidsGate> =
            entries.filter { it != Pinyin || language.startsWith("zh") }
    }
}

/** The gate's name in the interface's language. */
@get:StringRes
val KidsGate.label: Int
    get() = when (this) {
        KidsGate.Pin -> R.string.gate_pin
        KidsGate.Math -> R.string.gate_math
        KidsGate.Pinyin -> R.string.gate_pinyin
        KidsGate.English -> R.string.gate_english
        KidsGate.Common -> R.string.gate_common
    }
