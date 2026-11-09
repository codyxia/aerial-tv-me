package app.orchard.tv.settings

import kotlin.random.Random

/**
 * One question a parent answers instead of the PIN when [KidsGate] says so: a prompt and four
 * choices, one of them right. All of it is made here, offline, and answered with the remote's
 * direction keys and OK — no keyboard, no network, no content from anywhere else.
 */
data class Challenge(val prompt: String, val options: List<String>, val answer: Int) {
    fun isRight(choice: Int): Boolean = choice == answer
}

/**
 * The question banks behind [KidsGate]. Questions are content rather than interface text, so the
 * few sentences that are not numbers live here; the panel's labels are in res/values as usual.
 *
 * A question is a weak parental check: a child of the age this mode is for may well answer it.
 * The PIN stays the way into settings and, unless the owner allows it, the way out of the mode.
 */
object Challenges {
    private const val CHOICES = 4

    /**
     * The next question of [gate], or null when this interface has no bank for it (pinyin outside
     * Chinese). [avoid] is the prompt just answered, so a retry does not show the same one again.
     */
    fun next(gate: KidsGate, language: String, random: Random = Random.Default, avoid: String? = null): Challenge? {
        repeat(6) {
            val question = generate(gate, language, random) ?: return null
            if (question.prompt != avoid) return question
        }
        return generate(gate, language, random)
    }

    private fun generate(gate: KidsGate, language: String, random: Random): Challenge? = when (gate) {
        KidsGate.Pin -> null
        KidsGate.Math -> math(random)
        KidsGate.Pinyin -> if (language.startsWith("zh")) pinyin(random) else null
        KidsGate.English -> english(language, random)
        KidsGate.Common -> common(language, random)
    }

    /** Addition and subtraction within 100: a fresh one every time, and no bank to run out of. */
    private fun math(random: Random): Challenge {
        val a = random.nextInt(10, 100)
        val b = random.nextInt(1, a + 1)
        val answer = a - b
        val values = LinkedHashSet<Int>()
        values += answer
        var guard = 0
        while (values.size < CHOICES && guard++ < 100) values += (answer + random.nextInt(-9, 10)).coerceAtLeast(0)
        while (values.size < CHOICES) values += random.nextInt(0, 200)
        val options = values.toList().shuffled(random)
        return Challenge("$a − $b = ?", options.map(Int::toString), options.indexOf(answer))
    }

    private data class Pinyin(val word: String, val pinyin: String)

    private fun pinyin(random: Random): Challenge? {
        val word = PINYIN.random(random)
        val wrong = PINYIN.map { it.pinyin }.filter { it != word.pinyin }.distinct().shuffled(random).take(CHOICES - 1)
        if (wrong.size < CHOICES - 1) return null
        val options = (wrong + word.pinyin).shuffled(random)
        return Challenge("「${word.word}」的拼音是？", options, options.indexOf(word.pinyin))
    }

    private data class Opposite(val word: String, val answer: String, val options: List<String>)

    private fun english(language: String, random: Random): Challenge? {
        val question = OPPOSITES.random(random)
        val options = question.options.distinct().shuffled(random)
        if (options.size != CHOICES || question.answer !in options) return null
        val prompt = (ENGLISH_PROMPT[language] ?: ENGLISH_PROMPT.getValue("en")).format(question.word)
        return Challenge(prompt, options, options.indexOf(question.answer))
    }

    private data class Quiz(val prompt: String, val options: List<String>, val answer: Int)

    private fun common(language: String, random: Random): Challenge? {
        val bank = COMMON[language] ?: COMMON.getValue("en")
        val quiz = bank.random(random)
        if (quiz.options.size != CHOICES || quiz.answer !in quiz.options.indices) return null
        return Challenge(quiz.prompt, quiz.options, quiz.answer)
    }

    // Words a child learning pinyin meets early. Tone marks are part of the answer on purpose.
    private val PINYIN = listOf(
        Pinyin("苹果", "píng guǒ"),
        Pinyin("电视", "diàn shì"),
        Pinyin("学校", "xué xiào"),
        Pinyin("朋友", "péng you"),
        Pinyin("天空", "tiān kōng"),
        Pinyin("老师", "lǎo shī"),
        Pinyin("中国", "zhōng guó"),
        Pinyin("音乐", "yīn yuè"),
        Pinyin("电脑", "diàn nǎo"),
        Pinyin("汽车", "qì chē"),
        Pinyin("水果", "shuǐ guǒ"),
        Pinyin("时间", "shí jiān"),
        Pinyin("北京", "běi jīng"),
        Pinyin("月亮", "yuè liang"),
        Pinyin("森林", "sēn lín"),
        Pinyin("飞机", "fēi jī"),
        Pinyin("牛奶", "niú nǎi"),
        Pinyin("花园", "huā yuán"),
        Pinyin("孩子", "hái zi"),
        Pinyin("太阳", "tài yáng"),
        Pinyin("下雨", "xià yǔ"),
        Pinyin("小猫", "xiǎo māo"),
        Pinyin("公园", "gōng yuán"),
        Pinyin("早晨", "zǎo chén"),
    )

    // English opposites; the prompt says "opposite of <word>" in the interface's language.
    private val OPPOSITES = listOf(
        Opposite("hot", "cold", listOf("warm", "soft", "fast", "cold")),
        Opposite("big", "small", listOf("tall", "long", "wide", "small")),
        Opposite("fast", "slow", listOf("quick", "late", "hard", "slow")),
        Opposite("happy", "sad", listOf("angry", "tired", "sad", "busy")),
        Opposite("day", "night", listOf("morning", "noon", "evening", "night")),
        Opposite("open", "close", listOf("start", "close", "move", "turn")),
        Opposite("old", "new", listOf("young", "new", "large", "late")),
        Opposite("wet", "dry", listOf("cold", "dry", "warm", "clean")),
        Opposite("up", "down", listOf("over", "down", "left", "out")),
        Opposite("loud", "quiet", listOf("quiet", "strong", "high", "deep")),
    )

    private val ENGLISH_PROMPT = mapOf(
        "en" to "Which word is the opposite of “%s”?",
        "zh" to "哪个词是「%s」的反义词？",
        "ru" to "Какое слово противоположно «%s»?",
        "uk" to "Яке слово є протилежним до «%s»?",
        "kk" to "«%s» сөзіне қай сөз қарсы?",
    )

    private val COMMON: Map<String, List<Quiz>> = mapOf(
        "en" to listOf(
            Quiz("How many days are in a week?", listOf("5", "6", "7", "8"), 2),
            Quiz("How many months are in a year?", listOf("10", "11", "12", "13"), 2),
            Quiz("Which season comes after spring?", listOf("summer", "autumn", "winter", "rainy"), 0),
            Quiz("How many legs does a spider have?", listOf("6", "8", "10", "12"), 1),
            Quiz("Which of these is a fruit?", listOf("apple", "potato", "carrot", "onion"), 0),
            Quiz("What do plants need to grow?", listOf("water", "stone", "plastic", "metal"), 0),
            Quiz("How many hours are in a day?", listOf("12", "24", "30", "48"), 1),
            Quiz("Which animal can fly?", listOf("bird", "fish", "dog", "horse"), 0),
        ),
        "zh" to listOf(
            Quiz("一星期有几天？", listOf("5", "6", "7", "8"), 2),
            Quiz("一年有几个月？", listOf("10", "11", "12", "13"), 2),
            Quiz("春天之后是哪个季节？", listOf("夏天", "秋天", "冬天", "雨季"), 0),
            Quiz("蜘蛛有几条腿？", listOf("6", "8", "10", "12"), 1),
            Quiz("下面哪个是水果？", listOf("苹果", "土豆", "胡萝卜", "洋葱"), 0),
            Quiz("植物生长需要什么？", listOf("水", "石头", "塑料", "金属"), 0),
            Quiz("一天有多少小时？", listOf("12", "24", "30", "48"), 1),
            Quiz("哪种动物会飞？", listOf("鸟", "鱼", "狗", "马"), 0),
        ),
        "ru" to listOf(
            Quiz("Сколько дней в неделе?", listOf("5", "6", "7", "8"), 2),
            Quiz("Сколько месяцев в году?", listOf("10", "11", "12", "13"), 2),
            Quiz("Какое время года наступает после весны?", listOf("лето", "осень", "зима", "сезон дождей"), 0),
            Quiz("Сколько ног у паука?", listOf("6", "8", "10", "12"), 1),
            Quiz("Что из этого фрукт?", listOf("яблоко", "картофель", "морковь", "лук"), 0),
            Quiz("Что нужно растениям для роста?", listOf("вода", "камень", "пластик", "металл"), 0),
            Quiz("Сколько часов в сутках?", listOf("12", "24", "30", "48"), 1),
            Quiz("Какое животное умеет летать?", listOf("птица", "рыба", "собака", "лошадь"), 0),
        ),
        "uk" to listOf(
            Quiz("Скільки днів у тижні?", listOf("5", "6", "7", "8"), 2),
            Quiz("Скільки місяців у році?", listOf("10", "11", "12", "13"), 2),
            Quiz("Яка пора року настає після весни?", listOf("літо", "осінь", "зима", "сезон дощів"), 0),
            Quiz("Скільки ніг у павука?", listOf("6", "8", "10", "12"), 1),
            Quiz("Що з цього фрукт?", listOf("яблуко", "картопля", "морква", "цибуля"), 0),
            Quiz("Що потрібно рослинам для росту?", listOf("вода", "камінь", "пластик", "метал"), 0),
            Quiz("Скільки годин у добі?", listOf("12", "24", "30", "48"), 1),
            Quiz("Яка тварина вміє літати?", listOf("птах", "риба", "собака", "кінь"), 0),
        ),
        "kk" to listOf(
            Quiz("Аптада неше күн бар?", listOf("5", "6", "7", "8"), 2),
            Quiz("Бір жылда неше ай бар?", listOf("10", "11", "12", "13"), 2),
            Quiz("Көктемнен кейін қай жыл мезгілі келеді?", listOf("жаз", "күз", "қыс", "жаңбырлы маусым"), 0),
            Quiz("Өрмекшінің неше аяғы бар?", listOf("6", "8", "10", "12"), 1),
            Quiz("Мыналардың қайсысы жеміс?", listOf("алма", "картоп", "сәбіз", "пияз"), 0),
            Quiz("Өсімдіктерге өсу үшін не керек?", listOf("су", "тас", "пластик", "металл"), 0),
            Quiz("Тәулікте неше сағат бар?", listOf("12", "24", "30", "48"), 1),
            Quiz("Қай жануар ұша алады?", listOf("құс", "балық", "ит", "жылқы"), 0),
        ),
    )
}
