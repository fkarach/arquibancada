package com.fkarach.arquibancada.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Lê a agenda publicada no GitHub e guarda a última cópia para funcionar sem internet. */
class Repositorio(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val base = "https://raw.githubusercontent.com/fkarach/arquibancada/main/dados"

    private val arquivoAgenda get() = File(context.filesDir, "agenda.json")
    private val arquivoSeguindo get() = File(context.filesDir, "seguindo.json")

    fun agendaSalva(): Agenda? = ler(arquivoAgenda)?.let { runCatching { json.decodeFromString<Agenda>(it) }.getOrNull() }

    fun seguindoSalvo(): Seguindo? = ler(arquivoSeguindo)?.let { runCatching { json.decodeFromString<Seguindo>(it) }.getOrNull() }

    /** Baixa os dois arquivos. Devolve true se a agenda veio da internet. */
    suspend fun baixar(): Boolean = withContext(Dispatchers.IO) {
        val agendaTexto = baixarTexto("$base/agenda.json") ?: return@withContext false
        if (runCatching { json.decodeFromString<Agenda>(agendaTexto) }.isFailure) return@withContext false
        arquivoAgenda.writeText(agendaTexto)
        baixarTexto("$base/seguindo.json")?.let { texto ->
            if (runCatching { json.decodeFromString<Seguindo>(texto) }.isSuccess) arquivoSeguindo.writeText(texto)
        }
        true
    }

    private fun ler(f: File): String? = if (f.exists()) runCatching { f.readText() }.getOrNull() else null

    private fun baixarTexto(url: String): String? = runCatching {
        val conexao = URL("$url?t=${System.currentTimeMillis() / 60000}").openConnection() as HttpURLConnection
        conexao.connectTimeout = 10_000
        conexao.readTimeout = 15_000
        conexao.useCaches = false
        try {
            if (conexao.responseCode != 200) null
            else conexao.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conexao.disconnect()
        }
    }.getOrNull()
}
