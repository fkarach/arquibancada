package com.fkarach.arquibancada

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fkarach.arquibancada.ui.Cor
import com.fkarach.arquibancada.ui.IconeCalendario
import com.fkarach.arquibancada.ui.IconeEstrela
import com.fkarach.arquibancada.ui.TelaAgenda
import com.fkarach.arquibancada.ui.TelaDetalhe
import com.fkarach.arquibancada.ui.TelaMeus
import com.fkarach.arquibancada.ui.TemaArquibancada
import com.fkarach.arquibancada.ui.Tipo

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        pedirPermissaoDeNotificacao()
        intent?.getStringExtra(EXTRA_EVENTO)?.let { vm.abrir(it) }
        setContent {
            TemaArquibancada { Raiz(vm) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_EVENTO)?.let { vm.abrir(it) }
    }

    private fun pedirPermissaoDeNotificacao() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    companion object {
        const val EXTRA_EVENTO = "evento_id"
    }
}

@Composable
private fun Raiz(vm: AppViewModel) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val detalhe = estado.detalheId?.let { id -> estado.agenda?.eventos?.firstOrNull { it.id == id } }

    BackHandler(enabled = detalhe != null || estado.aba != Aba.AGENDA) {
        if (detalhe != null) vm.abrir(null) else vm.irPara(Aba.AGENDA)
    }

    Box(Modifier.fillMaxSize().background(Cor.ground)) {
        if (detalhe != null) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                TelaDetalhe(detalhe, estado, vm)
            }
        } else {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
                Box(Modifier.weight(1f)) {
                    when (estado.aba) {
                        Aba.AGENDA -> TelaAgenda(estado, vm)
                        Aba.MEUS -> TelaMeus(estado, vm)
                    }
                }
                BarraInferior(estado.aba) { vm.irPara(it) }
            }
        }
    }
}

@Composable
private fun BarraInferior(aba: Aba, onAba: (Aba) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Cor.ground).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Cor.raised))
        Row(Modifier.fillMaxWidth().height(68.dp)) {
            ItemBarra("Agenda", aba == Aba.AGENDA, { IconeCalendario(if (aba == Aba.AGENDA) Cor.ink else Cor.inkSubtle, tamanho = 24) }) { onAba(Aba.AGENDA) }
            ItemBarra("Meus", aba == Aba.MEUS, { IconeEstrela(aba == Aba.MEUS, if (aba == Aba.MEUS) Cor.ink else Cor.inkSubtle) }) { onAba(Aba.MEUS) }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ItemBarra(texto: String, ativo: Boolean, icone: @Composable () -> Unit, onClick: () -> Unit) {
    Column(
        Modifier.weight(1f).fillMaxSize().clickable(role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        icone()
        Text(texto, style = Tipo.meta.copy(fontSize = 11.sp, color = if (ativo) Cor.ink else Cor.inkSubtle))
    }
}
