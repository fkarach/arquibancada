package com.fkarach.arquibancada.lembretes

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.fkarach.arquibancada.MainActivity
import com.fkarach.arquibancada.R
import com.fkarach.arquibancada.data.Preferencias
import com.fkarach.arquibancada.data.Repositorio
import com.fkarach.arquibancada.logica.BRASILIA
import com.fkarach.arquibancada.logica.canalPrincipal
import com.fkarach.arquibancada.logica.daPraVer
import com.fkarach.arquibancada.logica.gratis
import com.fkarach.arquibancada.logica.inicioZoned
import java.time.ZonedDateTime

/** Programa um aviso 30 min antes de cada evento com lembrete ligado. */
object Lembretes {
    const val CANAL = "lembretes"
    private const val ANTECEDENCIA_MIN = 30L
    private const val PREFS = "lembretes_programados"

    fun criarCanal(ctx: Context) {
        val canal = NotificationChannel(CANAL, "Lembretes de início", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Aviso 30 minutos antes dos eventos que você marcou"
        }
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    fun reprogramar(ctx: Context) {
        val app = ctx.applicationContext
        val agenda = Repositorio(app).agendaSalva() ?: return
        val prefs = Preferencias(app)
        val servicos = prefs.servicos(Repositorio(app).seguindoSalvo()?.servicos.orEmpty())
        val alarmes = app.getSystemService(AlarmManager::class.java)
        val agora = ZonedDateTime.now(BRASILIA)
        val registro = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        // Cancela tudo o que foi programado antes.
        registro.getStringSet("ids", emptySet())!!.forEach { id -> alarmes.cancel(pendente(app, id, null, null)) }

        val novos = mutableSetOf<String>()
        agenda.eventos
            .filter { it.horarioDefinido && it.situacao != "adiado" && prefs.lembreteAtivo(it) }
            .forEach { e ->
                val quando = e.inicioZoned().minusMinutes(ANTECEDENCIA_MIN)
                if (quando.isAfter(agora)) {
                    val canal = e.canalPrincipal(servicos)
                    val corpo = when {
                        e.gratis() -> "$canal · grátis"
                        e.daPraVer(servicos) -> "$canal · no seu serviço"
                        e.ondeAssistir.isEmpty() -> "Canal ainda não confirmado"
                        else -> canal
                    }
                    alarmes.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        quando.toInstant().toEpochMilli(),
                        pendente(app, e.id, "${e.titulo} em 30 min", corpo),
                    )
                    novos += e.id
                }
            }
        registro.edit().putStringSet("ids", novos).apply()
    }

    private fun pendente(ctx: Context, id: String, titulo: String?, corpo: String?): PendingIntent {
        val intent = Intent(ctx, LembreteReceiver::class.java).apply {
            action = "com.fkarach.arquibancada.LEMBRETE"
            putExtra("id", id)
            titulo?.let { putExtra("titulo", it) }
            corpo?.let { putExtra("corpo", it) }
        }
        return PendingIntent.getBroadcast(ctx, id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun notificar(ctx: Context, id: String, titulo: String, corpo: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val abrir = PendingIntent.getActivity(
            ctx, id.hashCode(),
            Intent(ctx, MainActivity::class.java).putExtra(MainActivity.EXTRA_EVENTO, id).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, CANAL)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setColor(0xFFFF7A59.toInt())
            .setContentTitle(titulo)
            .setContentText(corpo)
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(ctx).notify(id.hashCode(), n)
    }
}

class LembreteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("id") ?: return
        val titulo = intent.getStringExtra("titulo") ?: return
        val corpo = intent.getStringExtra("corpo") ?: ""
        Lembretes.notificar(context, id, titulo, corpo)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) Lembretes.reprogramar(context)
    }
}
