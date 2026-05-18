package com.example.displaymoto.ui.screens.navigation

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.displaymoto.MainActivity
import com.example.displaymoto.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Foreground service que mantém o GPS ativo mesmo com a app em background
 * enquanto há uma rota ativa. Não usa Google Play Services Location para manter
 * a app livre de dependências Google (consistente com escolha de MapLibre).
 *
 * Emissões via singleton SharedFlow para evitar binder/IPC (a app é single-process).
 */
class LocationTrackingService : Service() {

    companion object {
        private const val CHANNEL_ID = "displaymoto_nav"
        private const val NOTIFICATION_ID = 4421
        const val ACTION_START = "com.example.displaymoto.action.START_TRACKING"
        const val ACTION_STOP  = "com.example.displaymoto.action.STOP_TRACKING"

        private val _updates = MutableSharedFlow<Location>(
            replay = 1,
            extraBufferCapacity = 4
        )
        /** Stream global de Locations enquanto o service está vivo. */
        val updates: SharedFlow<Location> = _updates.asSharedFlow()

        fun iniciar(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).setAction(ACTION_START)
            ContextCompat.startForegroundService(context, intent)
        }

        fun parar(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }

    private var locationManager: LocationManager? = null
    private val listener = LocationListener { location ->
        _updates.tryEmit(location)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                pararTracking()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> iniciarTracking()
        }
        return START_NOT_STICKY
    }

    private fun iniciarTracking() {
        criarCanal()
        val notif = construirNotificacao()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notif)
        }

        val temPermissao = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!temPermissao) {
            stopSelf()
            return
        }

        try {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val provider = if (locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER))
                LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
            // 2s / 5m — equilíbrio bateria vs responsividade para uma moto em movimento
            locationManager!!.requestLocationUpdates(provider, 2000L, 5f, listener)
        } catch (_: SecurityException) {
            stopSelf()
        }
    }

    private fun pararTracking() {
        try { locationManager?.removeUpdates(listener) } catch (_: Exception) {}
        locationManager = null
    }

    override fun onDestroy() {
        pararTracking()
        super.onDestroy()
    }

    private fun criarCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val canal = NotificationChannel(
                    CHANNEL_ID,
                    "Navegação DisplayMoto",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Notificação persistente durante a navegação ativa."
                    setShowBadge(false)
                }
                nm.createNotificationChannel(canal)
            }
        }
    }

    private fun construirNotificacao(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nav)
            .setContentTitle("DisplayMoto a navegar")
            .setContentText("A acompanhar a tua posição em rota")
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
