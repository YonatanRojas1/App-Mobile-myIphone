package com.myiphone.store.comprador

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Datos públicos de la integración con ePayco.
 * Solo se usa la llave PÚBLICA: la privada nunca va dentro de la app.
 * test = true => ambiente de pruebas (no se cobra dinero real).
 */
object EpaycoConfig {
    const val PUBLIC_KEY = "1f82cab2e967902fe301562cae648b21"
    const val MODO_DEMO = true
    const val TEST = true
    /**
     * Solo en pruebas: tope del valor que se envía a ePayco. La cuenta de prueba tiene un rango
     * transaccional limitado y rechaza montos de millones. El pedido guarda el total real.
     * Si ePayco aún lo rechaza, baja este valor (ej. 20000).
     */
    const val MONTO_MAX_PRUEBA = 10000L
    /** URL de respuesta ficticia: la app la intercepta antes de que cargue. */
    const val URL_RESPUESTA = "https://myiphone.app/respuesta"
    /** Endpoint público de ePayco para consultar el estado de una transacción por su ref_payco. */
    const val URL_VALIDACION = "https://secure.epayco.co/validation/v1/reference/"
}

/**
 * Pantalla de pago con ePayco (Standard Checkout dentro de un WebView).
 * Flujo: abre el checkout -> el usuario paga -> ePayco redirige con ref_payco ->
 * la app consulta el estado real de la transacción -> devuelve el resultado a PagoActivity.
 * Extras de entrada: monto (Long), descripcion, email, nombre, factura.
 * Resultado: RESULT_OK con "ref" y "estado" si fue aceptada; RESULT_CANCELED con "mensaje" si no.
 */
class EpaycoActivity : AppCompatActivity() {

    private lateinit var web: WebView
    private var verificando = false

    /** Configura el WebView y carga la página que abre el checkout de ePayco. */
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        setContentView(web)

        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.javaScriptCanOpenWindowsAutomatically = true
        web.webChromeClient = WebChromeClient()
        web.webViewClient = object : WebViewClient() {
            // Navegación principal: si es la URL de respuesta, la procesamos nosotros
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                interceptar(request.url.toString())

            // Ve TODAS las peticiones (incluido el iframe y los XHR del checkout).
            // Si alguna trae la referencia ref_payco, ya podemos consultar el estado del pago.
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val url = request.url.toString()
                Log.d("EPAYCO", url)
                val ref = extraerRef(url)
                if (ref != null || url.startsWith(EpaycoConfig.URL_RESPUESTA)) {
                    runOnUiThread { procesar(ref) }
                    if (url.startsWith(EpaycoConfig.URL_RESPUESTA))
                        return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                }
                return null
            }
        }

        val real = intent.getLongExtra("monto", 0)
        val monto = if (EpaycoConfig.TEST) minOf(real, EpaycoConfig.MONTO_MAX_PRUEBA) else real
        val html = paginaCheckout(
            intent.getStringExtra("descripcion").orEmpty(), monto.toString(),
            intent.getStringExtra("factura").orEmpty(),
            intent.getStringExtra("email").orEmpty(), intent.getStringExtra("nombre").orEmpty()
        )
        web.loadDataWithBaseURL("https://myiphone.app/", html, "text/html", "UTF-8", null)
    }

    /** Arma el HTML que carga checkout.js de ePayco y abre el modal de pago con los datos de la compra. */
    private fun paginaCheckout(desc: String, monto: String, factura: String, email: String, nombre: String): String {
        fun q(s: String) = JSONObject.quote(s)
        return """
            <html><head><meta name="viewport" content="width=device-width, initial-scale=1">
            <script src="https://checkout.epayco.co/checkout.js"></script></head>
            <body style="font-family:sans-serif;text-align:center;background:#F4F6FA">
            <p style="margin-top:40px;color:#1F2A44">Abriendo ePayco...</p>
            <button onclick="abrir()" style="padding:12px 24px;border:0;border-radius:12px;background:#3E7BFA;color:#fff;font-size:16px">Pagar con ePayco</button>
            <script>
            var handler = ePayco.checkout.configure({ key: ${q(EpaycoConfig.PUBLIC_KEY)}, test: ${EpaycoConfig.TEST} });
            function abrir() {
              handler.open({
                name: "MyIphone", description: ${q(desc)}, invoice: ${q(factura)},
                currency: "cop", amount: ${q(monto)}, tax_base: "0", tax: "0", country: "co", lang: "es",
                external: "false", response: ${q(EpaycoConfig.URL_RESPUESTA)},
                email_billing: ${q(email)}, name_billing: ${q(nombre)}
              });
            }
            window.onload = function() { setTimeout(abrir, 800); };
            </script></body></html>
        """.trimIndent()
    }

    /** Si la URL es la de respuesta de ePayco, saca ref_payco y verifica la transacción. */
    private fun interceptar(url: String): Boolean {
        if (!url.startsWith(EpaycoConfig.URL_RESPUESTA)) return false
        procesar(extraerRef(url))
        return true
    }

    /** Busca el ref_payco en la URL: como parámetro (?ref_payco=123) o en la ruta (/reference/123). */
    private fun extraerRef(url: String): String? =
        Regex("""ref_payco=(\d+)""").find(url)?.groupValues?.get(1)
            ?: Regex("""/validation/v1/reference/(\d+)""").find(url)?.groupValues?.get(1)

    /** Lanza la verificación una sola vez, aunque lleguen varias señales del checkout. */
    private fun procesar(ref: String?) {
        if (verificando || ref.isNullOrBlank()) return
        verificando = true
        verificar(ref)
    }

    /**
     * Consulta a ePayco el estado real del pago (x_cod_response: 1 = Aceptada, 2 = Rechazada,
     * 3 = Pendiente, 4 = Fallida) y termina la pantalla con el resultado.
     */
    private fun verificar(ref: String?) {
        if (ref.isNullOrBlank()) { terminar(false, "", "", "No se recibió la referencia de pago."); return }
        Thread {
            try {
                var codigo = 0
                var estado = ""
                // Si la transacción aún está pendiente (3), reintenta hasta 8 veces cada 4 s
                for (intento in 1..8) {
                    val con = URL(EpaycoConfig.URL_VALIDACION + ref).openConnection() as HttpURLConnection
                    con.connectTimeout = 15000; con.readTimeout = 15000
                    val json = JSONObject(con.inputStream.bufferedReader().readText())
                    val data = json.optJSONObject("data")
                    codigo = data?.optInt("x_cod_response") ?: 0
                    estado = data?.optString("x_response").orEmpty()
                    Log.d("EPAYCO", "verificación $intento: $codigo $estado")
                    if (codigo != 3 && codigo != 0) break
                    Thread.sleep(4000)
                }
                terminar(codigo == 1, ref, estado, "El pago quedó en estado: $estado")
            } catch (e: Exception) {
                terminar(false, ref, "", "No se pudo confirmar el pago: ${e.message}")
            }
        }.start()
    }

    /** Devuelve el resultado a PagoActivity y cierra esta pantalla. */
    private fun terminar(ok: Boolean, ref: String, estado: String, mensaje: String) {
        runOnUiThread {
            val data = Intent().putExtra("ref", ref).putExtra("estado", estado).putExtra("mensaje", mensaje)
            setResult(if (ok) RESULT_OK else RESULT_CANCELED, data)
            finish()
        }
    }

    /** Atrás cancela el pago. */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        setResult(RESULT_CANCELED, Intent().putExtra("mensaje", getString(com.myiphone.store.R.string.epayco_cancelado)))
        finish()
    }
}
