package com.fkarach.arquibancada.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fkarach.arquibancada.AppViewModel
import com.fkarach.arquibancada.Estado
import com.fkarach.arquibancada.data.Evento
import com.fkarach.arquibancada.logica.aConfirmar
import com.fkarach.arquibancada.logica.aoVivo
import com.fkarach.arquibancada.logica.dataLonga
import com.fkarach.arquibancada.logica.fim
import com.fkarach.arquibancada.logica.formatarAtualizacao
import com.fkarach.arquibancada.logica.hora
import com.fkarach.arquibancada.logica.inicioAnteriorZoned
import com.fkarach.arquibancada.logica.inicioZoned
import com.fkarach.arquibancada.logica.ondeOrdenado
import com.fkarach.arquibancada.logica.tamanhoTitulo
import com.fkarach.arquibancada.logica.tipoLegivel

@Composable
fun TelaDetalhe(e: Evento, estado: Estado, vm: AppViewModel) {
    val ctx = LocalContext.current
    val agora = estado.agora
    val vivo = e.aoVivo(agora)
    val lembrete = estado.versaoLembretes.let { vm.lembreteAtivo(e) }
    val anterior = e.inicioAnteriorZoned()

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(44.dp).clickable(role = Role.Button) { vm.abrir(null) }.semantics { contentDescription = "Voltar" },
                contentAlignment = Alignment.Center) { IconeVoltar() }
            if (vivo) SeloAoVivo(tamanho = 12)
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (e.situacao == "alterado" && anterior != null) {
                Box(Modifier.padding(horizontal = 20.dp).padding(bottom = 6.dp)) {
                    BannerMudanca("Horário alterado", "Mudou de ${anterior.hora()} para ${e.hora()}." + if (lembrete) " Seu lembrete foi reagendado." else "")
                }
            }
            if (e.situacao == "adiado") {
                Box(Modifier.padding(horizontal = 20.dp).padding(bottom = 6.dp)) {
                    BannerMudanca("Adiado", "Sem nova data por enquanto. O lembrete fica suspenso até remarcarem.")
                }
            }

            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(e.competicao.uppercase() + if (e.meuTime) " · MEU TIME" else "", style = Tipo.eyebrow.copy(color = Cor.inkSubtle))
                Text(
                    e.titulo.uppercase(),
                    style = Tipo.displayL.copy(fontSize = tamanhoTitulo(e.titulo).sp, lineHeight = (tamanhoTitulo(e.titulo) * 0.94).sp, color = Cor.ink),
                )
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Column {
                        Text(e.dataLonga(agora.toLocalDate()), style = Tipo.meta.copy(color = Cor.inkSubtle))
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (e.horarioDefinido) e.hora() else "A definir", style = Tipo.timeL.copy(color = Cor.ink))
                            if (anterior != null && e.situacao == "alterado") {
                                Text(anterior.hora(), style = Tipo.body.copy(color = Cor.inkSubtle, textDecoration = TextDecoration.LineThrough))
                            }
                        }
                    }
                    if (!e.local.isNullOrBlank()) {
                        Box(Modifier.width(1.dp).height(48.dp).background(Cor.border))
                        Column(Modifier.weight(1f)) {
                            Text("Local", style = Tipo.meta.copy(color = Cor.inkSubtle))
                            Text(e.local, style = Tipo.event.copy(color = Cor.ink))
                        }
                    }
                }
            }

            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                OndeAssistir(e, estado)
                if (e.porQueVale.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("POR QUE VALE", style = Tipo.section.copy(color = Cor.ink))
                        Text(e.porQueVale.joinToString(" "), style = Tipo.body.copy(color = Cor.inkSoft))
                    }
                }
                if (!e.observacao.isNullOrBlank()) {
                    Text(e.observacao, style = Tipo.meta.copy(fontSize = 13.sp, lineHeight = 18.sp, color = Cor.inkMuted))
                }
                Espaco(12)
            }
        }

        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (lembrete) {
                BotaoSecundario("Aviso 30 min antes", Modifier.weight(1f), icone = { IconeSino(true, Cor.ink, 18) }) { vm.alternarLembrete(e) }
            } else {
                BotaoPrimario("Avisar 30 min antes", Modifier.weight(1f), icone = { IconeSino(true, Cor.onAccent, 18) }) { vm.alternarLembrete(e) }
            }
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(26.dp)).background(Cor.surface)
                    .border(1.dp, Cor.borderStrong, RoundedCornerShape(26.dp))
                    .clickable(role = Role.Button) {
                        try { ctx.startActivity(intentAgenda(e, estado.servicos)) }
                        catch (_: ActivityNotFoundException) { Toast.makeText(ctx, "Nenhum app de agenda encontrado", Toast.LENGTH_SHORT).show() }
                    }
                    .semantics { contentDescription = "Adicionar à agenda do celular" },
                contentAlignment = Alignment.Center,
            ) { IconeCalendario(Cor.ink, mais = true) }
        }
    }
}

@Composable
private fun OndeAssistir(e: Evento, estado: Estado) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Cor.surface)
            .border(1.dp, Cor.border, RoundedCornerShape(18.dp)).padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("ONDE ASSISTIR", style = Tipo.section.copy(color = Cor.ink))
            if (e.aConfirmar()) TextoAConfirmar() else Text("✓ confirmado", style = Tipo.channel.copy(fontSize = 12.sp, color = Cor.ink))
        }
        val lista = e.ondeOrdenado(estado.servicos)
        if (lista.isEmpty()) {
            Divisor()
            Text("Nenhuma transmissão no Brasil anunciada ainda.", style = Tipo.body.copy(fontSize = 14.sp, color = Cor.inkSoft),
                modifier = Modifier.padding(vertical = 10.dp))
        }
        lista.forEach { o ->
            Divisor()
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(o.canal, style = Tipo.event.copy(color = Cor.ink))
                    Text(e.tipoLegivel(o), style = Tipo.meta.copy(color = Cor.inkSubtle))
                }
                when {
                    o.gratis -> SeloGratis(grande = true)
                    o.servico != null && o.servico in estado.servicos -> SeloNoSeu(o.servico, grande = true)
                }
            }
        }
        val origem = if (e.aConfirmar()) e.origemCanal?.takeIf { it.isNotBlank() }?.let { "Previsto: $it" } else "Confirmado"
        Text(
            listOfNotNull(origem, "atualizado ${formatarAtualizacao(estado.agenda?.atualizadoEm)}").joinToString(" · ") + ".",
            style = Tipo.meta.copy(color = Cor.inkSubtle),
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

private fun intentAgenda(e: Evento, servicos: Set<String>): Intent {
    val onde = e.ondeOrdenado(servicos).joinToString(", ") { it.canal }.ifBlank { "a confirmar" }
    return Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, e.titulo)
        putExtra(CalendarContract.Events.DESCRIPTION, "${e.competicao}\nOnde assistir: $onde\n(Arquibancada)")
        e.local?.let { putExtra(CalendarContract.Events.EVENT_LOCATION, it) }
        if (e.horarioDefinido) {
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, e.inicioZoned().toInstant().toEpochMilli())
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, e.fim().toInstant().toEpochMilli())
        } else {
            val dia = e.inicioZoned().toLocalDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
            putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, dia)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, dia + 86_400_000L)
        }
    }
}
