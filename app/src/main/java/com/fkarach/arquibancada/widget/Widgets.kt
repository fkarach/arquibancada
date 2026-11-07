package com.fkarach.arquibancada.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.fkarach.arquibancada.MainActivity
import com.fkarach.arquibancada.data.Agenda
import com.fkarach.arquibancada.data.Evento
import com.fkarach.arquibancada.data.Preferencias
import com.fkarach.arquibancada.data.Repositorio
import com.fkarach.arquibancada.logica.BRASILIA
import com.fkarach.arquibancada.logica.aConfirmar
import com.fkarach.arquibancada.logica.aoVivo
import com.fkarach.arquibancada.logica.canalPrincipal
import com.fkarach.arquibancada.logica.daPraVer
import com.fkarach.arquibancada.logica.gratis
import com.fkarach.arquibancada.logica.hora
import com.fkarach.arquibancada.logica.inicioZoned
import com.fkarach.arquibancada.logica.rotuloDiaCurto
import com.fkarach.arquibancada.logica.terminou
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZonedDateTime

private val GROUND = Color(0xFF0B1712)
private val ACCENT = Color(0xFFFF7A59)
private val INK = Color(0xFFEEF3EE)
private val MUTED = Color(0xFFB7C7BC)
private val PENDENTE = Color(0xFF13241B)
private val CONDENSADA = FontFamily("sans-serif-condensed")

private fun cor(c: Color) = ColorProvider(c)

/** Tudo o que os widgets precisam, calculado a partir da agenda salva no celular. */
private data class DadosWidget(
    val agora: ZonedDateTime,
    val servicos: Set<String>,
    val destaque: Evento?,
    val proximos: List<Evento>,
    val desatualizada: Boolean,
    val atualizadoEm: String?,
)

private fun carregar(ctx: Context): DadosWidget {
    val repo = Repositorio(ctx)
    val agenda: Agenda? = repo.agendaSalva()
    val servicos = Preferencias(ctx).servicos(repo.seguindoSalvo()?.servicos.orEmpty())
    val agora = ZonedDateTime.now(BRASILIA)
    val eventos = agenda?.eventos.orEmpty().filter { !it.terminou(agora) && it.situacao != "adiado" }.sortedBy { it.inicioZoned() }
    val destaque = eventos.firstOrNull { it.aoVivo(agora) && it.daPraVer(servicos) }
        ?: eventos.firstOrNull { it.horarioDefinido && it.daPraVer(servicos) && it.inicioZoned().isAfter(agora) }
    val proximos = eventos.filter { it.id != destaque?.id && !it.aoVivo(agora) }.take(3)
    val desatualizada = agenda?.atualizadoEm?.let {
        runCatching { Duration.between(OffsetDateTime.parse(it).toInstant(), agora.toInstant()).toDays() > 4 }.getOrDefault(false)
    } ?: true
    return DadosWidget(agora, servicos, destaque, proximos, desatualizada, agenda?.atualizadoEm)
}

private fun abrirApp(ctx: Context, e: Evento?) =
    actionStartActivity(Intent(ctx, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        e?.let { putExtra(MainActivity.EXTRA_EVENTO, it.id) }
    })

// ---------- Widget pequeno (2×2): próximo que dá pra ver ----------

class ProximoWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val d = carregar(context)
        provideContent { Pequeno(context, d) }
    }
}

class ProximoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ProximoWidget()
}

@Composable
private fun Pequeno(ctx: Context, d: DadosWidget) {
    val e = d.destaque
    val vivo = e?.aoVivo(d.agora) == true
    val fundo = when { vivo -> ACCENT; e != null && e.aConfirmar() -> PENDENTE; else -> GROUND }
    val tinta = if (vivo) GROUND else INK
    val suave = if (vivo) GROUND else MUTED
    Column(
        GlanceModifier.fillMaxSize().cornerRadius(24.dp).background(fundo).padding(16.dp).clickable(abrirApp(ctx, e)),
        verticalAlignment = Alignment.Vertical.Bottom,
    ) {
        if (d.desatualizada) {
            Text("Agenda desatualizada", style = TextStyle(color = cor(INK), fontSize = 14.sp, fontWeight = FontWeight.Bold))
            Text("Abra o app para baixar de novo.", style = TextStyle(color = cor(MUTED), fontSize = 11.sp))
            return@Column
        }
        if (e == null) {
            Text("Nada que dê pra ver nas próximas horas", style = TextStyle(color = cor(INK), fontSize = 14.sp, fontWeight = FontWeight.Bold))
            return@Column
        }
        val rotulo = when {
            vivo -> "● AO VIVO"
            e.gratis() -> "GRÁTIS · ${rotuloDiaCurto(e, d.agora.toLocalDate()).uppercase()}"
            else -> rotuloDiaCurto(e, d.agora.toLocalDate()).uppercase()
        }
        Text(rotulo, style = TextStyle(color = cor(if (vivo) GROUND else if (e.gratis()) ACCENT else MUTED), fontSize = 11.sp, fontWeight = FontWeight.Bold))
        Spacer(GlanceModifier.defaultWeight())
        if (!vivo) Text(e.hora(), style = TextStyle(color = cor(tinta), fontSize = 34.sp, fontWeight = FontWeight.Bold, fontFamily = CONDENSADA))
        Text(e.titulo.uppercase(), maxLines = 2, style = TextStyle(color = cor(tinta), fontSize = if (vivo) 20.sp else 15.sp, fontWeight = FontWeight.Bold, fontFamily = CONDENSADA))
        Text(
            if (vivo) "${e.canalPrincipal(d.servicos)} · desde ${e.hora()}" else e.canalPrincipal(d.servicos) + if (e.aConfirmar()) " · a confirmar" else "",
            maxLines = 1,
            style = TextStyle(color = cor(suave), fontSize = 12.sp, fontWeight = if (vivo) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (!vivo && e.aConfirmar()) FontStyle.Italic else FontStyle.Normal),
        )
    }
}

// ---------- Widget médio (4×2): destaque + próximos 3 ----------

class AgendaWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val d = carregar(context)
        provideContent { Medio(context, d) }
    }
}

class AgendaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AgendaWidget()
}

@Composable
private fun Medio(ctx: Context, d: DadosWidget) {
    Row(GlanceModifier.fillMaxSize().cornerRadius(24.dp).background(GROUND).padding(horizontal = 18.dp, vertical = 16.dp).clickable(abrirApp(ctx, null))) {
        if (d.desatualizada) {
            Column(GlanceModifier.fillMaxSize()) {
                Text("Agenda desatualizada", style = TextStyle(color = cor(INK), fontSize = 14.sp, fontWeight = FontWeight.Bold))
                Text("Abra o app para tentar de novo; horários e canais podem ter mudado.", style = TextStyle(color = cor(MUTED), fontSize = 12.sp))
            }
            return@Row
        }
        val e = d.destaque
        Column(GlanceModifier.width(132.dp).fillMaxHeight().clickable(abrirApp(ctx, e))) {
            if (e != null) {
                val vivo = e.aoVivo(d.agora)
                Text(
                    when { vivo -> "● AO VIVO"; e.gratis() -> "GRÁTIS"; else -> "PRA VOCÊ VER" },
                    style = TextStyle(color = cor(if (vivo || e.gratis()) ACCENT else MUTED), fontSize = 10.sp, fontWeight = FontWeight.Bold),
                )
                Spacer(GlanceModifier.defaultWeight())
                Text("${rotuloDiaCurto(e, d.agora.toLocalDate())} · ${e.canalPrincipal(d.servicos)}", maxLines = 1,
                    style = TextStyle(color = cor(MUTED), fontSize = 11.sp))
                if (!vivo) Text(e.hora(), style = TextStyle(color = cor(INK), fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = CONDENSADA))
                Text(e.titulo.uppercase(), maxLines = 2, style = TextStyle(color = cor(INK), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = CONDENSADA))
            } else {
                Text("Nada pra ver nas próximas horas", style = TextStyle(color = cor(MUTED), fontSize = 12.sp))
            }
        }
        Spacer(GlanceModifier.width(14.dp))
        Box(GlanceModifier.width(1.dp).fillMaxHeight().background(Color(0xFF22392C))) {}
        Spacer(GlanceModifier.width(14.dp))
        Column(GlanceModifier.defaultWeight().fillMaxHeight()) {
            d.proximos.forEachIndexed { i, p ->
                if (i > 0) Spacer(GlanceModifier.defaultWeight())
                Column(GlanceModifier.fillMaxWidth().clickable(abrirApp(ctx, p))) {
                    Text("${rotuloDiaCurto(p, d.agora.toLocalDate())} · ${p.hora()}", style = TextStyle(color = cor(MUTED), fontSize = 11.sp))
                    Text(p.titulo, maxLines = 1, style = TextStyle(color = cor(INK), fontSize = 13.sp, fontWeight = FontWeight.Bold))
                }
            }
            if (d.proximos.isEmpty()) Text("Sem outros eventos na agenda", style = TextStyle(color = cor(MUTED), fontSize = 12.sp))
            Spacer(GlanceModifier.height(0.dp))
        }
    }
}

object Widgets {
    suspend fun atualizarTodos(ctx: Context) {
        runCatching { ProximoWidget().updateAll(ctx) }
        runCatching { AgendaWidget().updateAll(ctx) }
    }
}
