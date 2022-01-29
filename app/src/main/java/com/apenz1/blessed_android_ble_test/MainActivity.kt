package com.apenz1.blessed_android_ble_test

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.leinardi.android.speeddial.FabWithLabelView
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialView
import java.util.*
import kotlin.math.abs
import kotlin.math.ceil


private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val LOCATION_PERMISSION_REQUEST_CODE = 2

private const val SERVICE_UUID = "a7f8fe1b-a57d-46a3-a0da-947a1d8c59ce"
private const val CHAR_FOR_READ_UUID = "f33dee97-d3d8-4fbd-8162-c980133f0c93"
private const val STEP_MOVEMENT_WRITE_UUID = "908badf3-fd6b-4eec-b362-2810e97db94e"
private const val START_STACKING_WRITE_UUID = "bed1dd25-79f2-4ce2-a6fa-000471efe3a0"
private const val CONTINUOUS_MOVEMENT_WRITE_UUID = "28c74a57-67cb-4b43-8adf-8776c1dbc475"
private const val NOTIFY_CURRENT_MOTOR_POSITION_UUID = "d2f362f4-6542-4b13-be5e-8c81d423a347"
private const val NOTIFY_STACKING_STEPS_TAKEN_UUID = "c14b46e2-553a-4664-98b0-653494882964"
private const val CCC_DESCRIPTOR_UUID =
    "00002902-0000-1000-8000-00805f9b34fb" // Client Characteristic Configuration Descriptor (standard for notify)

// TODO: Bluetooth gets disconnected on application tilt (MainActivity recreated??)
class MainActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var speedDialView: SpeedDialView
    private lateinit var bluetoothToggleButton: FabWithLabelView
    private lateinit var progressBar: ProgressBar
    private lateinit var bluetoothQueue: BleQueue

    private val sharedViewModel: SharedViewModel by viewModels()

    enum class BLELifecycleState {
        Disconnected,
        Scanning,
        Connecting,
        Connected
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }
    private var lifecycleState = BLELifecycleState.Disconnected
    private var isScanning = false
    private var connectedGatt: BluetoothGatt? = null
    private var characteristicForRead: BluetoothGattCharacteristic? = null
    var characteristicForStepMovementWrite: BluetoothGattCharacteristic? = null
    var characteristicForContinuousMovementWrite: BluetoothGattCharacteristic? = null
    var characteristicForStackingStartWrite: BluetoothGattCharacteristic? = null
    var characteristicForMotorPositionNotify: BluetoothGattCharacteristic? = null
    var characteristicForStackingStepsTakenNotify: BluetoothGattCharacteristic? = null

    // Display Snackbar notification that can be dismissed
    private fun displaySnackbar(msg: String, length: Int) {
        val parentLayout = findViewById<View>(android.R.id.content)
        val snackBar = Snackbar.make(parentLayout, msg, length)
        snackBar.setAction("DISMISS") {
            snackBar.dismiss()
        }
        snackBar.anchorView = speedDialView // Display above FAB
        snackBar.show()
    }

    //-- Permission utility functions --//
    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
        }
    }

    private fun requestLocationPermission() {
        if (isLocationPermissionGranted) {
            return
        }
        runOnUiThread {
            val alertDialog: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
            alertDialog.setTitle("Location permission required")
            alertDialog.setMessage("Location access is needed to scan for BLE devices.")
            alertDialog.setPositiveButton("Ok") { _, _ ->
                requestPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
            alertDialog.setNegativeButton("Cancel") { _, _ ->
                displaySnackbar("Failed to start scanning!", Snackbar.LENGTH_SHORT)
            }
            val alert: AlertDialog = alertDialog.create()
            alert.setCanceledOnTouchOutside(false)
            alert.show()

        }
    }

    private val isLocationPermissionGranted
        get() = hasPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)

    fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBar = findViewById(R.id.toolBarProgressBar)
        viewPager = findViewById(R.id.view_pager)

        // The pager adapter, which provides the pages to the view pager widget.
        val pagerAdapter = ViewPagerFragmentSateAdapter(this)
        viewPager.adapter = pagerAdapter

        // Setup Tab layout
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            val tabTitlesList = listOf("Settings", "Control")
            tab.text = tabTitlesList[position]
        }.attach()

        setupSpeedDial()
        setSupportActionBar(findViewById(R.id.topToolbar))

        //-- Setup observing of ViewModel command data change + sending command to device --//
        // Step movement
        sharedViewModel.stepMovementCommand.observe(this) { newCommand ->
            writeMessageToPeripheral(
                newCommand,
                characteristicForStepMovementWrite
            )
        }
        // Continuous movement
        sharedViewModel.continuousMovementCommand.observe(this) { newCommand ->
            writeMessageToPeripheral(
                newCommand,
                characteristicForContinuousMovementWrite
            )
        }
        // Shutter release
        // TODO: Implement
        sharedViewModel.shutterCommand.observe(this) { newCommand ->
            writeMessageToPeripheral(
                newCommand.toString(),
                characteristicForContinuousMovementWrite
            )
        }
        // Stop stacking
        sharedViewModel.stopStackingCommand.observe(this) {
            writeMessageToPeripheral(
                "Stop",
                characteristicForStackingStartWrite
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    // SpeedDial setup (always on top button)
    private fun setupSpeedDial() {
        speedDialView = findViewById<SpeedDialView>(R.id.speedDial)
        speedDialView.addActionItem(
            SpeedDialActionItem.Builder(
                R.id.fab_start_stacking_button,
                R.drawable.ic_start_stacking_white_24
            )
                .setLabel("Start stacking")
                .setFabSize(FloatingActionButton.SIZE_NORMAL)
                .create()
        )
        // Create Bluetooth connect/disconnect toggle
        bluetoothToggleButton = speedDialView.addActionItem(
            SpeedDialActionItem.Builder(R.id.fab_bluetooth_button, R.drawable.ic_bluetooth_white_24)
                .setLabel("Connect device")
                .setFabSize(FloatingActionButton.SIZE_NORMAL)
                .create()
        )!!
        // Action click handlers
        speedDialView.setOnActionSelectedListener(SpeedDialView.OnActionSelectedListener { actionItem ->
            when (actionItem.id) {
                R.id.fab_bluetooth_button -> {
                    // TODO: App crash when scan is running and user disables Bluetooth
                    // Re-start scanning (might throw error if not yet registered)
                    try {
                        unregisterReceiver(bleOnOffListener)
                    } catch (e: IllegalArgumentException) {
                        e.printStackTrace()
                    }
                    val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                    registerReceiver(bleOnOffListener, filter)
                    // Restart lifecycle + stop/start scan
                    if (isScanning) {
                        bleRestartLifecycle(false)
                    } else {
                        bleRestartLifecycle(true)
                    }
                }
                R.id.fab_start_stacking_button -> {
                    // Get values
                    val preShutterWaitTime = sharedViewModel.preShutterWaitTime.value
                    val postShutterWaitTime = sharedViewModel.postShutterWaitTime.value
                    val shuttersPerStep = sharedViewModel.shuttersPerStep.value
                    val stepSize = sharedViewModel.stepSize.value
                    val returnToStartPosition = sharedViewModel.returnToStartPosition.value

                    // Calculate other settings (specific to stacking mode used)
                    var startPosition = sharedViewModel.currentMotorPosition.value
                    var numberOfStepsToTake = 0
                    var movementDirection = "FWD"
                    if (sharedViewModel.operationMode.value == "Start to end") {
                        val startPos = sharedViewModel.startPositionStacking.value
                        val endPos = sharedViewModel.endPositionStacking.value
                        if (startPos != null && endPos != null) {
                            startPosition = startPos
                            val totalDistance = endPos - startPos
                            numberOfStepsToTake =
                                ceil(abs(totalDistance.toDouble()) / stepSize!!).toInt()
                            if (totalDistance < 0) {
                                movementDirection = "BCK"
                            }
                        }
                    } else if (sharedViewModel.operationMode.value == "Distance") {
                        val totalDistance = sharedViewModel.totalStackingDistance.value
                        if (totalDistance != null) {
                            numberOfStepsToTake =
                                ceil(totalDistance.toDouble() / stepSize!!).toInt()
                        }
                        movementDirection = sharedViewModel.movementDirection.value.toString()
                    }

                    val msgToWrite =
                        "PRE$preShutterWaitTime;PST$postShutterWaitTime;STP$shuttersPerStep;STS$stepSize;DIR$movementDirection;SPS$startPosition;NST$numberOfStepsToTake;RTS$returnToStartPosition"
                    Log.d("MSGToSend", msgToWrite)
                    // Send processed stack instructions to device
                    writeMessageToPeripheral(
                        msgToWrite,
                        characteristicForStackingStartWrite
                    )
                }
            }
            // Close the SpeedDial (with animation)
            speedDialView.close()
            return@OnActionSelectedListener true
            false
        })
    }

    // Update BluetoothToggle FAB state (label text) + notify user with Snackbar update
// TODO: Inspect what happens on peripheral disconnect (unplug device)
// TODO: Not reconnecting after long scan?? (scan started due to disconnection)
    fun updateBluetoothToggleState() {
        runOnUiThread {
            if (connectedGatt == null) {
                // No device connected
                if (!isScanning) {
                    // Device not connected + not scanning
                    bluetoothToggleButton.apply {
                        speedDialActionItem =
                            speedDialActionItemBuilder.setLabel("Connect to device").create()
                    }
                    progressBar.visibility = View.GONE
                } else {
                    // Scan in progress
                    bluetoothToggleButton.apply {
                        speedDialActionItem =
                            speedDialActionItemBuilder.setLabel("Stop scanning")
                                .create()
                    }
                    progressBar.visibility = View.VISIBLE
                    displaySnackbar("Scan in progress...", Snackbar.LENGTH_SHORT)
                }
            } else {
                // Device connected
                bluetoothToggleButton.apply {
                    speedDialActionItem =
                        speedDialActionItemBuilder.setLabel("Disconnect from device")
                            .create()
                }
                progressBar.visibility = View.GONE
                displaySnackbar("Connected to device.", Snackbar.LENGTH_LONG)
            }
        }
    }

    // "adapter" for ViewPager2; handles which fragment to show
    private class ViewPagerFragmentSateAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int {
            return 2
        }

        override fun createFragment(position: Int): Fragment {
            if (position == 0) {
                return SettingsScreenFragment()
            } else if (position == 1) {
                return ControlScreenFragment()
            }
            return SettingsScreenFragment()
        }
    }

    override fun onDestroy() {
        bleEndLifecycle()
        super.onDestroy()
    }

/*
fun onTapRead(view: View) {
    var gatt = connectedGatt ?: run {
        displaySnackbar("No device connected!", Snackbar.LENGTH_SHORT)
        return
    }
    var characteristic = characteristicForRead ?: run {
        Log.d("ERROR", "read failed, characteristic unavailable $CHAR_FOR_READ_UUID")
        return
    }
    if (!characteristic.isReadable()) {
        Log.d("ERROR", "read failed, characteristic not readable $CHAR_FOR_READ_UUID")
        return
    }
    gatt.readCharacteristic(characteristic)
}
*/

    // Send a message to the peripheral
    private fun writeMessageToPeripheral(
        msg: String,
        characteristic: BluetoothGattCharacteristic?
    ) {
        var gatt = connectedGatt ?: run {
            displaySnackbar("Device not connected!", Snackbar.LENGTH_SHORT)
            return
        }
        if (characteristic == null) {
            displaySnackbar(
                "write failed, characteristic unavailable.",
                Snackbar.LENGTH_SHORT
            )
            return
        }
        if (!characteristic.isWriteable()) {
            displaySnackbar(
                "write failed, characteristic not writeable.",
                Snackbar.LENGTH_SHORT
            )
            return
        }
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        characteristic.value = msg.toByteArray(Charsets.UTF_8)
        bluetoothQueue.writeCharacteristic(characteristic)
    }

    private fun bleEndLifecycle() {
        safeStopBleScan()
        connectedGatt?.close()
        setConnectedGattToNull()
        lifecycleState = BLELifecycleState.Disconnected
    }

    private fun setConnectedGattToNull() {
        connectedGatt = null

        characteristicForRead = null
        characteristicForStepMovementWrite = null
        characteristicForContinuousMovementWrite = null
        characteristicForStackingStartWrite = null
        characteristicForMotorPositionNotify = null
        characteristicForStackingStepsTakenNotify = null

        updateBluetoothToggleState()
    }

    // Restart BLE lifecycle
    private fun bleRestartLifecycle(scanBool: Boolean) {
        runOnUiThread {
            if (scanBool) {
                if (connectedGatt == null) {
                    safeStartBleScan()
                } else {
                    connectedGatt?.disconnect()
                }
            } else {
                bleEndLifecycle()
            }
        }
    }

    private fun safeStartBleScan() {
        if (isScanning) {
            return
        }
        if (!isLocationPermissionGranted) {
            // Permission (only needed for scanning)
            requestLocationPermission()
            return
        }

        // Proceed to scan
        val serviceFilter = scanFilter.serviceUuid?.uuid.toString()
        Log.d("MSG", "Starting BLE scan, filter: $serviceFilter")

        isScanning = true
        lifecycleState = BLELifecycleState.Scanning
        val scanSettings: ScanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
            .setReportDelay(0)
            .build()
        bleScanner.startScan(mutableListOf(scanFilter), scanSettings, scanCallback)
        updateBluetoothToggleState()
    }

    private fun safeStopBleScan() {
        if (!isScanning) {
            Log.d("MSG", "Already stopped")
            return
        }

        Log.d("MSG", "Stopping BLE scan")
        isScanning = false
        bleScanner.stopScan(scanCallback)

        // Don't update bluetooth toggle state here as after stopping the scan; bluetooth connection will begin
    }

    // TODO: Allow unsubscribe??
    private fun subscribeToNotifications(
        characteristic: BluetoothGattCharacteristic,
        gatt: BluetoothGatt
    ) {
        // First allow notifications, then write to config descriptor
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(UUID.fromString(CCC_DESCRIPTOR_UUID))
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        bluetoothQueue.writeDescriptor(descriptor)
    }


    private fun unsubscribeFromCharacteristic(characteristic: BluetoothGattCharacteristic) {
        val gatt = connectedGatt ?: return
        characteristic.getDescriptor(UUID.fromString(CCC_DESCRIPTOR_UUID))?.let { cccDescriptor ->
            if (!gatt.setCharacteristicNotification(characteristic, false)) {
                Log.d("ERROR", "setNotification(false) failed for ${characteristic.uuid}")
                return
            }
            cccDescriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(cccDescriptor)
        }
    }

    private val scanFilter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(UUID.fromString(SERVICE_UUID)))
        .build()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val name: String? = result.scanRecord?.deviceName ?: result.device.name
            Log.d("MSG", "onScanResult name=$name address= ${result.device?.address}")
            safeStopBleScan()
            lifecycleState = BLELifecycleState.Connecting
            result.device.connectGatt(this@MainActivity, false, gattCallback)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            Log.d("MSG", "onBatchScanResults, ignoring")
        }

        override fun onScanFailed(errorCode: Int) {
            Log.d("MSG", "onScanFailed errorCode=$errorCode")
            safeStopBleScan()
            lifecycleState = BLELifecycleState.Disconnected
            bleRestartLifecycle(false)
        }
    }

    // BLE events
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            // TODO: timeout timer: if this callback not called - disconnect(), wait 120ms, close()

            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    // TODO: bonding state
                    // recommended on UI thread https://punchthrough.com/android-ble-guide/
                    Handler(Looper.getMainLooper()).post {
                        gatt.discoverServices()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d("MSG", "Disconnected from $deviceAddress")
                    setConnectedGattToNull()
                    gatt.close()
                    lifecycleState = BLELifecycleState.Disconnected
                    bleRestartLifecycle(false)
                }
            } else {
                // TODO: random error 133 - close() and try reconnect

                Log.d(
                    "ERROR",
                    "onConnectionStateChange status=$status deviceAddress=$deviceAddress, disconnecting"
                )

                setConnectedGattToNull()
                gatt.close()
                lifecycleState = BLELifecycleState.Disconnected
                bleRestartLifecycle(false)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.d("MSG", "onServicesDiscovered services.count=${gatt.services.size} status=$status")

            if (status == 129 /*GATT_INTERNAL_ERROR*/) {
                // it should be a rare case, this article recommends to disconnect:
                // https://medium.com/@martijn.van.welie/making-android-ble-work-part-2-47a3cdaade07
                Log.d("ERROR", "status=129 (GATT_INTERNAL_ERROR), disconnecting")
                gatt.disconnect()
                return
            }

            val service = gatt.getService(UUID.fromString(SERVICE_UUID)) ?: run {
                Log.d("ERROR", "Service not found $SERVICE_UUID, disconnecting")
                gatt.disconnect()
                return
            }

            // Get gatt + setup BluetoothQueue
            connectedGatt = gatt
            bluetoothQueue = BleQueue(connectedGatt!!)

            characteristicForRead = service.getCharacteristic(UUID.fromString(CHAR_FOR_READ_UUID))
            characteristicForStepMovementWrite =
                service.getCharacteristic(UUID.fromString(STEP_MOVEMENT_WRITE_UUID))
            characteristicForContinuousMovementWrite =
                service.getCharacteristic(UUID.fromString(CONTINUOUS_MOVEMENT_WRITE_UUID))
            characteristicForStackingStartWrite =
                service.getCharacteristic(UUID.fromString(START_STACKING_WRITE_UUID))
            characteristicForMotorPositionNotify =
                service.getCharacteristic(UUID.fromString(NOTIFY_CURRENT_MOTOR_POSITION_UUID))
            characteristicForStackingStepsTakenNotify = service.getCharacteristic(
                UUID.fromString(
                    NOTIFY_STACKING_STEPS_TAKEN_UUID
                )
            )

            // Subscribe to notification sends
            /*
            characteristicForMotorPositionNotify?.let {
                // lifecycleState = BLELifecycleState.ConnectedSubscribing
                subscribeToNotifications(it, gatt)
            } ?: run {
                Log.d("WARN", "characteristic not found ")
                lifecycleState = BLELifecycleState.Connected
            }
            */

            Log.d("UPDATE_MSG", "ONE")
            subscribeToNotifications(characteristicForStackingStepsTakenNotify!!, connectedGatt!!)
            Log.d("UPDATE_MSG", "TWO")
            subscribeToNotifications(characteristicForMotorPositionNotify!!, connectedGatt!!)
            Log.d("UPDATE_MSG", "THREE")
            lifecycleState = BLELifecycleState.Connected

            updateBluetoothToggleState()
        }

        // TODO: Remove
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (characteristic.uuid == UUID.fromString(CHAR_FOR_READ_UUID)) {
                val strValue = characteristic.value.toString(Charsets.UTF_8)
                val log = "onCharacteristicRead " + when (status) {
                    BluetoothGatt.GATT_SUCCESS -> "OK, value=\"$strValue\""
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> "not allowed"
                    else -> "error $status"
                }
                Log.d("MSG", log)
            } else {
                Log.d("MSG", "onCharacteristicRead unknown uuid $characteristic.uuid")
            }
        }


        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            Log.d("onDescriptorWrite:", descriptor.uuid.toString() + " status:" + status)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            when (characteristic.uuid) {
                UUID.fromString(NOTIFY_CURRENT_MOTOR_POSITION_UUID) -> {
                    val strValue = characteristic.value.toString(Charsets.UTF_8)
                    runOnUiThread {
                        sharedViewModel.setCurrentMotorPosition(strValue.toInt())
                    }
                }
                UUID.fromString(NOTIFY_STACKING_STEPS_TAKEN_UUID) -> {
                    val strValue = characteristic.value.toString(Charsets.UTF_8)
                    Log.d("NOTIFY_STACKING_STEPS_TAKEN update:", strValue)
                    runOnUiThread {
                        sharedViewModel.setStackingProgressRawText(strValue)
                    }
                }
            }
        }
    }

    //region BluetoothGattCharacteristic extension
    private fun BluetoothGattCharacteristic.isReadable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)

    private fun BluetoothGattCharacteristic.isWriteable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)

/*
fun BluetoothGattCharacteristic.isWriteableWithoutResponse(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)

fun BluetoothGattCharacteristic.isNotifiable(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)

fun BluetoothGattCharacteristic.isIndicatable(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_INDICATE)
*/

    private fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean {
        return (properties and property) != 0
    }

    //region Permissions and Settings management
    enum class AskType {
        AskOnce,
        InsistUntilSuccess
    }

    private var permissionResultHandlers =
        mutableMapOf<Int, (Array<out String>, IntArray) -> Unit>()
    private var bleOnOffListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)) {
                BluetoothAdapter.STATE_ON -> {
                    if (lifecycleState == BLELifecycleState.Disconnected) {
                        bleRestartLifecycle(false)
                    }
                }
                BluetoothAdapter.STATE_OFF -> {
                    bleEndLifecycle()
                }
            }
        }
    }

    // Keep displaying "enable bluetooth" prompt
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ENABLE_BLUETOOTH_REQUEST_CODE -> {
                if (resultCode != Activity.RESULT_OK) {
                    promptEnableBluetooth()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_DENIED) {
                    displaySnackbar("Failed to start scanning!", Snackbar.LENGTH_SHORT)
                } else {
                    // Permission granted; (re)start scan
                    safeStartBleScan()
                }
            }
        }
    }

/*
private fun grantLocationPermission(askType: AskType, completion: (Boolean) -> Unit) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || isLocationPermissionGranted) {
        completion(true)
    } else {
        runOnUiThread {
            val requestCode = LOCATION_PERMISSION_REQUEST_CODE
            val wantedPermission = android.Manifest.permission.ACCESS_FINE_LOCATION

            // prepare motivation message
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Location permission required")
            builder.setMessage("BLE advertising requires location access, starting from Android 6.0")
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                requestPermission(wantedPermission, requestCode)
            }
            builder.setCancelable(false)

            // set permission result handler
            permissionResultHandlers[requestCode] = { permissions, grantResults ->
                val isSuccess = grantResults.firstOrNull() != PackageManager.PERMISSION_DENIED
                if (isSuccess || askType != AskType.InsistUntilSuccess) {
                    permissionResultHandlers.remove(requestCode)
                    completion(isSuccess)
                } else {
                    // show motivation message again
                    builder.create().show()
                }
            }

            // show motivation message
            builder.create().show()
        }
    }
}
*/

    private fun Activity.requestPermission(permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
    }
}