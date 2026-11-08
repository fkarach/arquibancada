package com.fkarach.arquibancada.logica

import com.fkarach.arquibancada.data.Evento
import com.fkarach.arquibancada.data.Onde
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

val BRASILIA: ZoneId = ZoneId.of("America/Sao_Paulo")
private val PT = Locale.forLanguageTag("pt-BR")

/** Início do evento em Brasília. Para data sem horário, meio-dia do dia (só para ordenar). */
fun Evento.inicioZoned(): ZonedDateTime =
    if (horarioDefinido && inicio.contains("T")) OffsetDateTime.parse(inicio).atZoneSameInstant(BRASILIA)
    else LocalDate.parse(inicio.take(10)).atTime(12, 0).atZone(BRASILIA)

fun Evento.inicioAnteriorZoned(): ZonedDateTime? =
    inicioAnterior?.takeIf { it.contains("T") }?.let { runCatching { OffsetDateTime.parse(it).atZoneSameInstant(BRASILIA) }.getOrNull() }

fun Evento.duracao(): Duration = when (esporte) {
    "futebol" -> Duration.ofMinutes(115)
    "nba" -> Duration.ofMinutes(150)
    "nfl" -> Duration.ofMinutes(200)
    "f1" -> Duration.ofMinutes(120)
    "mma", "esports" -> Duration.ofHours(4)
    else -> Duration.ofHours(3)
}

fun Evento.fim(): ZonedDateTime =
    if (horarioDefinido) inicioZoned().plus(duracao()) else LocalDate.parse(inicio.take(10)).plusDays(1).atStartOfDay(BRASILIA)

fun Evento.aoVivo(agora: ZonedDateTime): Boolean =
    horarioDefinido && situacao != "adiado" && !agora.isBefore(inicioZoned()) && agora.isBefore(fim())

fun Evento.terminou(agora: ZonedDateTime): Boolean = !agora.isBefore(fim())

fun Evento.aConfirmar(): Boolean = statusCanal != "confirmado"

/** Ordena: grátis primeiro, depois serviços da pessoa, depois o resto. */
fun Evento.ondeOrdenado(servicos: Set<String>): List<Onde> =
    ondeAssistir.sortedBy { o -> when { o.gratis -> 0; o.servico != null && o.servico in servicos -> 1; else -> 2 } }

fun Evento.gratis(): Boolean = ondeAssistir.any { it.gratis }

fun Evento.noSeuServico(servicos: Set<String>): Onde? =
    ondeAssistir.firstOrNull { !it.gratis && it.servico != null && it.servico in servicos }

fun Evento.daPraVer(servicos: Set<String>): Boolean = gratis() || noSeuServico(servicos) != null

fun Evento.canalPrincipal(servicos: Set<String>): String =
    ondeOrdenado(servicos).firstOrNull()?.canal ?: "Canal"

enum class Selo { GRATIS, NO_SEU, NENHUM }

fun Evento.selo(servicos: Set<String>): Selo = when {
    gratis() -> Selo.GRATIS
    noSeuServico(servicos) != null -> Selo.NO_SEU
    else -> Selo.NENHUM
}

fun nomeCurtoServico(servico: String?): String = when (servico) {
    "prime_video" -> "PRIME"
    "paramount_plus" -> "PARAMOUNT+"
    "globoplay" -> "GLOBOPLAY"
    "espn_disney" -> "DISNEY+"
    "hbo_max" -> "HBO MAX"
    "netflix" -> "NETFLIX"
    "league_pass" -> "LEAGUE PASS"
    "nfl_game_pass" -> "GAME PASS"
    else -> "PLANO"
}

/** Categoria da bolinha de esporte. */
data class Categoria(val chave: String, val sigla: String, val rotulo: String)

val CATEGORIAS = listOf(
    Categoria("futebol", "FUT", "Futebol"),
    Categoria("nfl", "NFL", "NFL"),
    Categoria("nba", "NBA", "NBA"),
    Categoria("f1", "F1", "F1"),
    Categoria("mma", "MMA", "Lutas"),
    Categoria("esports", "ESP", "eSports"),
    Categoria("esqui", "ESQ", "Esqui"),
)

fun categoriaDe(esporte: String): Categoria =
    CATEGORIAS.firstOrNull { it.chave == esporte } ?: Categoria(
        esporte,
        esporte.replace("_", "").take(3).uppercase(),
        esporte.split("_").joinToString(" ") { p -> p.replaceFirstChar { it.uppercase() } },
    )

/** Categoria da bolinha de um evento: a que vem nos dados (livre) ou a do esporte. */
fun Evento.categoria(): Categoria =
    categoria?.let { Categoria(it.chave, it.sigla.take(4).uppercase(), it.rotulo) } ?: categoriaDe(esporte)

/** Destaque: ao vivo que dá pra ver; senão, o melhor que dá pra ver nas próximas 48h. */
fun escolherDestaque(eventos: List<Evento>, servicos: Set<String>, agora: ZonedDateTime): Evento? {
    val possiveis = eventos.filter { it.daPraVer(servicos) && it.situacao != "adiado" && it.horarioDefinido }
    possiveis.filter { it.aoVivo(agora) }.maxByOrNull { it.pontos }?.let { return it }
    val limite = agora.plusHours(48)
    return possiveis.filter { it.inicioZoned().isAfter(agora) && it.inicioZoned().isBefore(limite) }
        .maxWithOrNull(compareBy<Evento> { it.pontos }.thenByDescending { it.inicioZoned() })
}

private val fmtHora = DateTimeFormatter.ofPattern("HH:mm", PT)
private val fmtDiaMes = DateTimeFormatter.ofPattern("d MMM", PT)
private val fmtSemana = DateTimeFormatter.ofPattern("EEE", PT)
private val fmtDataLonga = DateTimeFormatter.ofPattern("EEEE, d MMM", PT)

fun Evento.hora(): String = if (horarioDefinido) inicioZoned().format(fmtHora) else "—"

fun ZonedDateTime.hora(): String = format(fmtHora)

private fun String.semPonto() = replace(".", "")

fun rotuloDia(dia: LocalDate, hoje: LocalDate): Pair<String, String> {
    val sub = dia.format(fmtDiaMes).semPonto()
    return when (dia) {
        hoje -> "Hoje" to "${dia.format(fmtSemana).semPonto()}, $sub"
        hoje.plusDays(1) -> "Amanhã" to "${dia.format(fmtSemana).semPonto()}, $sub"
        else -> dia.format(fmtSemana).semPonto().replaceFirstChar { it.uppercase() } to sub
    }
}

fun rotuloDiaCurto(e: Evento, hoje: LocalDate): String {
    val dia = e.inicioZoned().toLocalDate()
    return when (dia) {
        hoje -> "Hoje"
        hoje.plusDays(1) -> "Amanhã"
        else -> "${dia.format(fmtSemana).semPonto().replaceFirstChar { it.uppercase() }} ${dia.dayOfMonth}"
    }
}

fun Evento.dataLonga(hoje: LocalDate): String {
    val dia = inicioZoned().toLocalDate()
    return when (dia) {
        hoje -> "Hoje, ${dia.format(fmtDiaMes).semPonto()}"
        hoje.plusDays(1) -> "Amanhã, ${dia.format(fmtDiaMes).semPonto()}"
        else -> dia.format(fmtDataLonga).semPonto().replaceFirstChar { it.uppercase() }
    }
}

fun formatarAtualizacao(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    return runCatching {
        val z = OffsetDateTime.parse(iso).atZoneSameInstant(BRASILIA)
        val hoje = LocalDate.now(BRASILIA)
        val dia = when (z.toLocalDate()) {
            hoje -> "hoje"
            hoje.minusDays(1) -> "ontem"
            hoje.plusDays(1) -> "amanhã"
            else -> z.format(DateTimeFormatter.ofPattern("EEE d MMM", PT)).semPonto()
        }
        "$dia às ${z.format(fmtHora)}"
    }.getOrDefault("—")
}

/** Tamanho do título do Detalhe pelo comprimento do nome. */
fun tamanhoTitulo(titulo: String): Int = when {
    titulo.length <= 10 -> 56
    titulo.length <= 16 -> 46
    titulo.length <= 24 -> 38
    else -> 32
}

/** Linha de competição + motivo, como no design. */
fun Evento.linhaMeta(): String {
    val motivo = motivos.firstOrNull()
    return if (motivo != null) "$competicao · $motivo" else competicao
}

fun Evento.tipoLegivel(o: Onde): String = when (o.tipo) {
    "tv_aberta" -> "TV aberta"
    "youtube" -> "YouTube"
    "streaming" -> "Streaming"
    "tv_fechada" -> "TV fechada"
    "ppv" -> "Pay-per-view"
    else -> o.tipo
}
