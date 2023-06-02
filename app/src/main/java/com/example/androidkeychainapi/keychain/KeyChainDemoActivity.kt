package com.example.androidkeychainapi.keychain

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.security.KeyChain
import android.security.KeyChainAliasCallback
import android.security.KeyChainException
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.androidkeychainapi.R
import com.example.androidkeychainapi.databinding.ActivityKeyChainDemoBinding
import java.io.BufferedInputStream
import java.io.IOException
import java.security.PrivateKey
import java.security.cert.X509Certificate

class KeyChainDemoActivity : AppCompatActivity(), KeyChainAliasCallback {
	
	private lateinit var binding: ActivityKeyChainDemoBinding
	
	/** Called when the activity is first created.  */
	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_key_chain_demo)
		binding = ActivityKeyChainDemoBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		// Set the view using the main.xml layout
		// Check whether the key chain is installed or not. This takes time and
		// should be done in another thread other than the main thread.
		Thread {
			if (isKeyChainAccessible) {
				// Key chain installed. Disable the install button and print
				// the key chain information
				disableKeyChainButton()
				printInfo()
			} else {
				Log.d(TAG, "Key Chain is not accessible")
			}
		}.start()
		
		// Setup the key chain installation button
		binding.keychainButton1.setOnClickListener { _: View? -> installPkcs12() }
		
		// Setup the simple SSL web server start/stop button
		binding.serverButton.setOnClickListener {
			if (binding.serverButton.text == resources.getString(R.string.server_start)) {
				binding.serverButton.setText(R.string.server_stop)
				startService(Intent(this, SecureWebServerService::class.java))
			} else {
				binding.serverButton.setText(R.string.server_start)
				stopServer()
			}
		}
		
		// Setup the test SSL page button
		findViewById<View>(R.id.test_ssl_button).setOnClickListener { v: View? ->
			startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TEST_SSL_URL)))
		}
	}
	
	/**
	 * This will be called when the user click on the notification to stop the
	 * SSL server
	 */
	override fun onNewIntent(intent: Intent) {
		Log.d(TAG, "In onNewIntent()")
		super.onNewIntent(intent)
		val isStopServer = intent.getBooleanExtra(EXTRA_STOP_SERVER, false)
		if (isStopServer) {
			binding.serverButton.setText(R.string.server_start)
			stopServer()
		}
	}
	
	/**
	 * This implements the KeyChainAliasCallback
	 */
	override fun alias(alias: String?) {
		if (alias != null) {
			setAlias(alias) // Set the alias in the application preference
			disableKeyChainButton()
			printInfo()
		} else {
			Log.d(TAG, "User hit Disallow")
		}
	}
	
	/**
	 * This method returns the alias of the key chain from the application
	 * preference
	 *
	 * @return The alias of the key chain
	 */
	private val alias: String?
		get() {
			val pref = getSharedPreferences(
				KEYCHAIN_PREF, MODE_PRIVATE
			)
			return pref.getString(KEYCHAIN_PREF_ALIAS, DEFAULT_ALIAS)
		}
	
	/**
	 * This method sets the alias of the key chain to the application preference
	 */
	private fun setAlias(alias: String) {
		val pref = getSharedPreferences(KEYCHAIN_PREF, MODE_PRIVATE)
		val editor = pref.edit()
		editor.putString(KEYCHAIN_PREF_ALIAS, alias)
		editor.apply()
	}
	
	/**
	 * This method prints the key chain information.
	 */
	@SuppressLint("SetTextI18n")
	private fun printInfo() {
		val alias = alias
		val certs = getCertificateChain(alias)
		val privateKey = getPrivateKey(alias)
		val sb = StringBuffer()
		for (cert in certs!!) {
			sb.append(cert.issuerDN)
			sb.append("\n")
		}
		runOnUiThread {
			val certTv = findViewById<View>(R.id.cert) as TextView
			val privateKeyTv = findViewById<View>(R.id.private_key) as TextView
			certTv.text = sb.toString()
			privateKeyTv.text = privateKey?.format + ":" + privateKey
		}
	}
	
	/**
	 * This method will launch an intent to install the key chain
	 */
	private fun installPkcs12() {
		try {
			val bis = BufferedInputStream(assets.open(PKCS12_FILENAME))
			val keychain = ByteArray(bis.available())
			bis.read(keychain)
			val installIntent = KeyChain.createInstallIntent()
			installIntent.putExtra(KeyChain.EXTRA_PKCS12, keychain)
			installIntent.putExtra(KeyChain.EXTRA_NAME, DEFAULT_ALIAS)
			// startActivityForResult(installIntent, INSTALL_KEYCHAIN_CODE)
			startForResult.launch(installIntent)
			
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}
	
	/*
		override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
			if (requestCode == INSTALL_KEYCHAIN_CODE) {
				when (resultCode) {
					RESULT_OK -> chooseCert()
					else -> super.onActivityResult(requestCode, resultCode, data)
				}
			}
		}
	*/
	
	private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
		if (result.resultCode == Activity.RESULT_OK) {
			val intent = result.data
			chooseCert()
		}
	}
	
	private fun chooseCert() {
		KeyChain.choosePrivateKeyAlias(
			this, this, arrayOf(),  // Any key types.
			null,  // Any issuers.
			"localhost",  // Any host
			-1,  // Any port
			DEFAULT_ALIAS
		)
	}
	
	private fun getCertificateChain(alias: String?): Array<X509Certificate>? {
		try {
			return KeyChain.getCertificateChain(this, alias!!)
		} catch (e: KeyChainException) {
			e.printStackTrace()
		} catch (e: InterruptedException) {
			e.printStackTrace()
		}
		return null
	}
	
	private fun getPrivateKey(alias: String?): PrivateKey? {
		try {
			return KeyChain.getPrivateKey(this, alias!!)
		} catch (e: KeyChainException) {
			e.printStackTrace()
		} catch (e: InterruptedException) {
			e.printStackTrace()
		}
		return null
	}
	
	/**
	 * This method checks if the key chain is installed
	 *
	 * @return true if the key chain is not installed or allowed
	 */
	private val isKeyChainAccessible: Boolean
		private get() = (getCertificateChain(alias) != null && getPrivateKey(alias) != null)
	
	/**
	 * This method starts the background service of the simple SSL web server
	 */
	private fun startServer() {
		val secureWebServerIntent = Intent(this, SecureWebServerService::class.java)
		startService(Intent(this, SecureWebServerService::class.java))
	}
	
	/**
	 * This method stops the background service of the simple SSL web server
	 */
	private fun stopServer() {
		stopService(Intent(this, SecureWebServerService::class.java))
	}
	
	/**
	 * This is a convenient method to disable the key chain install button
	 */
	private fun disableKeyChainButton() {
		runOnUiThread {
			binding.keychainButton1.setText(R.string.keychain_installed)
			binding.keychainButton1.isEnabled = false
		}
	}
	
	companion object {
		/**
		 * The file name of the PKCS12 file used
		 */
		const val PKCS12_FILENAME = "keychain.p12"
		
		/**
		 * The pass phrase of the PKCS12 file
		 */
		const val PKCS12_PASSWORD = "changeit"
		
		/**
		 * Intent extra name to indicate to stop server
		 */
		const val EXTRA_STOP_SERVER = "stop_server"
		
		// Log tag for this class
		private const val TAG = "KeyChainApiActivity"
		
		// Alias for certificate
		private const val DEFAULT_ALIAS = "My Key Chain"
		
		// Name of the application preference
		private const val KEYCHAIN_PREF = "keychain"
		
		// Name of preference name that saves the alias
		private const val KEYCHAIN_PREF_ALIAS = "alias"
		
		// Request code used when starting the activity using the KeyChain install
		// intent
		private const val INSTALL_KEYCHAIN_CODE = 1
		
		// Test SSL URL
		private const val TEST_SSL_URL = "https://localhost:8080"
	}
}