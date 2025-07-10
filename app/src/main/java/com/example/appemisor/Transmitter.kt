package com.example.appemisor
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import java.nio.ByteBuffer


class Transmitter(private val context: Context) {
    private val TAG = "Transmitter"
    private val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter
    private val advertiser = bluetoothAdapter?.bluetoothLeAdvertiser

    private var currentTemperature = 0
    private var currentHumidity = 0

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    fun startAdvertiser(temperature: Int, humidity: Int) {
        currentTemperature = temperature
        currentHumidity = humidity

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Permiso BLUETOOTH_ADVERTISE denegado")
            return
        }

        if (bluetoothAdapter == null || advertiser == null) {
            Log.e(TAG, "Bluetooth no soportado o advertiser nulo")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Log.e(TAG, "Bluetooth está desactivado")
            return
        }

        // Construir manufacturer data:
        // Aquí se usan 23 bytes, con los primeros 18 reservados para UUID y beacon,
        // y los últimos dos bytes para temperatura y humedad (valores enteros 0-255)
        val manufacturerData = ByteBuffer.allocate(23)
        val uuidBytes = getUuidBytes("6ef0e30d73084458b62ef706c692ca77")

        manufacturerData.put(0, 0x02.toByte()) // Beacon identifier
        manufacturerData.put(1, 0x15.toByte()) // Beacon identifier
        for (i in 2..17) {
            manufacturerData.put(i, uuidBytes[i - 2])
        }
        manufacturerData.put(18, 0x00.toByte()) // Major (puede usarse para otros datos)
        manufacturerData.put(19, 0x05.toByte()) // Major
        manufacturerData.put(20, temperature.toByte()) // Temperatura en byte (0-255)
        manufacturerData.put(21, humidity.toByte())    // Humedad en byte (0-255)
        manufacturerData.put(22, 0x76.toByte()) // txPower (valor arbitrario)

        val dataBuilder = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addManufacturerData(76, manufacturerData.array())

        val settingsBuilder = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setConnectable(false)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)

        advertiser.stopAdvertising(callbackClose)
        advertiser.startAdvertising(settingsBuilder.build(), dataBuilder.build(), callback)
        Log.d(TAG, "Publicidad iniciada con temperatura=$temperature y humedad=$humidity")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    fun stopAdvertiser() {

        advertiser?.stopAdvertising(callbackClose)
        Log.d(TAG, "Publicidad detenida")
    }

    private val callback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            Log.d(TAG, "Publicidad iniciada correctamente")
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.e(TAG, "Error al iniciar publicidad: $errorCode")
        }
    }

    private val callbackClose = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            Log.d(TAG, "Publicidad detenida correctamente")
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.e(TAG, "Error al detener publicidad: $errorCode")
        }
    }

    private fun getUuidBytes(uuidStr: String): ByteArray {
        // Convierte el UUID sin guiones a bytes
        val cleanStr = uuidStr.replace("-", "")
        val bytes = ByteArray(16)
        for (i in 0 until 16) {
            val index = i * 2
            bytes[i] = cleanStr.substring(index, index + 2).toInt(16).toByte()
        }
        return bytes
    }
}