package com.myiphone.store.comprador

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formato {
    private val localeCo = Locale("es", "CO")

    /** 4999000 -> "$ 4.999.000" */
    fun cop(valor: Long): String {
        val nf = NumberFormat.getIntegerInstance(localeCo)
        return "$ " + nf.format(valor)
    }

    fun fecha(millis: Long): String =
        SimpleDateFormat("dd MMM yyyy, HH:mm", localeCo).format(Date(millis))
}
