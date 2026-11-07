package com.fkarach.arquibancada

import android.app.Application
import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fkarach.arquibancada.data.Repositorio
import com.fkarach.arquibancada.lembretes.Lembretes
import com.fkarach.arquibancada.widget.Widgets
import java.util.concurrent.TimeUnit

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Lembretes.criarCanal(this)
        val pedido = PeriodicWorkRequestBuilder<AtualizacaoWorker>(12, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("atualizar_agenda", ExistingPeriodicWorkPolicy.KEEP, pedido)
    }
}

/** Em segundo plano: baixa a agenda mais recente, reprograma avisos e redesenha os widgets. */
class AtualizacaoWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val ok = Repositorio(applicationContext).baixar()
        Lembretes.reprogramar(applicationContext)
        Widgets.atualizarTodos(applicationContext)
        return if (ok) Result.success() else Result.retry()
    }
}
