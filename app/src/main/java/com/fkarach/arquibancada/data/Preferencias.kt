package com.fkarach.arquibancada.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Escolhas guardadas só no celular. */
class Preferencias(context: Context) {
    private val p = context.getSharedPreferences("arquibancada", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun servicos(padrao: List<String>): Set<String> =
        p.getStringSet("servicos", null) ?: padrao.toSet().ifEmpty { setOf("tv_aberta", "youtube", "prime_video") }

    fun alternarServico(servico: String, padrao: List<String>) {
        val atual = servicos(padrao).toMutableSet()
        if (!atual.add(servico)) atual.remove(servico)
        p.edit().putStringSet("servicos", atual).apply()
    }

    // ---- Lembretes: escolha manual por evento (null = segue o padrão "meu time") ----

    private fun ligados() = p.getStringSet("lembrete_on", emptySet())!!
    private fun desligados() = p.getStringSet("lembrete_off", emptySet())!!

    fun lembreteManual(id: String): Boolean? = when (id) {
        in desligados() -> false
        in ligados() -> true
        else -> null
    }

    fun definirLembrete(id: String, ativo: Boolean) {
        val on = ligados().toMutableSet()
        val off = desligados().toMutableSet()
        if (ativo) { off.remove(id); on.add(id) } else { on.remove(id); off.add(id) }
        p.edit().putStringSet("lembrete_on", on).putStringSet("lembrete_off", off).apply()
    }

    // ---- Interesses pedidos pelo app, valendo antes da próxima atualização ----

    fun seguirPendentes(): List<Seguido> =
        p.getString("seguir_pendentes", null)?.let { runCatching { json.decodeFromString<List<Seguido>>(it) }.getOrNull() } ?: emptyList()

    fun adicionarPendente(s: Seguido) {
        val lista = seguirPendentes().filter { it.chave != s.chave } + s
        p.edit().putString("seguir_pendentes", json.encodeToString(lista)).apply()
    }

    fun removerPendente(chave: String) {
        p.edit().putString("seguir_pendentes", json.encodeToString(seguirPendentes().filter { it.chave != chave })).apply()
    }

    fun niveisLocais(): Map<String, String> =
        p.getString("niveis_locais", null)?.let { runCatching { json.decodeFromString<Map<String, String>>(it) }.getOrNull() } ?: emptyMap()

    fun definirNivelLocal(chave: String, nivel: String?) {
        val m = niveisLocais().toMutableMap()
        if (nivel == null) m.remove(chave) else m[chave] = nivel
        p.edit().putString("niveis_locais", json.encodeToString(m)).apply()
    }

    fun removidosLocais(): Set<String> = p.getStringSet("removidos_locais", emptySet())!!

    fun marcarRemovido(chave: String) {
        p.edit().putStringSet("removidos_locais", removidosLocais() + chave).apply()
    }

    /** Depois que o GitHub confirma, limpa o que já está refletido no arquivo oficial. */
    fun limparConfirmados(servidor: List<Seguido>) {
        val chaves = servidor.associateBy { it.chave }
        seguirPendentes().filter { it.chave in chaves }.forEach { removerPendente(it.chave) }
        niveisLocais().filter { (k, v) -> chaves[k]?.nivel == v }.keys.forEach { definirNivelLocal(it, null) }
        val ainda = removidosLocais().filter { it in chaves }.toSet()
        p.edit().putStringSet("removidos_locais", ainda).apply()
    }

    var soDaPraVer: Boolean
        get() = p.getBoolean("so_da_pra_ver", false)
        set(v) { p.edit().putBoolean("so_da_pra_ver", v).apply() }

    var ultimaFalhaDeRede: Boolean
        get() = p.getBoolean("falha_rede", false)
        set(v) { p.edit().putBoolean("falha_rede", v).apply() }
}
