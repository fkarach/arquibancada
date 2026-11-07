package com.fkarach.arquibancada.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fkarach.arquibancada.AppViewModel
import com.fkarach.arquibancada.Estado
import com.fkarach.arquibancada.data.Evento
import com.fkarach.arquibancada.logica.CATEGORIAS
import com.fkarach.arquibancada.logica.aConfirmar
import com.fkarach.arquibancada.logica.aoVivo
import com.fkarach.arquibancada.logica.canalPrincipal
import com.fkarach.arquibancada.logica.categoriaDe
import com.fkarach.arquibancada.logica.daPraVer
import com.fkarach.arquibancada.logica.escolherDestaque
import com.fkarach.arquibancada.logica.formatarAtualizacao
import com.fkarach.arquibancada.logica.hora
import com.fkarach.arquibancada.logica.inicioZoned
import com.fkarach.arquibancada.logica.rotuloDia
import com.fkarach.arquibancada.logica.rotuloDiaCurto
import com.fkarach.arquibancada.logica.terminou

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaAgenda(estado: Estado, vm: AppViewModel) {
    val agora = estado.agora
    val hoje = agora.toLocalDate()
    val todos = estado.agenda?.eventos.orEmpty().filterNot { it.terminou(agora) }.sortedBy { it.inicioZoned() }

    var visiveis = todos
    estado.filtroEsporte?.let { f -> visiveis = visiveis.filter { it.esporte == f } }
    if (estado.soDaPraVer) visiveis = visiveis.filter { it.daPraVer(estado.servicos) }

    val destaque = escolherDestaque(visiveis, estado.servicos, agora)
    val lista = visiveis.filter { it.id != destaque?.id }
    val porDia = lista.groupBy { it.inicioZoned().toLocalDate() }.toSortedMap()

    // Bolinhas: só esportes que existem na agenda; ao vivo primeiro, depois hoje, depois o resto.
    val presentes = CATEGORIAS.filter { c -> todos.any { it.esporte == c.chave } } +
        todos.map { categoriaDe(it.esporte) }.distinctBy { it.chave }.filter { c -> CATEGORIAS.none { it.chave == c.chave } }
    val bolinhas = presentes.map { c ->
        val doEsporte = todos.filter { it.esporte == c.chave }
        val estadoB = when {
            doEsporte.any { it.aoVivo(agora) } -> EstadoBolinha.AO_VIVO
            doEsporte.any { it.inicioZoned().toLocalDate() == hoje } -> EstadoBolinha.HOJE
            else -> EstadoBolinha.APAGADA
        }
        c to estadoB
    }.sortedBy { it.second.ordinal }

    PullToRefreshBox(isRefreshing = estado.atualizando && !estado.carregando, onRefresh = { vm.atualizar() }, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("ARQUIBANCADA", style = Tipo.appTitle.copy(color = Cor.ink))
                    Chip("Dá pra ver", ligado = estado.soDaPraVer, comX = true, onClick = { vm.alternarDaPraVer() })
                }
            }
            item {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 8.dp, bottom = 14.dp),
                ) {
                    items(bolinhas, key = { it.first.chave }) { (c, eb) ->
                        val rotulo = if (eb == EstadoBolinha.AO_VIVO) "Ao vivo" else c.rotulo
                        Bolinha(c.sigla, rotulo, eb, selecionada = estado.filtroEsporte == c.chave) { vm.filtrarEsporte(c.chave) }
                    }
                }
            }
            if (estado.offline && estado.agenda != null) {
                item {
                    Box(Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp)) {
                        BannerOffline("Mostrando a agenda salva (${formatarAtualizacao(estado.agenda?.atualizadoEm)}). Canais podem ter mudado.") { vm.atualizar() }
                    }
                }
            }
            when {
                estado.carregando -> item { Carregando() }
                estado.agenda == null -> item { Vazio("Sem agenda ainda", "Não consegui baixar a agenda. Confira a internet e puxe a tela para baixo para tentar de novo.") }
                visiveis.isEmpty() -> item {
                    val nome = estado.filtroEsporte?.let { categoriaDe(it).rotulo } ?: "evento"
                    Vazio(
                        if (estado.soDaPraVer) "Nada de $nome pra você ver ainda" else "Nada de $nome nos próximos 30 dias",
                        if (estado.soDaPraVer) "Os eventos com canal definido estão em serviços que você não marcou em Meus. Canais costumam ser confirmados na semana do jogo."
                        else "A próxima atualização da agenda pode trazer novidades.",
                    )
                }
                else -> {
                    if (destaque != null) item {
                        Box(Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
                            CartaoDestaque(destaque, estado) { vm.abrir(destaque.id) }
                        }
                    }
                    porDia.forEach { (dia, eventos) ->
                        item(key = "dia-$dia") {
                            val (titulo, sub) = rotuloDia(dia, hoje)
                            Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 2.dp), verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(titulo.uppercase(), style = Tipo.section.copy(color = Cor.ink))
                                Text(sub, style = Tipo.meta.copy(fontSize = 13.sp, color = Cor.inkSubtle))
                            }
                        }
                        items(eventos, key = { it.id }) { e ->
                            Box(Modifier.padding(horizontal = 20.dp)) {
                                LinhaEvento(
                                    e, estado.servicos,
                                    lembrete = estado.versaoLembretes.let { vm.lembreteAtivo(e) },
                                    aoVivo = e.aoVivo(agora),
                                    onSino = { vm.alternarLembrete(e) },
                                    onClick = { vm.abrir(e.id) },
                                )
                            }
                        }
                    }
                    item {
                        Text(
                            "Agenda atualizada ${formatarAtualizacao(estado.agenda?.atualizadoEm)}",
                            style = Tipo.meta.copy(color = Cor.inkSubtle),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CartaoDestaque(e: Evento, estado: Estado, onClick: () -> Unit) {
    val agora = estado.agora
    val vivo = e.aoVivo(agora)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Cor.surface)
            .border(1.dp, Cor.border, RoundedCornerShape(18.dp)).clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            if (vivo) SeloAoVivo(tamanho = 12)
            else Text("PRÓXIMO GRANDE · ${rotuloDiaCurto(e, agora.toLocalDate()).uppercase()}", style = Tipo.eyebrow.copy(color = Cor.inkSubtle))
            SeloDoEvento(e, estado.servicos, grande = true)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(e.titulo.uppercase(), style = Tipo.displayL.copy(color = Cor.ink), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                if (vivo) "${e.competicao} · começou às ${e.hora()}" else e.competicao,
                style = Tipo.body.copy(fontSize = 14.sp, color = Cor.inkMuted),
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Cor.border))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!vivo) Text(e.hora(), style = Tipo.timeL.copy(color = Cor.ink))
                Text(e.canalPrincipal(estado.servicos), style = Tipo.event.copy(fontSize = 18.sp, color = Cor.ink))
            }
            if (e.aConfirmar()) TextoAConfirmar() else Text("canal confirmado", style = Tipo.meta.copy(color = Cor.inkSubtle))
        }
    }
}

@Composable
private fun Carregando() {
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.fillMaxWidth().height(168.dp).clip(RoundedCornerShape(18.dp)).background(Cor.surface))
        repeat(4) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp, 20.dp).clip(RoundedCornerShape(4.dp)).background(Cor.surface))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.fillMaxWidth(0.7f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(Cor.surface))
                    Box(Modifier.fillMaxWidth(0.45f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(Cor.surface))
                }
                Box(Modifier.size(44.dp).clip(CircleShape).background(Cor.surface))
            }
        }
        Text("Baixando a agenda dos próximos 30 dias…", style = Tipo.meta.copy(fontSize = 13.sp, color = Cor.inkSubtle),
            modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun Vazio(titulo: String, texto: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 60.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(titulo.uppercase(), style = Tipo.displayL.copy(color = Cor.ink))
        Text(texto, style = Tipo.body.copy(color = Cor.inkMuted))
    }
}
