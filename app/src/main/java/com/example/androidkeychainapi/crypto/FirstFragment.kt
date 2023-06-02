package com.example.androidkeychainapi.crypto

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.androidkeychainapi.databinding.FragmentFirstBinding
import java.io.File
import java.io.FileInputStream

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {
	private var _binding: FragmentFirstBinding? = null
	private val binding get() = _binding!!
	private  val cryptoManager = CryptoManager()
	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		
		_binding = FragmentFirstBinding.inflate(inflater, container, false)
		return binding.root
		
	}
	

	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.resultButton.setOnClickListener {
			//findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
			encryptText(binding.editTex.text.toString())
		}
		binding.decryptButton.setOnClickListener {
			decryptText()
		}
	}
	
	private fun encryptText(toString: String) {
		if (toString.isBlank()) return
		val file = File(context?.filesDir, "secret.txt")
		if(!file.exists()){
			file.createNewFile()
		}
		val fos= file.outputStream()
		val encryptedText= cryptoManager.encrypt(toString.encodeToByteArray(), fos).decodeToString()
		Log.d(CryptoManager.TAG,"Encrypted Text: $encryptedText")
		
		binding.textviewFirst.text= "Encrypted Text: $encryptedText"
		
	}private fun decryptText() {
		val file = File(context?.filesDir, "secret.txt")
		if(file.exists()){
			val inputString = file.bufferedReader().use { it.readText() }
			//@Հ���U�|)/��
			Log.d(CryptoManager.TAG,"Text to be Encrypted: $inputString")
			val decryptedText= cryptoManager.decrypt(FileInputStream(file)).decodeToString()
			binding.textviewFirst.text= "Decrypted Text: $decryptedText"
		}
		
	}
	
	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}
}