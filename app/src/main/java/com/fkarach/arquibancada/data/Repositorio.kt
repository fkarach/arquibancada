package com.fkarach.arquibancada.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Lê a agenda publicada no GitHub e guarda a última cópia para funcionar sem internet. */
class Repositorio(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val base = "https://raw.githubusercontent.com/fkarach/arquibancada/main/dados"

    private val arquivoAgenda get() = File(context.filesDir, "agenda.json")
    private val arquivoSeguindo get() = File(context.filesDir, "seguindo.json")
    private val arquivoLogos get() = File(context.filesDir, "logos.json")
    private val pastaLogos get() = File(context.filesDir, "logos").apply { mkdirs() }

    fun agendaSalva(): Agenda? = ler(arquivoAgenda)?.let { runCatching { json.decodeFromString<Agenda>(it) }.getOrNull() }

    fun seguindoSalvo(): Seguindo? = ler(arquivoSeguindo)?.let { runCatching { json.decodeFromString<Seguindo>(it) }.getOrNull() }

    fun logosSalvos(): Map<String, String> =
        ler(arquivoLogos)?.let { runCatching { json.decodeFromString<Logos>(it).logos }.getOrNull() } ?: emptyMap()

    /** Baixa os arquivos de dados e as imagens dos logos. Devolve true se a agenda veio da internet. */
    suspend fun baixar(): Boolean = withContext(Dispatchers.IO) {
        val agendaTexto = baixarTexto("$base/agenda.json") ?: return@withContext false
        if (runCatching { json.decodeFromString<Agenda>(agendaTexto) }.isFailure) return@withContext false
        arquivoAgenda.writeText(agendaTexto)
        baixarTexto("$base/seguindo.json")?.let { texto ->
            if (runCatching { json.decodeFromString<Seguindo>(texto) }.isSuccess) arquivoSeguindo.writeText(texto)
        }
        baixarTexto("$base/logos.json")?.let { texto ->
            if (runCatching { json.decodeFromString<Logos>(texto) }.isSuccess) arquivoLogos.writeText(texto)
        }
        baixarImagensDosLogos()
        true
    }

    /** Arquivo local (já reduzido) do logo, se já foi baixado. */
    fun arquivoDoLogo(url: String): File? = File(pastaLogos, nomeArquivo(url)).takeIf { it.exists() && it.length() > 0 }

    /** Logo pequeno para o widget (o Android limita o tamanho das imagens em widgets). */
    fun bitmapDoLogo(url: String?): Bitmap? = url?.let { arquivoDoLogo(it) }?.let { runCatching { BitmapFactory.decodeFile(it.path) }.getOrNull() }

    private fun baixarImagensDosLogos() {
        logosSalvos().values.distinct().forEach { url ->
            val destino = File(pastaLogos, nomeArquivo(url))
            if (destino.exists() && destino.length() > 0) return@forEach
            runCatching {
                val bytes = baixarBytes(url) ?: return@forEach
                val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@forEach
                val reduzido = Bitmap.createScaledBitmap(original, 96, 96 * original.height / original.width.coerceAtLeast(1), true)
                destino.outputStream().use { reduzido.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
    }

    private fun nomeArquivo(url: String): String =
        MessageDigest.getInstance("SHA-1").digest(url.toByteArray()).joinToString("") { "%02x".format(it) } + ".png"

    private fun ler(f: File): String? = if (f.exists()) runCatching { f.readText() }.getOrNull() else null

    private fun baixarTexto(url: String): String? =
        baixarBytes("$url?t=${System.currentTimeMillis() / 60000}")?.toString(Charsets.UTF_8)

    private fun baixarBytes(url: String): ByteArray? = runCatching {
        val conexao = URL(url).openConnection() as HttpURLConnection
        conexao.connectTimeout = 10_000
        conexao.readTimeout = 15_000
        conexao.useCaches = false
        conexao.setRequestProperty("User-Agent", "Arquibancada-Android")
        try {
            if (conexao.responseCode != 200) null else conexao.inputStream.use { it.readBytes() }
        } finally {
            conexao.disconnect()
        }
    }.getOrNull()
}
