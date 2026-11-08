package com.fkarach.arquibancada

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fkarach.arquibancada.data.Agenda
import com.fkarach.arquibancada.data.Evento
import com.fkarach.arquibancada.data.Preferencias
import com.fkarach.arquibancada.data.Repositorio
import com.fkarach.arquibancada.data.Seguido
import com.fkarach.arquibancada.data.Seguindo
import com.fkarach.arquibancada.lembretes.Lembretes
import com.fkarach.arquibancada.logica.BRASILIA
import com.fkarach.arquibancada.logica.Perfil
import com.fkarach.arquibancada.widget.Widgets
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.time.ZonedDateTime

enum class Aba { AGENDA, MEUS }

data class Estado(
    val agenda: Agenda? = null,
    val seguindo: Seguindo? = null,
    val perfil: Perfil? = null,
    val logos: Map<String, String> = emptyMap(),
    val servicos: Set<String> = emptySet(),
    val carregando: Boolean = true,
    val atualizando: Boolean = false,
    val offline: Boolean = false,
    val filtroEsporte: String? = null,
    val soDaPraVer: Boolean = false,
    val detalheId: String? = null,
    val aba: Aba = Aba.AGENDA,
    val seguindoNovo: Boolean = false,
    val editandoInteresse: String? = null,
    val versaoLembretes: Int = 0,
    val agora: ZonedDateTime = ZonedDateTime.now(BRASILIA),
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repositorio(app)
    val prefs = Preferencias(app)

    private val _estado = MutableStateFlow(Estado())
    val estado: StateFlow<Estado> = _estado

    init {
        val agenda = repo.agendaSalva()
        val seguindo = repo.seguindoSalvo()
        _estado.value = Estado(
            agenda = agenda,
            seguindo = seguindo,
            perfil = Perfil(seguindo?.seguindo.orEmpty(), prefs),
            logos = repo.logosSalvos(),
            servicos = prefs.servicos(seguindo?.servicos.orEmpty()),
            carregando = agenda == null,
            soDaPraVer = prefs.soDaPraVer,
        )
        atualizar()
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                _estado.update { it.copy(agora = ZonedDateTime.now(BRASILIA)) }
            }
        }
    }

    fun atualizar() {
        viewModelScope.launch {
            _estado.update { it.copy(atualizando = true) }
            val ok = repo.baixar()
            prefs.ultimaFalhaDeRede = !ok
            val agenda = repo.agendaSalva()
            val seguindo = repo.seguindoSalvo()
            if (ok) prefs.limparConfirmados(seguindo?.seguindo.orEmpty())
            _estado.update {
                it.copy(
                    agenda = agenda,
                    seguindo = seguindo,
                    perfil = Perfil(seguindo?.seguindo.orEmpty(), prefs),
                    logos = repo.logosSalvos(),
                    servicos = prefs.servicos(seguindo?.servicos.orEmpty()),
                    carregando = false,
                    atualizando = false,
                    offline = !ok,
                    agora = ZonedDateTime.now(BRASILIA),
                )
            }
            depoisDeMudar()
        }
    }

    private fun recarregarPerfil() {
        _estado.update { it.copy(perfil = Perfil(it.seguindo?.seguindo.orEmpty(), prefs), versaoLembretes = it.versaoLembretes + 1) }
        depoisDeMudar()
    }

    private fun depoisDeMudar() {
        val ctx = getApplication<Application>()
        Lembretes.reprogramar(ctx)
        viewModelScope.launch { Widgets.atualizarTodos(ctx) }
    }

    fun lembreteAtivo(e: Evento) = _estado.value.perfil?.lembreteAtivo(e) ?: e.meuTime

    fun alternarLembrete(e: Evento) {
        prefs.definirLembrete(e.id, !lembreteAtivo(e))
        _estado.update { it.copy(versaoLembretes = it.versaoLembretes + 1) }
        depoisDeMudar()
    }

    fun alternarServico(servico: String) {
        prefs.alternarServico(servico, _estado.value.seguindo?.servicos.orEmpty())
        _estado.update { it.copy(servicos = prefs.servicos(it.seguindo?.servicos.orEmpty())) }
        depoisDeMudar()
    }

    // ---------- Interesses ----------

    fun abrirSeguir(aberto: Boolean) = _estado.update { it.copy(seguindoNovo = aberto) }

    fun editarInteresse(chave: String?) = _estado.update { it.copy(editandoInteresse = chave) }

    fun seguir(nome: String, tipo: String, esporte: String, modalidade: String?, nivel: String, detalhe: String?) {
        val id = slug(listOfNotNull(nome, modalidade ?: esporte).joinToString("-"))
        val novo = Seguido(id = id, nome = nome.trim(), tipo = tipo, esporte = slug(esporte).replace("-", "_"),
            modalidade = modalidade?.let { slug(it).replace("-", "_") }, detalhe = detalhe, nivel = nivel)
        prefs.adicionarPendente(novo)
        Perfil.abrirPedido(
            getApplication(), "seguir ${novo.nome}",
            listOf("acao" to "seguir", "id" to novo.id, "nome" to novo.nome, "tipo" to novo.tipo, "esporte" to novo.esporte,
                "modalidade" to novo.modalidade, "nivel" to novo.nivel, "detalhe" to novo.detalhe),
        )
        avisar("Confirme no GitHub tocando em Create. Os eventos entram na próxima atualização.")
        _estado.update { it.copy(seguindoNovo = false) }
        recarregarPerfil()
    }

    fun mudarNivel(s: Seguido, nivel: String) {
        val perfil = _estado.value.perfil
        if (perfil != null && perfil.ehPendente(s)) {
            prefs.adicionarPendente(s.copy(nivel = nivel))
        } else {
            prefs.definirNivelLocal(s.chave, nivel)
            Perfil.abrirPedido(getApplication(), "${s.nome} → $nivel", listOf("acao" to "nivel", "id" to s.chave, "nivel" to nivel))
            avisar("Já vale no app. Toque em Create no GitHub para salvar na agenda.")
        }
        _estado.update { it.copy(editandoInteresse = null) }
        recarregarPerfil()
    }

    fun pararDeSeguir(s: Seguido) {
        val perfil = _estado.value.perfil
        if (perfil != null && perfil.ehPendente(s)) {
            prefs.removerPendente(s.chave)
        } else {
            prefs.marcarRemovido(s.chave)
            Perfil.abrirPedido(getApplication(), "parar de seguir ${s.nome}", listOf("acao" to "parar", "id" to s.chave))
            avisar("Toque em Create no GitHub para confirmar.")
        }
        _estado.update { it.copy(editandoInteresse = null) }
        recarregarPerfil()
    }

    private fun avisar(texto: String) = Toast.makeText(getApplication(), texto, Toast.LENGTH_LONG).show()

    private fun slug(t: String): String =
        Normalizer.normalize(t, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase()
            .replace(Regex("[^a-z0-9]+"), "-").trim('-').take(60).ifBlank { "interesse" }

    // ---------- Navegação ----------

    fun filtrarEsporte(chave: String?) = _estado.update { it.copy(filtroEsporte = if (it.filtroEsporte == chave) null else chave) }

    fun alternarDaPraVer() {
        val novo = !_estado.value.soDaPraVer
        prefs.soDaPraVer = novo
        _estado.update { it.copy(soDaPraVer = novo) }
    }

    fun abrir(id: String?) = _estado.update { it.copy(detalheId = id) }

    fun irPara(aba: Aba) = _estado.update { it.copy(aba = aba, detalheId = null, seguindoNovo = false, editandoInteresse = null) }
}
