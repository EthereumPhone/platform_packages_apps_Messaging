package com.android.messaging.datamodel

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import java.util.concurrent.CompletableFuture
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import org.web3j.ens.EnsResolver
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.math.BigInteger
import android.os.StrictMode

class WalletSDK(
    context: Context,
    val web3RPC: String = "https://eth-mainnet.g.alchemy.com/v2/7VRG5CtXPdmq6p65GXf2uz6g_8Xb2oPz"
)  {

    companion object {
        const val SYS_SERVICE_CLASS = "android.os.WalletProxy"
        const val SYS_SERVICE = "wallet"
        const val DECLINE = "decline"
        const val NOTFULFILLED = "notfulfilled"
    }


    private val cls: Class<*> = Class.forName(SYS_SERVICE_CLASS)
    private val createSession = cls.declaredMethods[2]
    private val getUserDecision = cls.declaredMethods[5]
    private val hasBeenFulfilled = cls.declaredMethods[6]
    private val sendTransaction =  cls.declaredMethods[7]
    private val signMessageSys = cls.declaredMethods[8]
    private val getAddress = cls.declaredMethods[3]
    private val getChainId = cls.declaredMethods[4]
    private val changeChainId = cls.declaredMethods[1]
    private var address: String? = null
    @SuppressLint("WrongConstant")
    private val proxy = context.getSystemService(SYS_SERVICE)
    private var web3j: Web3j? = null
    private var sysSession: String? = null

    init {
        if (proxy == null) {
            throw Exception("No system wallet found")
        } else {
            sysSession = createSession.invoke(proxy) as String
            val reqID = getAddress.invoke(proxy, sysSession) as String
            while ((hasBeenFulfilled.invoke(proxy, reqID) as String) == NOTFULFILLED) {
                Thread.sleep(10)
            }
            address = hasBeenFulfilled.invoke(proxy, reqID) as String
        }
        web3j = Web3j.build(HttpService(web3RPC))
    }

    /**
     * Sends transaction to
     */

    fun getTransactionCount(address: String): String {
        val url = URL(web3RPC)
        
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        
        connection.setRequestProperty("accept", "application/json")
        connection.setRequestProperty("content-type", "application/json")
        
        connection.doOutput = true
        
        val jsonInputString = """
        {
        "id": 1,
        "jsonrpc": "2.0",
        "params": [
            "$address",
            "latest"
        ],
        "method": "eth_getTransactionCount"
        }
        """.trimIndent()
        
        val os: OutputStream = connection.outputStream
        os.write(jsonInputString.toByteArray(Charsets.UTF_8))
        os.flush()
        
        val responseCode = connection.responseCode
        
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            var line: String?
            val response = StringBuilder()
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()
            
            connection.disconnect()
            
            val jsonObj = JSONObject(response.toString())
            if (jsonObj.has("result")) {
                return hexStringToBigInteger(jsonObj.getString("result"))
            } else {
                throw Exception("Error: $responseCode")
            }
        } else {
            connection.disconnect()
            throw Exception("Error: $responseCode")
        }
    }


    fun getGasPrice(): String {
        val url = URL(web3RPC)
        
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        
        connection.setRequestProperty("accept", "application/json")
        connection.setRequestProperty("content-type", "application/json")
        
        connection.doOutput = true
        
        val jsonInputString = """
        {
        "id": 1,
        "jsonrpc": "2.0",
        "method": "eth_gasPrice"
        }
        """.trimIndent()
        
        val os: OutputStream = connection.outputStream
        os.write(jsonInputString.toByteArray(Charsets.UTF_8))
        os.flush()
        
        val responseCode = connection.responseCode
        
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            var line: String?
            val response = StringBuilder()
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()
            
            connection.disconnect()
            
            val jsonObj = JSONObject(response.toString())
            if (jsonObj.has("result")) {
                return hexStringToBigInteger(jsonObj.getString("result"))
            } else {
                throw Exception("Error: $responseCode")
            }
        } else {
            connection.disconnect()
            throw Exception("Error: $responseCode")
        }
    }

    fun sendTransaction(signedTx: String): String {
        val url = URL(web3RPC)

        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"

        connection.setRequestProperty("accept", "application/json")
        connection.setRequestProperty("content-type", "application/json")

        connection.doOutput = true

        val jsonInputString = """
    {
    "id": 1,
    "jsonrpc": "2.0",
    "method": "eth_sendRawTransaction",
    "params": ["$signedTx"]
    }
    """.trimIndent()

        val os: OutputStream = connection.outputStream
        os.write(jsonInputString.toByteArray(Charsets.UTF_8))
        os.flush()

        val responseCode = connection.responseCode

        if (responseCode == HttpURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            var line: String?
            val response = StringBuilder()
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()

            connection.disconnect()

            val jsonObj = JSONObject(response.toString())
            if (jsonObj.has("result")) {
                return jsonObj.getString("result") // Transaction hash
            } else {
                throw Exception("Error: $responseCode")
            }
        } else {
            connection.disconnect()
            throw Exception("Error: $responseCode")
        }
    }

    fun hexStringToBigInteger(hexString: String): String {
        // Remove "0x" prefix if present
        val cleanHexString = if (hexString.startsWith("0x")) hexString.substring(2) else hexString
        
        // Parse the cleaned hex string to a BigInteger
        return BigInteger(cleanHexString, 16).toString()
    }

    fun resolveENS(ensName: String): String {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()

        StrictMode.setThreadPolicy(policy)
        // Replace with the URL of your Ethereum node's RPC endpoint
        val url = "https://ensdata.net/$ensName"
        println("Sending request to $url")
        val client = OkHttpClient()
        val request: Request = okhttp3.Request.Builder()
            .url(url)
            .build()
        val response = client.newCall(request).execute()
        val responseText = response.body!!.string()
        println("Response: $responseText")
        val jsonObject = JSONObject(responseText)
        return if (jsonObject.has("address")) {
            jsonObject.getString("address");
        } else {
            throw Exception("ENS name not found");
        }
    }


    @RequiresApi(Build.VERSION_CODES.N)
    fun sendTransaction(toI: String, value: String, data: String, gasPrice: String? = null, gasAmount: String = "21000", chainId: Int = 1): CompletableFuture<String> {
        val completableFuture = CompletableFuture<String>()
        var gasPriceVAL = gasPrice
        println("ETHSENDTEST: Checking proxy $proxy")
        if(proxy != null) {
            // Use system-wallet
            
            println("ETHSENDTEST: GOING INTO COMPLETABLE FUTURE")
            CompletableFuture.runAsync {
                val to = if (toI.startsWith("0x")) {
                    toI
                } else {
                    resolveENS(toI)
                }
                println("ETHSENDTEST: Address: $to")
                println("ETHSENDTEST: INSIDE COMPLETABLE FUTURE")
                println("ETHSENDTEST: MY OWN ADDRESS: ${address}")
                val ethGetTransactionCount = try {
                    getTransactionCount(address!!)
                } catch (e: Exception) {
                    println("ETHSENDTEST: ERROR: ${e.message}")
                    e.printStackTrace()
                    "0"
                }
                 

                println("ETHSENDTEST: ${ethGetTransactionCount}")

                if (gasPrice == null) {
                    gasPriceVAL = getGasPrice()
                }

                println("ETHSENDTEST: ${gasPrice}")

                val reqID = sendTransaction.invoke(proxy, sysSession, to, value, data, ethGetTransactionCount, gasPriceVAL, gasAmount, chainId)

                var result = NOTFULFILLED

                while (true) {
                    val tempResult =  hasBeenFulfilled!!.invoke(proxy, reqID)
                    if (tempResult != null) {
                        result = tempResult as String
                        if(result != NOTFULFILLED) {
                            break
                        }
                    }
                    Thread.sleep(100)
                    println("ETHSENDTEST temp-result: ${result}")
                }
                if (result == DECLINE) {
                    println("ETHSENDTEST hasbeendeclined: ${result}")
                    completableFuture.complete(DECLINE)
                } else {
                    try {
                        println("ETHSENDTEST result: ${result}")
                        val txHash = sendTransaction(result)
                        println("ETHSENDTEST: TxHash: ${txHash}")
                        completableFuture.complete(txHash)
                    } catch (e: Exception) {
                        println("ETHSENDTEST: TxHash: ${e.message}")
                        e.printStackTrace()
                    }

                }
            }
            return completableFuture
        } else {
            throw Exception("No system wallet found")
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun signMessage(message: String, type: String = "personal_sign"): CompletableFuture<String> {
        val completableFuture = CompletableFuture<String>()
        if(proxy != null) {
            CompletableFuture.runAsync {
                val reqID = signMessageSys.invoke(proxy, sysSession, message, type) as String

                var result =  NOTFULFILLED

                while (true) {
                    val tempResult =  hasBeenFulfilled!!.invoke(proxy, reqID)
                    if (tempResult != null) {
                        result = tempResult as String
                        if(result != NOTFULFILLED) {
                            break
                        }
                    }
                    Thread.sleep(100)
                }
                completableFuture.complete(result)
            }

            return completableFuture
        } else {
            throw Exception("No system wallet found")
        }
    }

    /**
     * Creates connection to the Wallet system service.
     * If wallet is not found, user is redirect to WalletConnect login
     */
    fun createSession(onConnected: ((address: String) -> Unit)? = null): String {
        if(proxy != null) {
            onConnected?.let { it(sysSession.orEmpty()) }
            return sysSession.orEmpty()
        } else {
            throw Exception("No system wallet found")
        }
    }


    fun getAddress(): String {
        if (proxy != null) {
            return address.orEmpty()
        } else {
            throw Exception("No system wallet found")
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun getChainId(): Int {
        if (proxy != null) {
            val completableFuture = CompletableFuture<Int>()
            CompletableFuture.runAsync {
                val reqId = getChainId.invoke(proxy, sysSession) as String
                while ((hasBeenFulfilled.invoke(proxy, reqId) as String) == NOTFULFILLED) {
                    Thread.sleep(10)
                }
                completableFuture.complete(Integer.parseInt(hasBeenFulfilled.invoke(proxy, reqId) as String))
            }
            return completableFuture.get()
        } else {
            throw Exception("No system wallet found")
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun changeChainid(chainId: Int): CompletableFuture<String> {
        val completableFuture = CompletableFuture<String>()
        if(proxy != null) {
            CompletableFuture.runAsync {
                val reqID = changeChainId.invoke(proxy, sysSession, chainId) as String

                var result =  NOTFULFILLED

                while (true) {
                    val tempResult =  hasBeenFulfilled!!.invoke(proxy, reqID)
                    if (tempResult != null) {
                        result = tempResult as String
                        if(result != NOTFULFILLED) {
                            break
                        }
                    }
                    Thread.sleep(100)
                }
                completableFuture.complete(result)
            }

            return completableFuture
        } else {
            throw Exception("No system wallet found")
        }
    }

    fun isEthOS(): Boolean {
        return proxy != null
    }
}
