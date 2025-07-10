package com.example.appemisor

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.inputmethodservice.Keyboard.Row
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import java.nio.ByteBuffer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.gestures.snapping.SnapPosition.Center
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        lateinit var transmitter: Transmitter
        super.onCreate(savedInstanceState)
        transmitter = Transmitter(this)

        setContent {
            TransmitterScreen(transmitter)
        }
    }
}

@Composable
fun TransmitterScreen(transmitter: Transmitter) {
    var temperature by remember { mutableStateOf(25) } // simulated temperature
    var humidity by remember { mutableStateOf(50) }    // simulated humidity
    var isAdvertising by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val requestBluetoothAdvertisePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted, now attempt to start advertising
                // You might want to also check if Bluetooth is enabled here if it's not done elsewhere
                if (checkBluetoothEnabled(context)) {
                    transmitter.startAdvertiser(temperature, humidity)
                    isAdvertising = true
                } else {
                    Toast.makeText(context, "Bluetooth está desactivado. Por favor, actívalo.", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, "Permiso de Bluetooth Advertise denegado. No se puede iniciar la publicidad.", Toast.LENGTH_LONG).show()
                isAdvertising = false // Ensure state is reset if permission is denied
            }
        }
    )

    // Side effect to check Bluetooth enabled status on composition or when needed
    // This is optional but good practice to guide the user.
    DisposableEffect(Unit) {
        if (!checkBluetoothEnabled(context)) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            context.startActivity(enableBtIntent)
            Toast.makeText(context, "Por favor, activa Bluetooth para iniciar la publicidad.", Toast.LENGTH_LONG).show()
        }
        onDispose {}
    }


    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Temperatura: $temperature °C", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Humedad: $humidity %", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = {
                if (!isAdvertising) {
                    // Check for BLUETOOTH_ADVERTISE permission first
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // For Android 12 (API 31) and above
                        when {
                            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED -> {
                                // Permission already granted, proceed with starting advertising
                                if (checkBluetoothEnabled(context)) {
                                    transmitter.startAdvertiser(temperature, humidity)
                                    isAdvertising = true
                                } else {
                                    Toast.makeText(context, "Bluetooth está desactivado. Por favor, actívalo.", Toast.LENGTH_LONG).show()
                                }
                            }
                            // Optionally, handle if you should show a rationale to the user
                            // ActivityCompat.shouldShowRequestPermissionRationale(...)
                            else -> {
                                // Request the permission
                                requestBluetoothAdvertisePermissionLauncher.launch(Manifest.permission.BLUETOOTH_ADVERTISE)
                            }
                        }
                    } else {

                        if (checkBluetoothEnabled(context)) {
                            transmitter.startAdvertiser(temperature, humidity)
                            isAdvertising = true
                        } else {
                            Toast.makeText(context, "Bluetooth está desactivado. Por favor, actívalo.", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    // If advertising, stop it directly (assuming permission to stop is implicit if you could start)
                    // It's still good practice to check, but less critical than starting.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) {
                            transmitter.stopAdvertiser()
                            isAdvertising = false
                        } else {
                            Toast.makeText(context, "Permiso de Bluetooth Advertise denegado. No se puede detener la publicidad.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        transmitter.stopAdvertiser()
                        isAdvertising = false
                    }
                }
            }) {
                Text(if (isAdvertising) "Detener transmisión" else "Iniciar transmisión")
            }
        }
    }
}

// Helper function to check if Bluetooth is enabled
@SuppressLint("ServiceCast")
private fun checkBluetoothEnabled(context: Context): Boolean {
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter = bluetoothManager?.adapter
    return bluetoothAdapter?.isEnabled == true
}