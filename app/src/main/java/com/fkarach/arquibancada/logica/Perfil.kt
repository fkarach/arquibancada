package com.fkarach.arquibancada.logica

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.fkarach.arquibancada.data.Evento
import com.fkarach.arquibancada.data.Preferencias
import com.fkarach.arquibancada.data.Repositorio
import com.fkarach.arquibancada.data.Seguido

/**
 * Os interesses como a pessoa vê agora: o que está no GitHub, mais o que ela acabou de
 * pedir pelo app (pendente) e as mudanças de nível feitas no celular (valem na hora).
 */
class Perfil(private val servidor: List<Seguido>, private val prefs: Preferencias) {

    val pendentes: List<Seguido> = prefs.seguirPendentes().filter { p -> servidor.none { it.chave == p.chave } }
    private val niveis: Map<String, String> = prefs.niveisLocais()

    /** Lista para a tela Meus: servidor (sem os removidos) + pendentes. */
    val todos: List<Seguido> =
        servidor.filter { it.chave !in prefs.removidosLocais() }.map { s -> niveis[s.chave]?.let { s.copy(nivel = it) } ?: s } + pendentes

    fun ehPendente(s: Seguido) = pendentes.any { it.chave == s.chave }

    private fun nivel(id: String): String? = todos.firstOrNull { it.chave == id }?.nivel

    fun meuTime(e: Evento): Boolean =
        if (e.interesses.isEmpty()) e.meuTime else e.interesses.any { nivel(it) == "meu_time" }

    /** Some da agenda se algum interesse do evento foi silenciado e nenhum é "meu time". */
    fun silenciado(e: Evento): Boolean =
        e.interesses.any { nivel(it) == "silenciar" } && !meuTime(e)

    fun lembreteAtivo(e: Evento): Boolean = prefs.lembreteManual(e.id) ?: meuTime(e)

    companion object {
        fun carregar(ctx: Context): Perfil =
            Perfil(Repositorio(ctx).seguindoSalvo()?.seguindo.orEmpty(), Preferencias(ctx))

        private const val NOVA_ISSUE = "https://github.com/fkarach/arquibancada/issues/new"

        /** Abre no navegador o pedido já preenchido; a automação do GitHub aplica ao ser criado. */
        fun abrirPedido(ctx: Context, titulo: String, linhas: List<Pair<String, String?>>) {
            val corpo = linhas.filter { !it.second.isNullOrBlank() }.joinToString("\n") { "${it.first}: ${it.second}" } +
                "\n\n_Pedido feito pelo app Arquibancada. Toque em Create/Criar para confirmar._"
            val url = Uri.parse(NOVA_ISSUE).buildUpon()
                .appendQueryParameter("title", "Arquibancada: $titulo")
                .appendQueryParameter("body", corpo)
                .build()
            ctx.startActivity(Intent(Intent.ACTION_VIEW, url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
