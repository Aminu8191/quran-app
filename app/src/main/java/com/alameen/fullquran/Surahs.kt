package com.alameen.fullquran

data class Surah(val n: Int, val en: String, val ar: String, val ayahs: Int) {
    fun title(): String = if (AppSettings.lang == "ha") "Surat $en" else en
}

private val RAW = listOf(
    "Al-Fatihah|الفاتحة", "Al-Baqarah|البقرة", "Ali 'Imran|آل عمران", "An-Nisa|النساء",
    "Al-Ma'idah|المائدة", "Al-An'am|الأنعام", "Al-A'raf|الأعراف", "Al-Anfal|الأنفال",
    "At-Tawbah|التوبة", "Yunus|يونس", "Hud|هود", "Yusuf|يوسف",
    "Ar-Ra'd|الرعد", "Ibrahim|إبراهيم", "Al-Hijr|الحجر", "An-Nahl|النحل",
    "Al-Isra|الإسراء", "Al-Kahf|الكهف", "Maryam|مريم", "Ta-Ha|طه",
    "Al-Anbiya|الأنبياء", "Al-Hajj|الحج", "Al-Mu'minun|المؤمنون", "An-Nur|النور",
    "Al-Furqan|الفرقان", "Ash-Shu'ara|الشعراء", "An-Naml|النمل", "Al-Qasas|القصص",
    "Al-'Ankabut|العنكبوت", "Ar-Rum|الروم", "Luqman|لقمان", "As-Sajdah|السجدة",
    "Al-Ahzab|الأحزاب", "Saba|سبأ", "Fatir|فاطر", "Ya-Sin|يس",
    "As-Saffat|الصافات", "Sad|ص", "Az-Zumar|الزمر", "Ghafir|غافر",
    "Fussilat|فصلت", "Ash-Shura|الشورى", "Az-Zukhruf|الزخرف", "Ad-Dukhan|الدخان",
    "Al-Jathiyah|الجاثية", "Al-Ahqaf|الأحقاف", "Muhammad|محمد", "Al-Fath|الفتح",
    "Al-Hujurat|الحجرات", "Qaf|ق", "Adh-Dhariyat|الذاريات", "At-Tur|الطور",
    "An-Najm|النجم", "Al-Qamar|القمر", "Ar-Rahman|الرحمن", "Al-Waqi'ah|الواقعة",
    "Al-Hadid|الحديد", "Al-Mujadilah|المجادلة", "Al-Hashr|الحشر", "Al-Mumtahanah|الممتحنة",
    "As-Saff|الصف", "Al-Jumu'ah|الجمعة", "Al-Munafiqun|المنافقون", "At-Taghabun|التغابن",
    "At-Talaq|الطلاق", "At-Tahrim|التحريم", "Al-Mulk|الملك", "Al-Qalam|القلم",
    "Al-Haqqah|الحاقة", "Al-Ma'arij|المعارج", "Nuh|نوح", "Al-Jinn|الجن",
    "Al-Muzzammil|المزمل", "Al-Muddaththir|المدثر", "Al-Qiyamah|القيامة", "Al-Insan|الإنسان",
    "Al-Mursalat|المرسلات", "An-Naba|النبأ", "An-Nazi'at|النازعات", "'Abasa|عبس",
    "At-Takwir|التكوير", "Al-Infitar|الانفطار", "Al-Mutaffifin|المطففين", "Al-Inshiqaq|الانشقاق",
    "Al-Buruj|البروج", "At-Tariq|الطارق", "Al-A'la|الأعلى", "Al-Ghashiyah|الغاشية",
    "Al-Fajr|الفجر", "Al-Balad|البلد", "Ash-Shams|الشمس", "Al-Layl|الليل",
    "Ad-Duha|الضحى", "Ash-Sharh|الشرح", "At-Tin|التين", "Al-'Alaq|العلق",
    "Al-Qadr|القدر", "Al-Bayyinah|البينة", "Az-Zalzalah|الزلزلة", "Al-'Adiyat|العاديات",
    "Al-Qari'ah|القارعة", "At-Takathur|التكاثر", "Al-'Asr|العصر", "Al-Humazah|الهمزة",
    "Al-Fil|الفيل", "Quraysh|قريش", "Al-Ma'un|الماعون", "Al-Kawthar|الكوثر",
    "Al-Kafirun|الكافرون", "An-Nasr|النصر", "Al-Masad|المسد", "Al-Ikhlas|الإخلاص",
    "Al-Falaq|الفلق", "An-Nas|الناس"
)

private val AYAHS = intArrayOf(
    7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
    112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85,
    54, 53, 89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13,
    14, 11, 11, 18, 12, 12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42,
    29, 19, 36, 25, 22, 17, 19, 26, 30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11,
    11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6
)

val SURAHS: List<Surah> = RAW.mapIndexed { i, s ->
    val p = s.split("|")
    Surah(i + 1, p[0], p[1], AYAHS[i])
}

fun normalizeName(s: String): String =
    s.lowercase().replace("-", "").replace("'", "").replace(" ", "")
