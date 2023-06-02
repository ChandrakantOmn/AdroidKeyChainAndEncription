package com.example.androidkeychainapi.keychain

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.CATEGORY_CALL
import com.example.androidkeychainapi.R


class SecureWebServerService : Service() {
	// A handle to the simple SSL web server
	lateinit var sws: SecureWebServer
	
	/**
	 * Start the SSL web server and set an on-going notification
	 */
	override fun onCreate() {
		super.onCreate()
		sws = SecureWebServer(this)
		sws.start()
		createNotification()
	}
	
	/**
	 * Stop the SSL web server and remove the on-going notification
	 */
	override fun onDestroy() {
		super.onDestroy()
		sws.stop()
		stopForeground(STOP_FOREGROUND_REMOVE)
	}
	
	/**
	 * Return null as there is nothing to bind
	 */
	override fun onBind(intent: Intent): IBinder? {
		return null
	}
	
	/**
	 * Create an on-going notification. It will stop the server when the user
	 * clicks on the notification.
	 */
	private fun createNotification() {
		val notificationChannel = NotificationChannel(
			"CHANNEL_ID",
			"Foreground Service Channel",
			NotificationManager.IMPORTANCE_HIGH
		)
		notificationChannel.description = "Channel description"
		notificationChannel.enableLights(true)
		notificationChannel.lightColor = Color.RED
		notificationChannel.enableVibration(true)
		val notificationManager = getSystemService(NotificationManager::class.java)
		notificationManager.createNotificationChannel(notificationChannel)
		
		val notificationIntent = Intent(this, KeyChainDemoActivity::class.java)
		notificationIntent.putExtra(KeyChainDemoActivity.EXTRA_STOP_SERVER, true)
		val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
		val notification: Notification = NotificationCompat.Builder(this, "CHANNEL_ID")
			.setContentTitle(getText(R.string.notification_title))
			.setContentText(getText(R.string.notification_message))
			.setSmallIcon(R.mipmap.ic_launcher_round)
			.setTicker(getText(R.string.ticker_text))
			.setPriority(NotificationCompat.PRIORITY_HIGH)
			.setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE)
			.setCategory(CATEGORY_CALL)
			.setContentIntent(pendingIntent)
			.build()
		startForeground(101, notification)
	}
	
	
	
	companion object {
		// Log tag for this class
		private const val TAG = "SecureWebServerService"
		
		// A special ID assigned to this on-going notification.
		private const val ONGOING_NOTIFICATION = 1248
	}
}