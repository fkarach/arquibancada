package com.fkarach.arquibancada.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fkarach.arquibancada.AppViewModel
import com.fkarach.arquibancada.Estado
import com.fkarach.arquibancada.logica.categoriaDe
import com.fkarach.arquibancada.logica.formatarAtualizacao

private val SERVICOS = listOf(
    "tv_aberta" to "TV aberta",
    "youtube" to "YouTube",
    "prime_video" to "Prime Video",
    "globoplay" to "Globoplay / sportv",
    "espn_disney" to "ESPN / Disney+",
    "paramount_plus" to "Paramount+",
    "hbo_max" to "HBO Max",
    "netflix" to "Netflix",
    "league_pass" to "NBA League Pass",
    "nfl_game_pass" to "NFL Game Pass",
)

private fun monograma(nome: String): String {
    val partes = nome.split(" ").filter { it.isNotBlank() }
    return when {
        partes.size >= 3 -> partes.take(3).joinToString("") { it.first().uppercase() }
        else -> nome.replace(" ", "").take(3).uppercase()
    }
}

private fun nivelLegivel(n: String) = when (n) {
    "meu_time" -> "Meu time"
    "silenciar" -> "Silenciado"
    else -> "Normal"
}

private fun modalidadeLegivel(esporte: String, modalidade: String?) = when (modalidade) {
    "league_of_legends" -> "League of Legends"
    "rocket_league" -> "Rocket League"
    null -> categoriaDe(esporte).rotulo
    else -> modalidade.replace("_", " ").replaceFirstChar { it.uppercase() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TelaMeus(estado: Estado, vm: AppViewModel) {
    val seguindo = estado.seguindo
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Text("MEUS", style = Tipo.appTitle.copy(color = Cor.ink), modifier = Modifier.padding(top = 18.dp, bottom = 16.dp))

        Secao("Seguindo")
        seguindo?.seguindo.orEmpty().forEach { s ->
            Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(Cor.raised), contentAlignment = Alignment.Center) {
                    Text(monograma(s.nome), style = Tipo.timeM.copy(fontSize = 14.sp, color = Cor.ink))
                }
                Column(Modifier.weight(1f)) {
                    Text(s.nome, style = Tipo.event.copy(fontSize = 15.sp, color = Cor.ink))
                    Text(categoriaDe(s.esporte).rotulo, style = Tipo.meta.copy(color = Cor.inkSubtle))
                }
                Text(nivelLegivel(s.nivel), style = Tipo.channel.copy(color = Cor.ink),
                    modifier = Modifier.border(1.dp, Cor.borderStrong, RoundedCornerShape(17.dp)).padding(horizontal = 12.dp, vertical = 7.dp))
            }
            Divisor()
        }
        Text(
            "Para seguir alguém novo ou mudar o nível, me peça no chat. A mudança entra na próxima atualização da agenda.",
            style = Tipo.meta.copy(fontSize = 12.sp, lineHeight = 17.sp, color = Cor.inkSubtle),
            modifier = Modifier.padding(top = 8.dp, bottom = 22.dp),
        )

        Secao("Modalidades")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            seguindo?.modalidades.orEmpty().filter { it.nivel != "silenciar" }.forEach { m ->
                Text(modalidadeLegivel(m.esporte, m.modalidade), style = Tipo.channel.copy(color = Cor.ink),
                    modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(Cor.surface)
                        .border(1.dp, Cor.borderStrong, RoundedCornerShape(18.dp)).padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }
        Espaco(22)

        Secao("Meus serviços")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SERVICOS.forEach { (chave, nome) ->
                Chip(if (chave in estado.servicos) "✓ $nome" else nome, ligado = false, onClick = { vm.alternarServico(chave) })
            }
        }
        Text("Jogos nesses serviços entram no filtro \"Dá pra ver\". TV aberta e YouTube contam como grátis.",
            style = Tipo.meta.copy(fontSize = 12.sp, lineHeight = 17.sp, color = Cor.inkSubtle), modifier = Modifier.padding(top = 8.dp, bottom = 22.dp))

        Secao("Avisos e dados")
        LinhaInfo("Avisar antes do início", "30 min")
        Divisor()
        LinhaInfo("Lembrete ligado por padrão", "Meus times")
        Divisor()
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("Agenda atualizada", style = Tipo.body.copy(color = Cor.ink))
                Text("${formatarAtualizacao(estado.agenda?.atualizadoEm)} · a cada 3 dias", style = Tipo.meta.copy(color = Cor.inkSubtle))
                estado.agenda?.proximaAtualizacao?.let {
                    Text("Próxima: ${formatarAtualizacao(it)}", style = Tipo.meta.copy(color = Cor.inkSubtle))
                }
            }
            Chip(if (estado.atualizando) "Baixando…" else "Atualizar agora", ligado = false, onClick = { vm.atualizar() })
        }
        Espaco(24)
    }
}

@Composable
private fun Secao(titulo: String) {
    Text(titulo.uppercase(), style = Tipo.section.copy(fontSize = 18.sp, color = Cor.inkMuted), modifier = Modifier.padding(bottom = 6.dp))
}

@Composable
private fun LinhaInfo(rotulo: String, valor: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(rotulo, style = Tipo.body.copy(color = Cor.ink))
        Text(valor, style = Tipo.body.copy(color = Cor.ink, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
    }
}
