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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fkarach.arquibancada.AppViewModel

private val TIPOS = listOf("time" to "Time", "atleta" to "Atleta", "campeonato" to "Campeonato", "modalidade" to "Modalidade", "tema" to "Tema")
private val ESPORTES = listOf(
    "futebol" to "Futebol", "nfl" to "NFL", "nba" to "NBA", "f1" to "F1", "mma" to "Lutas", "esports" to "eSports",
    "kings_league" to "Kings League", "volei" to "Vôlei", "tenis" to "Tênis", "esqui" to "Esqui", "outro" to "Outro",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TelaSeguir(vm: AppViewModel) {
    var nome by rememberSaveable { mutableStateOf("") }
    var tipo by rememberSaveable { mutableStateOf("time") }
    var esporte by rememberSaveable { mutableStateOf("futebol") }
    var outroEsporte by rememberSaveable { mutableStateOf("") }
    var jogo by rememberSaveable { mutableStateOf("") }
    var detalhe by rememberSaveable { mutableStateOf("") }
    var nivel by rememberSaveable { mutableStateOf("meu_time") }

    val esporteFinal = if (esporte == "outro") outroEsporte.trim() else esporte
    val podeEnviar = nome.isNotBlank() && esporteFinal.isNotBlank() && (esporte != "esports" || jogo.isNotBlank())

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clickable(role = Role.Button) { vm.abrirSeguir(false) }.semantics { contentDescription = "Voltar" },
                contentAlignment = Alignment.Center) { IconeVoltar() }
            Text("SEGUIR", style = Tipo.appTitle.copy(color = Cor.ink))
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Espaco(4)
            Campo("Nome", nome, { nome = it }, "Ex.: FURIA, Seleção Brasileira, Copa do Mundo feminina")

            Grupo("Tipo") {
                TIPOS.forEach { (k, v) -> Chip(v, ligado = tipo == k, onClick = { tipo = k }) }
            }
            Grupo("Esporte") {
                ESPORTES.forEach { (k, v) -> Chip(v, ligado = esporte == k, onClick = { esporte = k }) }
            }
            if (esporte == "outro") Campo("Qual esporte", outroEsporte, { outroEsporte = it }, "Ex.: handebol, surfe, xadrez")
            if (esporte == "esports") Campo("Jogo", jogo, { jogo = it }, "Ex.: CS2, Valorant, LoL")
            Campo("Detalhe (opcional)", detalhe, { detalhe = it }, "Ajuda a achar o certo. Ex.: time feminino, campeonato da Ásia")

            Grupo("Nível") {
                Chip("Meu time", ligado = nivel == "meu_time", onClick = { nivel = "meu_time" })
                Chip("Normal", ligado = nivel == "normal", onClick = { nivel = "normal" })
            }
            Text(
                if (nivel == "meu_time") "Todos os eventos entram, com lembrete ligado." else "Só os eventos que passam na régua de relevância.",
                style = Tipo.meta.copy(fontSize = 13.sp, color = Cor.inkSubtle),
            )
            Text(
                "Ao tocar em Seguir, o GitHub abre com o pedido pronto: toque em Create para confirmar. " +
                    "Aparece em Meus na hora; os eventos chegam na próxima atualização da agenda (até 3 dias).",
                style = Tipo.meta.copy(fontSize = 12.sp, lineHeight = 17.sp, color = Cor.inkSubtle),
            )
            Espaco(8)
        }

        Box(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp)) {
            BotaoPrimario("Seguir", Modifier.fillMaxWidth().alpha(if (podeEnviar) 1f else 0.4f)) {
                if (podeEnviar) {
                    vm.seguir(
                        nome = nome.trim(), tipo = tipo, esporte = esporteFinal,
                        modalidade = jogo.trim().takeIf { esporte == "esports" && it.isNotBlank() },
                        nivel = nivel, detalhe = detalhe.trim().ifBlank { null },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Grupo(titulo: String, conteudo: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(titulo.uppercase(), style = Tipo.eyebrow.copy(color = Cor.inkSubtle))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { conteudo() }
    }
}

@Composable
private fun Campo(rotulo: String, valor: String, onChange: (String) -> Unit, dica: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(rotulo.uppercase(), style = Tipo.eyebrow.copy(color = Cor.inkSubtle))
        BasicTextField(
            value = valor,
            onValueChange = { onChange(it.take(80)) },
            singleLine = true,
            textStyle = Tipo.body.copy(fontSize = 16.sp, color = Cor.ink),
            cursorBrush = SolidColor(Cor.accent),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { campo ->
                Box(
                    Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).background(Cor.surface)
                        .border(1.dp, Cor.border, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (valor.isEmpty()) Text(dica, style = Tipo.body.copy(fontSize = 14.sp, color = Cor.inkSubtle), maxLines = 1)
                    campo()
                }
            },
        )
    }
}
