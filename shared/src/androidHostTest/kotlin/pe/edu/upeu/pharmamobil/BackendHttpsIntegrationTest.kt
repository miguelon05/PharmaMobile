package pe.edu.upeu.pharmamobil

import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import pe.edu.upeu.pharmamobil.data.remote.ApiConfig
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.crearHttpClient
import java.io.File
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import kotlin.test.Test
import kotlin.test.assertTrue

/** Opt-in: el JVM de pruebas no utiliza el network_security_config de Android. */
class BackendHttpsIntegrationTest {
    @Test
    fun getRealConCertificadoValidadoDevuelve200() = runBlocking {
        val caPath = System.getenv("PHARMA_HTTPS_CA")
        assumeTrue("Definir PHARMA_HTTPS_CA para ejecutar contra el backend local", caPath != null)
        val certificate = File(requireNotNull(caPath)).inputStream().use {
            CertificateFactory.getInstance("X.509").generateCertificate(it)
        }
        val store = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setCertificateEntry("pharma-local", certificate)
        }
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply { init(store) }
        val manager = factory.trustManagers.filterIsInstance<X509TrustManager>().single()
        val ssl = SSLContext.getInstance("TLS").apply { init(null, arrayOf(manager), null) }
        val engine = OkHttp.create { config { sslSocketFactory(ssl.socketFactory, manager) } }
        val dummyAlmacen = object : pe.edu.upeu.pharmamobil.domain.platform.AlmacenSeguro {
            override fun guardarToken(token: String) {}
            override fun leerToken(): String? = null
            override fun borrarToken() {}
        }
        val client = crearHttpClient(engine, ApiConfig("https://localhost:8443/api/v1/", loggingEnabled = true), dummyAlmacen)
        try {
            val page = ProductoApi(client).listar(0)
            assertTrue(page.totalElementos >= 0)
            println("GET real HTTPS 200: ${page.contenido.size} productos en pagina ${page.pagina}")
        } finally { client.close(); engine.close() }
    }
}
