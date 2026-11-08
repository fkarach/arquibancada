package com.fkarach.arquibancada.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.fkarach.arquibancada.MainActivity
import com.fkarach.arquibancada.data.Evento
import com.fkarach.arquibancada.data.Preferencias
import com.fkarach.arquibancada.data.Repositorio
import com.fkarach.arquibancada.logica.BRASILIA
import com.fkarach.arquibancada.logica.aConfirmar
import com.fkarach.arquibancada.logica.aoVivo
import com.fkarach.arquibancada.logica.canalPrincipal
import com.fkarach.arquibancada.logica.categoria
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
private val SURFACE = Color(0xFF13241B)
private val RAISED = Color(0xFF1C3326)
private val BORDER = Color(0xFF22392C)
private val ACCENT = Color(0xFFFF7A59)
private val INK = Color(0xFFEEF3EE)
private val MUTED = Color(0xFFB7C7BC)
private val CONDENSADA = FontFamily("sans-serif-condensed")

private fun cor(c: Color) = ColorProvider(c)

/** Tudo o que os widgets precisam, calculado a partir da agenda salva no celular. */
private class DadosWidget(
    val agora: ZonedDateTime,
    val servicos: Set<String>,
    val aoVivo: List<Evento>,
    val destaque: Evento?,
    val proximos: List<Evento>,
    val desatualizada: Boolean,
    val escudos: Map<String, Bitmap>,
)

private fun carregar(ctx: Context): DadosWidget {
    val repo = Repositorio(ctx)
    val agenda = repo.agendaSalva()
    val servicos = Preferencias(ctx).servicos(repo.seguindoSalvo()?.servicos.orEmpty())
    val agora = ZonedDateTime.now(BRASILIA)
    val perfil = com.fkarach.arquibancada.logica.Perfil.carregar(ctx)
    val eventos = agenda?.eventos.orEmpty().filter { !it.terminou(agora) && it.situacao != "adiado" && !perfil.silenciado(it) }.sortedBy { it.inicioZoned() }
    // Ao vivo: mais relevantes primeiro (o que dá pra ver, depois pontos).
    val aoVivo = eventos.filter { it.aoVivo(agora) }
        .sortedWith(compareByDescending<Evento> { it.daPraVer(servicos) }.thenByDescending { it.pontos })
    val destaque = aoVivo.firstOrNull { it.daPraVer(servicos) }
        ?: eventos.firstOrNull { it.horarioDefinido && it.daPraVer(servicos) && it.inicioZoned().isAfter(agora) }
    val proximos = eventos.filter { !it.aoVivo(agora) && it.id != destaque?.id }.take(8)
    val desatualizada = agenda?.atualizadoEm?.let {
        runCatching { Duration.between(OffsetDateTime.parse(it).toInstant(), agora.toInstant()).toDays() > 4 }.getOrDefault(false)
    } ?: true
    val logos = repo.logosSalvos()
    val nomes = (aoVivo + listOfNotNull(destaque) + proximos).flatMap { it.times }.distinct()
    val escudos = nomes.mapNotNull { n -> repo.bitmapDoLogo(logos[n])?.let { n to it } }.toMap()
    return DadosWidget(agora, servicos, aoVivo, destaque, proximos, desatualizada, escudos)
}

private fun abrirApp(ctx: Context, e: Evento?) =
    actionStartActivity(Intent(ctx, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        e?.let { putExtra(MainActivity.EXTRA_EVENTO, it.id) }
    })

/** "UFC Fight Night · Las Vegas" -> "UFC Fight Night": dá contexto a nomes que a pessoa não reconhece. */
private fun Evento.competicaoCurta(): String = competicao.substringBefore(" · ").ifBlank { categoria().rotulo }

// ---------- Escudos no widget ----------

@Composable
private fun EscudoW(d: DadosWidget, nome: String, sigla: String?, tamanho: Dp) {
    val bmp = d.escudos[nome]
    Box(
        GlanceModifier.size(tamanho).cornerRadius(tamanho / 2).background(if (bmp != null) INK else RAISED),
        contentAlignment = Alignment.Center,
    ) {
        if (bmp != null) {
            Image(ImageProvider(bmp), contentDescription = nome, contentScale = ContentScale.Fit,
                modifier = GlanceModifier.size(tamanho * 0.72f))
        } else {
            Text((sigla ?: nome).take(3).uppercase(), style = TextStyle(color = cor(INK), fontSize = (tamanho.value * 0.32f).sp,
                fontWeight = FontWeight.Bold, fontFamily = CONDENSADA))
        }
    }
}

@Composable
private fun MarcaW(d: DadosWidget, e: Evento, tamanho: Dp) {
    if (e.times.size == 2) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EscudoW(d, e.times[0], e.monogramas.getOrNull(0), tamanho)
            Spacer(GlanceModifier.width(3.dp))
            EscudoW(d, e.times[1], e.monogramas.getOrNull(1), tamanho)
        }
    } else {
        EscudoW(d, e.categoria().rotulo, e.categoria().sigla, tamanho)
    }
}

@Composable
private fun Desatualizada() {
    Column {
        Text("Agenda desatualizada", style = TextStyle(color = cor(INK), fontSize = 14.sp, fontWeight = FontWeight.Bold))
        Text("Abra o app para baixar de novo; horários e canais podem ter mudado.", style = TextStyle(color = cor(MUTED), fontSize = 11.sp))
    }
}

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
    val fundo = when { vivo -> ACCENT; e != null && e.aConfirmar() -> SURFACE; else -> GROUND }
    val tinta = if (vivo) GROUND else INK
    val suave = if (vivo) GROUND else MUTED
    Column(GlanceModifier.fillMaxSize().cornerRadius(24.dp).background(fundo).padding(14.dp).clickable(abrirApp(ctx, e))) {
        if (d.desatualizada) { Desatualizada(); return@Column }
        if (e == null) {
            Text("Nada que dê pra ver nas próximas horas", style = TextStyle(color = cor(INK), fontSize = 14.sp, fontWeight = FontWeight.Bold))
            return@Column
        }
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MarcaW(d, e, 26.dp)
            Spacer(GlanceModifier.defaultWeight())
            val rotulo = when { vivo -> "● AO VIVO"; e.gratis() -> "GRÁTIS"; else -> "" }
            Text(rotulo, style = TextStyle(color = cor(if (vivo) GROUND else ACCENT), fontSize = 10.sp, fontWeight = FontWeight.Bold))
        }
        Spacer(GlanceModifier.defaultWeight())
        Text(
            if (vivo) e.competicaoCurta().uppercase() else "${rotuloDiaCurto(e, d.agora.toLocalDate())} · ${e.competicaoCurta()}",
            maxLines = 1, style = TextStyle(color = cor(suave), fontSize = 11.sp),
        )
        if (!vivo) Text(e.hora(), style = TextStyle(color = cor(tinta), fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = CONDENSADA))
        Text(e.titulo.uppercase(), maxLines = 2, style = TextStyle(color = cor(tinta), fontSize = if (vivo) 18.sp else 14.sp,
            fontWeight = FontWeight.Bold, fontFamily = CONDENSADA))
        Text(
            if (vivo) "${e.canalPrincipal(d.servicos)} · desde ${e.hora()}" else e.canalPrincipal(d.servicos) + if (e.aConfirmar()) " · a confirmar" else "",
            maxLines = 1,
            style = TextStyle(color = cor(suave), fontSize = 11.sp, fontWeight = if (vivo) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (!vivo && e.aConfirmar()) FontStyle.Italic else FontStyle.Normal),
        )
    }
}

// ---------- Widget médio/grande: todos os ao vivo + próximos, até onde couber ----------

class AgendaWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val d = carregar(context)
        provideContent { Medio(context, d) }
    }
}

class AgendaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AgendaWidget()
}

private val ALTURA_LINHA = 44.dp

@Composable
private fun LinhaW(ctx: Context, d: DadosWidget, e: Evento, vivo: Boolean) {
    Row(GlanceModifier.fillMaxWidth().height(ALTURA_LINHA).clickable(abrirApp(ctx, e)), verticalAlignment = Alignment.CenterVertically) {
        MarcaW(d, e, 22.dp)
        Spacer(GlanceModifier.width(10.dp))
        Column(GlanceModifier.defaultWeight()) {
            Text(e.titulo, maxLines = 1, style = TextStyle(color = cor(INK), fontSize = 13.sp, fontWeight = FontWeight.Bold))
            Text(
                if (vivo) "${e.competicaoCurta()} · ${e.canalPrincipal(d.servicos)}"
                else "${e.competicaoCurta()} · ${rotuloDiaCurto(e, d.agora.toLocalDate())} ${e.hora()}",
                maxLines = 1, style = TextStyle(color = cor(MUTED), fontSize = 11.sp),
            )
        }
        if (vivo) Text("●", style = TextStyle(color = cor(ACCENT), fontSize = 12.sp))
        else if (e.gratis()) Text("GRÁTIS", style = TextStyle(color = cor(ACCENT), fontSize = 9.sp, fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun Cabecalho(texto: String, corTexto: Color) {
    Text(texto, style = TextStyle(color = cor(corTexto), fontSize = 10.sp, fontWeight = FontWeight.Bold),
        modifier = GlanceModifier.padding(bottom = 2.dp))
}

@Composable
private fun Medio(ctx: Context, d: DadosWidget) {
    val altura = LocalSize.current.height
    // Linhas que cabem: altura menos padding (32) e um cabeçalho (16) por seção.
    val vagas = (((altura - 32.dp - 16.dp) / ALTURA_LINHA).toInt()).coerceAtLeast(2)

    Column(GlanceModifier.fillMaxSize().cornerRadius(24.dp).background(GROUND).padding(horizontal = 16.dp, vertical = 14.dp)
        .clickable(abrirApp(ctx, null))) {
        if (d.desatualizada) { Desatualizada(); return@Column }

        val vivoForaDoDestaque = d.aoVivo.size == 1 && d.destaque?.id != d.aoVivo[0].id
        if (d.aoVivo.size >= 2 || vivoForaDoDestaque) {
            // Vários ao vivo: lista em tela cheia, ao vivo primeiro, depois o que vem.
            val vivos = d.aoVivo.take(vagas)
            Cabecalho("AO VIVO AGORA · ${d.aoVivo.size}", ACCENT)
            vivos.forEach { LinhaW(ctx, d, it, vivo = true) }
            val sobra = vagas - vivos.size - 1
            if (sobra > 0 && d.proximos.isNotEmpty()) {
                Spacer(GlanceModifier.height(4.dp))
                Cabecalho("DEPOIS", MUTED)
                d.proximos.take(sobra).forEach { LinhaW(ctx, d, it, vivo = false) }
            }
            return@Column
        }

        Row(GlanceModifier.fillMaxSize()) {
            val e = d.destaque
            Column(GlanceModifier.width(136.dp).fillMaxHeight().clickable(abrirApp(ctx, e))) {
                if (e != null) {
                    val vivo = e.aoVivo(d.agora)
                    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        MarcaW(d, e, 28.dp)
                        Spacer(GlanceModifier.defaultWeight())
                        Text(when { vivo -> "● AO VIVO"; e.gratis() -> "GRÁTIS"; else -> "" },
                            style = TextStyle(color = cor(ACCENT), fontSize = 9.sp, fontWeight = FontWeight.Bold))
                    }
                    Spacer(GlanceModifier.defaultWeight())
                    Text(e.competicaoCurta(), maxLines = 1, style = TextStyle(color = cor(MUTED), fontSize = 11.sp))
                    if (!vivo) Text("${rotuloDiaCurto(e, d.agora.toLocalDate())} · ${e.hora()}",
                        style = TextStyle(color = cor(INK), fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = CONDENSADA))
                    Text(e.titulo.uppercase(), maxLines = 2, style = TextStyle(color = cor(INK), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = CONDENSADA))
                    Text(e.canalPrincipal(d.servicos), maxLines = 1, style = TextStyle(color = cor(MUTED), fontSize = 11.sp))
                } else {
                    Text("Nada pra ver nas próximas horas", style = TextStyle(color = cor(MUTED), fontSize = 12.sp))
                }
            }
            Spacer(GlanceModifier.width(14.dp))
            Box(GlanceModifier.width(1.dp).fillMaxHeight().background(BORDER)) {}
            Spacer(GlanceModifier.width(14.dp))
            Column(GlanceModifier.defaultWeight().fillMaxHeight()) {
                Cabecalho("PRÓXIMOS", MUTED)
                d.proximos.take(vagas).forEach { LinhaW(ctx, d, it, vivo = false) }
                if (d.proximos.isEmpty()) Text("Sem outros eventos na agenda", style = TextStyle(color = cor(MUTED), fontSize = 12.sp))
            }
        }
    }
}

object Widgets {
    suspend fun atualizarTodos(ctx: Context) {
        runCatching { ProximoWidget().updateAll(ctx) }
        runCatching { AgendaWidget().updateAll(ctx) }
    }
}
