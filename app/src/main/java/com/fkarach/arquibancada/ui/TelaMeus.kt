package com.fkarach.arquibancada.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fkarach.arquibancada.AppViewModel
import com.fkarach.arquibancada.Estado
import com.fkarach.arquibancada.data.Seguido
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
    val partes = nome.split(" ").filter { it.isNotBlank() && it.first().isLetterOrDigit() }
    return when {
        partes.size >= 3 -> partes.take(3).joinToString("") { it.first().uppercase() }
        else -> nome.replace(" ", "").take(3).uppercase()
    }
}

fun nivelLegivel(n: String) = when (n) {
    "meu_time" -> "Meu time"
    "silenciar" -> "Silenciado"
    else -> "Normal"
}

private fun tipoLegivel(t: String) = when (t) {
    "atleta" -> "Atleta"
    "campeonato" -> "Campeonato"
    "modalidade" -> "Modalidade"
    "tema" -> "Tema"
    else -> "Time"
}

private fun subtitulo(s: Seguido): String {
    val esporte = s.modalidade?.let { categoriaDe(it).rotulo } ?: categoriaDe(s.esporte).rotulo
    return listOf(tipoLegivel(s.tipo), if (s.tipo == "modalidade" || s.esporte == "varios") null else esporte, s.detalhe?.takeIf { s.tipo != "tema" })
        .filterNotNull().joinToString(" · ")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TelaMeus(estado: Estado, vm: AppViewModel) {
    val perfil = estado.perfil
    val todos = perfil?.todos.orEmpty()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("MEUS", style = Tipo.appTitle.copy(color = Cor.ink))
            Chip("+ Seguir", ligado = true, onClick = { vm.abrirSeguir(true) })
        }

        listOf("meu_time" to "Meu time", "normal" to "Acompanho", "silenciar" to "Silenciados").forEach { (nivel, titulo) ->
            val doNivel = todos.filter { it.nivel == nivel }
            if (doNivel.isNotEmpty()) {
                Secao(titulo)
                doNivel.forEach { s ->
                    LinhaInteresse(s, estado, pendente = perfil?.ehPendente(s) == true,
                        editando = estado.editandoInteresse == s.chave, vm = vm)
                    Divisor()
                }
                Espaco(22)
            }
        }
        if (todos.isEmpty()) {
            Text("Você ainda não segue nada. Toque em + Seguir.", style = Tipo.body.copy(color = Cor.inkMuted),
                modifier = Modifier.padding(bottom = 22.dp))
        }

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
        LinhaInfo("Lembrete ligado por padrão", "Meu time")
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LinhaInteresse(s: Seguido, estado: Estado, pendente: Boolean, editando: Boolean, vm: AppViewModel) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val logo = estado.logos[s.nome] ?: estado.logos.entries.firstOrNull { s.nome.endsWith(" ${it.key}") }?.value
            Escudo(s.nome, logo, 40.dp, monograma(s.nome))
            Column(Modifier.weight(1f)) {
                Text(s.nome, style = Tipo.event.copy(fontSize = 15.sp, color = Cor.ink))
                Text(
                    if (pendente) "Entra na próxima atualização" else subtitulo(s),
                    style = Tipo.meta.copy(color = Cor.inkSubtle, fontStyle = if (pendente) FontStyle.Italic else FontStyle.Normal),
                    maxLines = 2,
                )
            }
            Text(
                nivelLegivel(s.nivel) + "  ▾",
                style = Tipo.channel.copy(color = Cor.ink),
                modifier = Modifier.clip(RoundedCornerShape(17.dp)).border(1.dp, Cor.borderStrong, RoundedCornerShape(17.dp))
                    .clickable(role = Role.Button) { vm.editarInteresse(if (editando) null else s.chave) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        if (editando) {
            FlowRow(Modifier.padding(start = 52.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("meu_time", "normal", "silenciar").filter { it != s.nivel }.forEach { n ->
                    Chip(nivelLegivel(n).replace("Silenciado", "Silenciar"), ligado = false, onClick = { vm.mudarNivel(s, n) })
                }
                Text("Parar de seguir", style = Tipo.channel.copy(color = Cor.inkMuted, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.clip(RoundedCornerShape(18.dp)).clickable(role = Role.Button) { vm.pararDeSeguir(s) }
                        .padding(horizontal = 12.dp, vertical = 9.dp))
            }
        }
    }
}

@Composable
fun Secao(titulo: String) {
    Text(titulo.uppercase(), style = Tipo.section.copy(fontSize = 18.sp, color = Cor.inkMuted), modifier = Modifier.padding(bottom = 6.dp))
}

@Composable
private fun LinhaInfo(rotulo: String, valor: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(rotulo, style = Tipo.body.copy(color = Cor.ink))
        Text(valor, style = Tipo.body.copy(color = Cor.ink, fontWeight = FontWeight.Bold))
    }
}
