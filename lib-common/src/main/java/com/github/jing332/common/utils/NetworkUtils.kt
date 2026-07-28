package com.github.jing332.common.utils

import android.util.Log
import cn.hutool.core.lang.Validator
import com.drake.net.Net
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.Enumeration

// https://github.com/gedoor/legado/blob/f01629d7e2f97d06796273bac3c13eb16f068325/app/src/main/java/io/legado/app/utils/NetworkUtils.kt
object NetworkUtils {

    /**
     * Get local Ip address.
     */
    fun getLocalIpAddress(): List<InetAddress> {
        val enumeration: Enumeration<NetworkInterface>
        try {
            enumeration = NetworkInterface.getNetworkInterfaces()
        } catch (e: SocketException) {
            return emptyList()
        }

        val addressList = mutableListOf<InetAddress>()

        while (enumeration.hasMoreElements()) {
            val nif = enumeration.nextElement()
            val addresses = nif.inetAddresses ?: continue
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()
                if (!address.isLoopbackAddress && isIPv4Address(address.hostAddress)) {
                    addressList.add(address)
                }
            }
        }
        return addressList
    }

    /**
     * Check if valid IPV4 address.
     *
     * @param input the address string to check for validity.
     * @return True if the input parameter is a valid IPv4 address.
     */
    fun isIPv4Address(input: String?): Boolean {
        return input != null && input.isNotEmpty()
                && input[0] in '1'..'9'
                && input.count { it == '.' } == 3
                && Validator.isIpv4(input)
    }

    /**
     * Check if valid IPV6 address.
     */
    fun isIPv6Address(input: String?): Boolean {
        return input != null && input.contains(":") && Validator.isIpv6(input)
    }

    /**
     * Check if valid IP address.
     */
    fun isIPAddress(input: String?): Boolean {
        return isIPv4Address(input) || isIPv6Address(input)
    }

    fun uploadLog(log: String): String? {
        val uploadUrl = "https://bin.kv2.dev"

        return try {
            val resp = Net.post(uploadUrl) {
                body = log.toRequestBody("text/plain".toMediaType())
            }.execute<Response>()

            resp.request.url.toString()
        } catch (e: Exception) {
            Log.e("UploadLog", e.toString())
            null
        }
    }
}