package com.fkarach.arquibancada.data

import android.content.Context

/** Escolhas guardadas só no celular: serviços assinados e lembretes ligados/desligados. */
class Preferencias(context: Context) {
    private val p = context.getSharedPreferences("arquibancada", Context.MODE_PRIVATE)

    fun servicos(padrao: List<String>): Set<String> =
        p.getStringSet("servicos", null) ?: padrao.toSet().ifEmpty { setOf("tv_aberta", "youtube", "prime_video") }

    fun alternarServico(servico: String, padrao: List<String>) {
        val atual = servicos(padrao).toMutableSet()
        if (!atual.add(servico)) atual.remove(servico)
        p.edit().putStringSet("servicos", atual).apply()
    }

    private fun ligados() = p.getStringSet("lembrete_on", emptySet())!!
    private fun desligados() = p.getStringSet("lembrete_off", emptySet())!!

    fun lembreteAtivo(e: Evento): Boolean =
        when {
            e.id in desligados() -> false
            e.id in ligados() -> true
            else -> e.meuTime
        }

    fun alternarLembrete(e: Evento) {
        val on = ligados().toMutableSet()
        val off = desligados().toMutableSet()
        if (lembreteAtivo(e)) { on.remove(e.id); off.add(e.id) } else { off.remove(e.id); on.add(e.id) }
        p.edit().putStringSet("lembrete_on", on).putStringSet("lembrete_off", off).apply()
    }

    var soDaPraVer: Boolean
        get() = p.getBoolean("so_da_pra_ver", false)
        set(v) { p.edit().putBoolean("so_da_pra_ver", v).apply() }

    var ultimaFalhaDeRede: Boolean
        get() = p.getBoolean("falha_rede", false)
        set(v) { p.edit().putBoolean("falha_rede", v).apply() }
}
