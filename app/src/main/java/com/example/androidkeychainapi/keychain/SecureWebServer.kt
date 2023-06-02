package com.example.androidkeychainapi.keychain

import android.content.Context
import android.util.Base64
import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLServerSocketFactory

 class SecureWebServer(ctx: Context) {
	private var sssf: SSLServerSocketFactory? = null
	private var sss: SSLServerSocket? = null
	// A flag to control whether the web server should be kept running
	private var isRunning = true
	// The base64 encoded image string used as an embedded image
	private val base64Image: String
	
	/**
	 * WebServer constructor.
	 */
	init {
		try {
			// Get an SSL context using the TLS protocol
			val sslContext = SSLContext.getInstance("TLS")
			// Get a key manager factory using the default algorithm
			val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
			// Load the PKCS12 key chain
			val ks = KeyStore.getInstance("PKCS12")
			val fis = ctx.assets
				.openFd(KeyChainDemoActivity.PKCS12_FILENAME)
				.createInputStream()
			ks.load(fis, KeyChainDemoActivity.PKCS12_PASSWORD.toCharArray())
			kmf.init(ks, KeyChainDemoActivity.PKCS12_PASSWORD.toCharArray())
			// Initialize the SSL context
			sslContext.init(kmf.keyManagers, null, null)
			// Create the SSL server socket factory
			sssf = sslContext.serverSocketFactory
		} catch (e: Exception) {
			e.printStackTrace()
		}
		
		// Create the base64 image string used in the server response
		base64Image = createBase64Image(ctx)
	}
	
	/**
	 * This method starts the web server listening to the port 8080
	 */
	 fun start() {
		Thread(Runnable {
			Log.d(TAG, "Secure Web Server is starting up on port 8080")
			sss = try {
				// Create the secure server socket
				sssf!!.createServerSocket(8080) as SSLServerSocket
			} catch (e: Exception) {
				println("Error: $e")
				return@Runnable
			}
			Log.d(TAG, "Waiting for connection")
			while (isRunning) {
				try {
					// Wait for an SSL connection
					val socket = sss!!.accept()
					
					// Got a connection
					Log.d(TAG, "Connected, sending data.")
					val `in` = BufferedReader(
						InputStreamReader(socket.getInputStream())
					)
					val out = PrintWriter(
						socket
							.getOutputStream()
					)
					
					// Read the data until a blank line is reached which
					// signifies the end of the client HTTP headers
					var str = "."
					while (str != "") str = `in`.readLine()
					
					// Send a HTTP response
					out.println("HTTP/1.0 200 OK")
					out.println("Content-Type: text/html")
					out.println("Server: Android KeyChainiDemo SSL Server")
					// this blank line signals the end of the headers
					out.println("")
					// Send the HTML page
					out.println("<H1>Welcome to Android!</H1>")
					// Add an embedded Android image
					out.println("<img src='data:image/png;base64,$base64Image'/>")
					out.flush()
					socket.close()
				} catch (e: Exception) {
					Log.d(TAG, "Error: $e")
				}
			}
		}).start()
	}
	
	/**
	 * This method stops the SSL web server
	 */
	 fun stop() {
		try {
			// Break out from the infinite while loop in start()
			isRunning = false
			
			// Close the socket
			if (sss != null) {
				sss!!.close()
			}
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}
	
	/**
	 * This method reads a binary image from the assets folder and returns the
	 * base64 encoded image string.
	 *
	 * @param ctx The service this web server is running in.
	 * @return String The base64 encoded image string or "" if there is an
	 * exception
	 */
	private fun createBase64Image(ctx: Context): String {
		val bis: BufferedInputStream
		try {
			bis = BufferedInputStream(ctx.assets.open(EMBEDDED_IMAGE_FILENAME))
			val embeddedImage = ByteArray(bis.available())
			bis.read(embeddedImage)
			return Base64.encodeToString(embeddedImage, Base64.DEFAULT)
		} catch (e: IOException) {
			e.printStackTrace()
		}
		return ""
	}
	
	companion object {
		// Log tag for this class
		private const val TAG = "SecureWebServer"
		// File name of the image used in server response
		private const val EMBEDDED_IMAGE_FILENAME = "training-prof.png"
	}
}