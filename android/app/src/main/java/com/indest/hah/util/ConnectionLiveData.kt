package com.indest.hah.util

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.LiveData
import com.indest.hah.game.enums.Internet
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

val TAG = "ConnectionLiveData"

class ConnectionLiveData(context: Context) : LiveData<Internet>() {

    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private val cm = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
    private val validNetworks: MutableSet<Network> = HashSet()

    init {
        postValue(Internet.OFFLINE)
    }

    private fun postInternetStateValue() {
        if (validNetworks.size > 0) {
            if (isConnectedToInternet()) {
                postValue(Internet.ONLINE)
                Log.d(TAG, "Internet connection: ${Internet.ONLINE}.")
            } else {
                postValue(Internet.OFFLINE)
                Log.d(TAG, "Internet connection: ${Internet.OFFLINE}.")
            }
        } else {
            postValue(Internet.OFFLINE)
            Log.d(TAG, "Internet connection: ${Internet.OFFLINE}.")
        }
    }

    override fun onActive() {
        networkCallback = createNetworkCallback()
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(networkRequest, networkCallback)
    }

    override fun onInactive() {
        cm.unregisterNetworkCallback(networkCallback)
    }

    private fun createNetworkCallback() = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val networkCapabilities = cm.getNetworkCapabilities(network)
            val hasInternetCapability = networkCapabilities?.hasCapability(NET_CAPABILITY_INTERNET)
            if (hasInternetCapability == true) {
                validNetworks.add(network)
                postInternetStateValue()
            }
        }

        override fun onLost(network: Network) {
            validNetworks.remove(network)
            postInternetStateValue()
        }
    }

    private fun isConnectedToInternet(): Boolean {
        val url = URL("https://www.google.com")
        (url.openConnection() as? HttpURLConnection)?.runCatching {
            requestMethod = "GET"
            val hasInternet = responseCode == HttpsURLConnection.HTTP_OK
            inputStream.close()
            disconnect()
            return hasInternet
        }?.onFailure {
            Log.e(TAG, "URL openConnection onFailure: ${it.message}")
            return false
        }
        return false
    }

}