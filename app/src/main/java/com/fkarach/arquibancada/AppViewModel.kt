package com.fkarach.arquibancada

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fkarach.arquibancada.data.Agenda
import com.fkarach.arquibancada.data.Evento
import com.fkarach.arquibancada.data.Preferencias
import com.fkarach.arquibancada.data.Repositorio
import com.fkarach.arquibancada.data.Seguindo
import com.fkarach.arquibancada.lembretes.Lembretes
import com.fkarach.arquibancada.logica.BRASILIA
import com.fkarach.arquibancada.widget.Widgets
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

enum class Aba { AGENDA, MEUS }

data class Estado(
    val agenda: Agenda? = null,
    val seguindo: Seguindo? = null,
    val logos: Map<String, String> = emptyMap(),
    val servicos: Set<String> = emptySet(),
    val carregando: Boolean = true,
    val atualizando: Boolean = false,
    val offline: Boolean = false,
    val filtroEsporte: String? = null,
    val soDaPraVer: Boolean = false,
    val detalheId: String? = null,
    val aba: Aba = Aba.AGENDA,
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
            _estado.update {
                it.copy(
                    agenda = agenda,
                    seguindo = seguindo,
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

    private fun depoisDeMudar() {
        val ctx = getApplication<Application>()
        Lembretes.reprogramar(ctx)
        viewModelScope.launch { Widgets.atualizarTodos(ctx) }
    }

    fun lembreteAtivo(e: Evento) = prefs.lembreteAtivo(e)

    fun alternarLembrete(e: Evento) {
        prefs.alternarLembrete(e)
        _estado.update { it.copy(versaoLembretes = it.versaoLembretes + 1) }
        depoisDeMudar()
    }

    fun alternarServico(servico: String) {
        prefs.alternarServico(servico, _estado.value.seguindo?.servicos.orEmpty())
        _estado.update { it.copy(servicos = prefs.servicos(it.seguindo?.servicos.orEmpty())) }
        depoisDeMudar()
    }

    fun filtrarEsporte(chave: String?) = _estado.update { it.copy(filtroEsporte = if (it.filtroEsporte == chave) null else chave) }

    fun alternarDaPraVer() {
        val novo = !_estado.value.soDaPraVer
        prefs.soDaPraVer = novo
        _estado.update { it.copy(soDaPraVer = novo) }
    }

    fun abrir(id: String?) = _estado.update { it.copy(detalheId = id) }

    fun irPara(aba: Aba) = _estado.update { it.copy(aba = aba, detalheId = null) }
}
