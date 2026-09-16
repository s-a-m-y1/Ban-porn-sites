package com.contentfilter.app

object SupportConfig {
    const val email = "sam858y@gmail.com"
    const val message = "حِصن مشروع وقفي لحماية الأسرة. دعمك يساعدنا على الاستمرار والتطوير."
    const val cta = "ادعم حِصن"
    data class Payment(val id: String, val name: String, val enabled: Boolean, val placeholder: Boolean, val url: String? = null)
    val payments = listOf(
        Payment("paymob", "Paymob", false, true),
        Payment("fawry", "Fawry", false, true),
        Payment("vodafone_cash", "Vodafone Cash", false, true)
    )
}
