// 816032089

package dev.example.studentapp.network

import android.util.Log
import com.google.gson.Gson
import dev.example.studentapp.models.ContentModel
import java.io.BufferedReader
import java.io.BufferedWriter
import java.net.Socket
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.concurrent.thread
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.text.Charsets.UTF_8

class Client (private val networkMessageInterface: NetworkMessageInterface, plaintext: String){
    private lateinit var clientSocket: Socket
    private lateinit var reader: BufferedReader
    private lateinit var writer: BufferedWriter
    var ip:String = ""

    private val strongSeed = hashStrSha256(plaintext)
    private val aesKey = generateAESKey(strongSeed)
    private val aesIV = generateIV(strongSeed)
    private var connectMessage = true
    private var bool = true

    init {
        thread {
            clientSocket = Socket("192.168.49.1", Server.PORT)
            reader = clientSocket.inputStream.bufferedReader()
            writer = clientSocket.outputStream.bufferedWriter()
            ip = clientSocket.inetAddress.hostAddress!!
            while(true){
                try{

                    val initialMessage = ContentModel("I am here.", ip)
                    sendMessage(initialMessage)
                    var serverResponse = reader.readLine()
                    if (serverResponse != null){
                        var serverContent = Gson().fromJson(serverResponse, ContentModel::class.java)
                        Log.e("Client" ,"Received a message from the server' ${serverContent.message}'")

                        var encryptedMessage = encryptMessage(serverContent.message,aesKey,aesIV)
                        Log.e("Client", "Encryption")

                        val serverReply = ContentModel(encryptedMessage,strongSeed)
                        sendMessage(serverReply)
                        Log.e("Client", "Encrypted Message Sent")


                        sendMessage(ContentModel(encryptMessage(plaintext, aesKey, aesIV),ip))

                        serverResponse = reader.readLine()
                        if (serverResponse != null){
                            serverContent = Gson().fromJson(serverResponse, ContentModel::class.java)
                            if(decryptMessage(serverContent.message,aesKey,aesIV)!="Valid") {

                                clientSocket.close()
                            }
                            Log.e("Client" ,"Received a message from the server' ${serverContent.message}'")
                        }
                        networkMessageInterface.onContent(serverContent)
                    }
                } catch(e: Exception){
                    Log.e("CLIENT", "An error has occurred in the client")
                    e.printStackTrace()
                    break
                }
            }
        }
    }

    fun sendMessage(content: ContentModel){
        thread {
            if (!clientSocket.isConnected){
                throw Exception("We aren't currently connected to the server!")
            }
            val contentAsStr:String = Gson().toJson(content)
            writer.write("$contentAsStr\n")
            writer.flush()
        }

    }

    fun close(){
        clientSocket.close()
    }

    private fun ByteArray.toHex() = joinToString(separator = "") { byte -> "%02x".format(byte) }

    private fun getFirstNChars(str: String, n:Int) = str.substring(0,n)

    private fun hashStrSha256(str: String): String{
        val algorithm = "SHA-256"
        val hashedString = MessageDigest.getInstance(algorithm).digest(str.toByteArray(UTF_8))
        return hashedString.toHex();
    }

    private fun generateAESKey(seed: String): SecretKeySpec {
        val first32Chars = getFirstNChars(seed,32)
        val secretKey = SecretKeySpec(first32Chars.toByteArray(), "AES")
        return secretKey
    }

    private fun generateIV(seed: String): IvParameterSpec {
        val first16Chars = getFirstNChars(seed, 16)
        return IvParameterSpec(first16Chars.toByteArray())
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun encryptMessage(plaintext: String, aesKey: SecretKey, aesIv: IvParameterSpec):String{
        val plainTextByteArr = plaintext.toByteArray()

        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, aesIv)

        val encrypt = cipher.doFinal(plainTextByteArr)
        return Base64.Default.encode(encrypt)
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun decryptMessage(encryptedText: String, aesKey: SecretKey, aesIv: IvParameterSpec):String{
        val textToDecrypt = Base64.Default.decode(encryptedText)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")

        cipher.init(Cipher.DECRYPT_MODE, aesKey,aesIv)

        val decrypt = cipher.doFinal(textToDecrypt)
        return String(decrypt)

    }
}