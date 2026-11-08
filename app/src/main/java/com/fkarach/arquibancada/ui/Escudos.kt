package com.fkarach.arquibancada.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fkarach.arquibancada.data.Evento
import com.fkarach.arquibancada.logica.categoria

/** Escudo num disco claro (o branco "refletor" do app); sem logo, cai para o monograma. */
@Composable
fun Escudo(nome: String, url: String?, tamanho: Dp, sigla: String? = null, contorno: Boolean = false) {
    Box(
        Modifier.size(tamanho).clip(CircleShape)
            .background(if (url != null) Cor.ink else Cor.raised)
            .then(if (contorno) Modifier.border(2.dp, Cor.ground, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = nome,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(tamanho * 0.16f),
            )
        } else {
            Text((sigla ?: nome).take(3).uppercase(), style = Tipo.timeM.copy(fontSize = (tamanho.value * 0.34f).sp, color = Cor.ink))
        }
    }
}

/** Selo visual do evento: dois escudos sobrepostos (jogos) ou a sigla do esporte (F1, lutas, eSports...). */
@Composable
fun MarcaDoEvento(e: Evento, logos: Map<String, String>, tamanho: Dp = 26.dp) {
    if (e.times.size == 2) {
        val sobrepor = tamanho * 0.3f
        Box(Modifier.width(tamanho * 2 - sobrepor)) {
            Escudo(e.times[0], logos[e.times[0]], tamanho, e.monogramas.getOrNull(0))
            Box(Modifier.offset(x = tamanho - sobrepor)) {
                Escudo(e.times[1], logos[e.times[1]], tamanho, e.monogramas.getOrNull(1), contorno = true)
            }
        }
    } else {
        Box(Modifier.width(tamanho * 2 - tamanho * 0.3f), contentAlignment = Alignment.CenterStart) {
            Escudo(e.categoria().rotulo, null, tamanho, e.categoria().sigla)
        }
    }
}

/** Escudos grandes lado a lado, para o Detalhe e o cartão de destaque. */
@Composable
fun ConfrontoGrande(e: Evento, logos: Map<String, String>, tamanho: Dp) {
    if (e.times.size != 2) return
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Escudo(e.times[0], logos[e.times[0]], tamanho, e.monogramas.getOrNull(0))
        Text("x", style = Tipo.body.copy(color = Cor.inkSubtle))
        Escudo(e.times[1], logos[e.times[1]], tamanho, e.monogramas.getOrNull(1))
    }
}
