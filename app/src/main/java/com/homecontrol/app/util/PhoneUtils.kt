package com.homecontrol.app.util

import android.telephony.SmsMessage

data class Country(val name: String, val code: String)

object Countries {
    val all = listOf(
        Country("ایران", "+98"),
        Country("افغانستان", "+93"),
        Country("عراق", "+964"),
        Country("ترکیه", "+90"),
        Country("امارات", "+971"),
        Country("آذربایجان", "+994"),
        Country("ارمنستان", "+374"),
        Country("پاکستان", "+92"),
        Country("ترکمنستان", "+993"),
        Country("قطر", "+974"),
        Country("کویت", "+965"),
        Country("عربستان", "+966"),
        Country("عمان", "+968"),
        Country("آلمان", "+49"),
        Country("بریتانیا", "+44"),
        Country("آمریکا و کانادا", "+1")
    )
}

object PhoneUtils {
    /** Converts Persian/Arabic-Indic digits to ASCII digits. */
    fun asciiDigits(input: String): String = buildString {
        for (c in input) {
            append(
                when (c) {
                    in '۰'..'۹' -> '0' + (c - '۰')
                    in '٠'..'٩' -> '0' + (c - '٠')
                    else -> c
                }
            )
        }
    }

    /**
     * Returns the number in international format (+989121234567) or null if invalid.
     * Accepts: 09121234567, 9121234567, 00989121234567, +989121234567.
     */
    fun normalize(raw: String, countryCode: String): String? {
        val s = asciiDigits(raw).filter { it.isDigit() || it == '+' }
        if (s.isEmpty()) return null
        val full = when {
            s.startsWith("+") -> s
            s.startsWith("00") -> "+" + s.drop(2)
            s.startsWith("0") -> countryCode + s.drop(1)
            else -> countryCode + s
        }
        val digits = full.drop(1)
        val valid = full.count { it == '+' } == 1 && digits.all { it.isDigit() } && digits.length in 8..15
        return if (valid) full else null
    }
}

object SmsUtils {
    /** Number of SMS segments needed for [text]. */
    fun partsCount(text: String): Int = SmsMessage.calculateLength(text, false)[0]
    fun fitsOneSms(text: String): Boolean = partsCount(text) <= 1
}
