package com.fkarach.arquibancada.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fkarach.arquibancada.data.Evento
import com.fkarach.arquibancada.logica.Selo
import com.fkarach.arquibancada.logica.canalPrincipal
import com.fkarach.arquibancada.logica.hora
import com.fkarach.arquibancada.logica.linhaMeta
import com.fkarach.arquibancada.logica.noSeuServico
import com.fkarach.arquibancada.logica.nomeCurtoServico
import com.fkarach.arquibancada.logica.aConfirmar
import com.fkarach.arquibancada.logica.selo

// ---------- Selos ----------

@Composable
fun SeloGratis(grande: Boolean = false) {
    Text(
        "GRÁTIS",
        style = Tipo.badge.copy(fontSize = if (grande) 11.sp else 10.sp, color = Cor.onAccent),
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Cor.accent).padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

@Composable
fun SeloNoSeu(servico: String?, grande: Boolean = false) {
    Text(
        "NO SEU ${nomeCurtoServico(servico)}",
        style = Tipo.badge.copy(fontSize = if (grande) 11.sp else 10.sp, color = Cor.ink),
        modifier = Modifier.border(1.dp, Cor.inkMuted, RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp),
    )
}

@Composable
fun SeloAoVivo(tamanho: Int = 10) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.border(1.dp, Cor.accent, RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(Cor.accent))
        Text("AO VIVO", style = Tipo.badge.copy(fontSize = tamanho.sp, color = Cor.accent, letterSpacing = 0.8.sp))
    }
}

@Composable
fun TextoAConfirmar(texto: String = "a confirmar") {
    Text(texto, style = Tipo.meta.copy(fontStyle = FontStyle.Italic, color = Cor.inkSubtle))
}

@Composable
fun SeloDoEvento(e: Evento, servicos: Set<String>, grande: Boolean = false) {
    when (e.selo(servicos)) {
        Selo.GRATIS -> SeloGratis(grande)
        Selo.NO_SEU -> SeloNoSeu(e.noSeuServico(servicos)?.servico, grande)
        Selo.NENHUM -> Unit
    }
}

// ---------- Ícones desenhados (sem biblioteca de ícones) ----------

@Composable
fun IconeSino(cheio: Boolean, cor: Color, tamanho: Int = 19) {
    Canvas(Modifier.size(tamanho.dp)) {
        val s = size.width / 24f
        val corpo = Path().apply {
            moveTo(6f * s, 8f * s)
            cubicTo(6f * s, 4.7f * s, 8.7f * s, 2f * s, 12f * s, 2f * s)
            cubicTo(15.3f * s, 2f * s, 18f * s, 4.7f * s, 18f * s, 8f * s)
            cubicTo(18f * s, 15f * s, 21f * s, 17f * s, 21f * s, 17f * s)
            lineTo(3f * s, 17f * s)
            cubicTo(3f * s, 17f * s, 6f * s, 15f * s, 6f * s, 8f * s)
            close()
        }
        if (cheio) drawPath(corpo, cor, style = Fill)
        else drawPath(corpo, cor, style = Stroke(width = 1.8f * s, join = StrokeJoin.Round))
        drawLine(cor, Offset(10.3f * s, 21f * s), Offset(13.7f * s, 21f * s), strokeWidth = 1.8f * s, cap = StrokeCap.Round)
    }
}

@Composable
fun IconeVoltar(cor: Color = Cor.ink) {
    Canvas(Modifier.size(24.dp)) {
        val s = size.width / 24f
        val p = Path().apply { moveTo(15f * s, 18f * s); lineTo(9f * s, 12f * s); lineTo(15f * s, 6f * s) }
        drawPath(p, cor, style = Stroke(width = 2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun IconeCalendario(cor: Color, mais: Boolean = false, tamanho: Int = 22) {
    Canvas(Modifier.size(tamanho.dp)) {
        val s = size.width / 24f
        val st = Stroke(width = 1.8f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(cor, topLeft = Offset(3f * s, 4f * s), size = androidx.compose.ui.geometry.Size(18f * s, 18f * s),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * s), style = st)
        drawLine(cor, Offset(16f * s, 2f * s), Offset(16f * s, 6f * s), strokeWidth = 1.8f * s, cap = StrokeCap.Round)
        drawLine(cor, Offset(8f * s, 2f * s), Offset(8f * s, 6f * s), strokeWidth = 1.8f * s, cap = StrokeCap.Round)
        drawLine(cor, Offset(3f * s, 10f * s), Offset(21f * s, 10f * s), strokeWidth = 1.8f * s)
        if (mais) {
            drawLine(cor, Offset(12f * s, 14f * s), Offset(12f * s, 19f * s), strokeWidth = 1.8f * s, cap = StrokeCap.Round)
            drawLine(cor, Offset(9.5f * s, 16.5f * s), Offset(14.5f * s, 16.5f * s), strokeWidth = 1.8f * s, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun IconeEstrela(cheia: Boolean, cor: Color) {
    Canvas(Modifier.size(24.dp)) {
        val s = size.width / 24f
        val pts = listOf(12f to 2f, 15.09f to 8.26f, 22f to 9.27f, 17f to 14.14f, 18.18f to 21.02f, 12f to 17.77f,
            5.82f to 21.02f, 7f to 14.14f, 2f to 9.27f, 8.91f to 8.26f)
        val p = Path().apply {
            moveTo(pts[0].first * s, pts[0].second * s)
            pts.drop(1).forEach { lineTo(it.first * s, it.second * s) }
            close()
        }
        if (cheia) drawPath(p, cor, style = Fill) else drawPath(p, cor, style = Stroke(width = 1.8f * s, join = StrokeJoin.Round))
    }
}

// ---------- Sino de lembrete ----------

@Composable
fun BotaoSino(ativo: Boolean, onClick: () -> Unit) {
    val desc = if (ativo) "Lembrete ativado" else "Ativar lembrete"
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .then(if (ativo) Modifier.background(Cor.raised) else Modifier.border(1.dp, Cor.borderStrong, CircleShape))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = desc },
    ) {
        IconeSino(cheio = ativo, cor = if (ativo) Cor.ink else Cor.inkSubtle)
    }
}

// ---------- Bolinha de esporte ----------

enum class EstadoBolinha { AO_VIVO, HOJE, APAGADA }

@Composable
fun Bolinha(sigla: String, rotulo: String, estado: EstadoBolinha, selecionada: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.width(56.dp).clickable(role = Role.Button, onClick = onClick),
    ) {
        if (selecionada) {
            Box(Modifier.size(54.dp).clip(CircleShape).background(Cor.inverse), contentAlignment = Alignment.Center) {
                Text(sigla, style = Tipo.timeM.copy(fontSize = 15.sp, color = Cor.onInverse))
            }
        } else {
            val (borda, larg) = when (estado) {
                EstadoBolinha.AO_VIVO -> Cor.accent to 2.dp
                EstadoBolinha.HOJE -> Cor.ink to 2.dp
                EstadoBolinha.APAGADA -> Cor.borderStrong to 1.dp
            }
            Box(Modifier.size(54.dp).border(BorderStroke(larg, borda), CircleShape).padding(3.dp + (if (larg == 1.dp) 1.dp else 0.dp)),
                contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(46.dp).clip(CircleShape).background(if (estado == EstadoBolinha.APAGADA) Cor.surface else Cor.raised),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(sigla, style = Tipo.timeM.copy(fontSize = 15.sp, color = if (estado == EstadoBolinha.APAGADA) Cor.inkMuted else Cor.ink))
                }
            }
        }
        Text(rotulo, style = Tipo.meta.copy(fontSize = 11.sp, color = Cor.inkSubtle), maxLines = 1)
    }
}

// ---------- Linha de evento ----------

@Composable
fun LinhaEvento(e: Evento, servicos: Set<String>, lembrete: Boolean, aoVivo: Boolean, onSino: () -> Unit, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
    ) {
        Text(e.hora(), style = Tipo.timeM.copy(color = Cor.ink), modifier = Modifier.width(52.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(e.titulo, style = Tipo.event.copy(color = Cor.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(e.linhaMeta(), style = Tipo.meta.copy(color = Cor.inkSubtle), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 2.dp)) {
                if (aoVivo) SeloAoVivo()
                if (e.situacao == "adiado") Text("ADIADO", style = Tipo.badge.copy(color = Cor.ink))
                SeloDoEvento(e, servicos)
                Text(e.canalPrincipal(servicos), style = Tipo.channel.copy(color = Cor.ink), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false))
                if (e.aConfirmar()) TextoAConfirmar()
            }
        }
        BotaoSino(lembrete, onSino)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Cor.raised))
}

// ---------- Banners ----------

@Composable
fun BannerMudanca(titulo: String, texto: String) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Cor.inverse).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(titulo, style = Tipo.event.copy(fontSize = 14.sp, color = Cor.onInverse))
        Text(texto, style = Tipo.body.copy(fontSize = 13.sp, lineHeight = 18.sp, color = Cor.onInverse))
    }
}

@Composable
fun BannerOffline(texto: String, onTentar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Cor.raised)
            .border(1.dp, Cor.borderStrong, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Sem internet", style = Tipo.event.copy(fontSize = 14.sp, color = Cor.ink))
            Text(texto, style = Tipo.body.copy(fontSize = 13.sp, lineHeight = 18.sp, color = Cor.inkMuted))
        }
        Chip("Tentar", ligado = false, onClick = onTentar)
    }
}

// ---------- Botões ----------

@Composable
fun Chip(texto: String, ligado: Boolean, onClick: () -> Unit, comX: Boolean = false) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .then(if (ligado) Modifier.background(Cor.accent) else Modifier.background(Cor.surface).border(1.dp, Cor.borderStrong, RoundedCornerShape(18.dp)))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
    ) {
        Text(
            if (comX && ligado) "$texto  ×" else texto,
            style = Tipo.channel.copy(color = if (ligado) Cor.onAccent else Cor.ink, fontWeight = if (ligado) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.SemiBold),
        )
    }
}

@Composable
fun BotaoPrimario(texto: String, modifier: Modifier = Modifier, icone: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    Row(
        modifier = modifier.height(52.dp).clip(RoundedCornerShape(26.dp)).background(Cor.accent).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        icone?.invoke()
        Text(texto, style = Tipo.event.copy(fontSize = 15.sp, color = Cor.onAccent))
    }
}

@Composable
fun BotaoSecundario(texto: String, modifier: Modifier = Modifier, icone: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    Row(
        modifier = modifier.height(52.dp).clip(RoundedCornerShape(26.dp)).background(Cor.surface)
            .border(1.dp, Cor.borderStrong, RoundedCornerShape(26.dp)).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        icone?.invoke()
        if (texto.isNotEmpty()) Text(texto, style = Tipo.event.copy(fontSize = 15.sp, color = Cor.ink))
    }
}

@Composable
fun Divisor() = Box(Modifier.fillMaxWidth().height(1.dp).background(Cor.raised))

@Composable
fun Espaco(dp: Int) = Spacer(Modifier.height(dp.dp))
