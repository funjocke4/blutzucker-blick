// SPDX-License-Identifier: GPL-3.0-only
// Blutzucker-Blick: see LICENSE in the repository root.
package de.blutzuckerblick

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.widget.EditText
import android.widget.Spinner
import android.widget.RadioGroup
import android.widget.RadioButton
import android.widget.ArrayAdapter
import android.text.InputType
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ColorDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.view.View
import android.view.MotionEvent
import android.view.ViewConfiguration
import java.text.SimpleDateFormat
import java.util.Date
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothGattDescriptor
import java.util.UUID
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import android.widget.TextView
import android.widget.ImageView
import org.json.JSONArray
import org.json.JSONObject
import java.util.ArrayDeque
import java.util.Locale
import java.util.Calendar
import java.util.GregorianCalendar
import kotlin.math.roundToInt

/** BLE scan, GATT inspection and standard Glucose Service notification diagnostics. */
class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var deviceList: LinearLayout
    private lateinit var details: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val found = linkedMapOf<String, BluetoothDevice>()
    private val glucoseService = UUID.fromString("00001808-0000-1000-8000-00805f9b34fb")
    private val glucoseMeasurement = UUID.fromString("00002a18-0000-1000-8000-00805f9b34fb")
    private val cccd = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    private val contextUuid = UUID.fromString("00002a34-0000-1000-8000-00805f9b34fb")
    private val racpUuid = UUID.fromString("00002a52-0000-1000-8000-00805f9b34fb")
    private val subscriptions = ArrayDeque<Pair<BluetoothGattCharacteristic, ByteArray>>()
    private var records = 0
    private var scanning = false
    private var importing = false
    private var received = 0
    private lateinit var savedLabel: TextView
    private lateinit var summaryLabel: TextView
    private lateinit var hba1cTable: LinearLayout
    private lateinit var hba1cButton: Button
    private lateinit var recentValuesLabel: TextView
    private lateinit var recentValuesButton: Button
    private var recentValuesExpanded = false
    private var hba1cExpanded = false
    private lateinit var trendChart: TrendChart
    private lateinit var latestMood: TextView
    private val mealOptions = arrayOf(
        "Nicht zugeordnet", "Morgens nach dem Schlaf", "Vor dem Essen", "Nach dem Essen", "Zwischendurch"
    )
    private val mealLabels = arrayOf(
        "Nicht zugeordnet", "🌅 Morgens nach dem Schlaf",
        "🍽 Vor dem Essen",
        "✓ Nach dem Essen", "☀️ Zwischendurch"
    )
    // Bestehende Messwerte bleiben unangetastet; alte Kategorien erhalten nur
    // eine verständliche neue Anzeige. "Sonstiges" wird nicht mehr angeboten.
    private fun visibleMeal(value: String): String = when (value) {
        "Nüchtern" -> "Morgens nach dem Schlaf"
        "Sonstiges" -> "Nicht zugeordnet"
        else -> value
    }
    private val pendingMeals = mutableMapOf<Int, String>()
    private val chartPrefs by lazy { getSharedPreferences("chart_bounds", MODE_PRIVATE) }
    private var lowerLimit = 70.0
    private var upperLimit = 180.0
    private var fixedAxisMin: Double? = null
    private var fixedAxisMax: Double? = null
    private val liveScanRequestCode = 4108
    private var selectedPeriod = 0
    private val periodNames = arrayOf("Tag", "Woche", "Monat", "Jahr")
    private val periodButtons = mutableListOf<Button>()
    private var gatt: BluetoothGatt? = null
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val address = device.address
            val name = try { device.name ?: result.scanRecord?.deviceName ?: "Unbekannt" } catch (_: SecurityException) { "Unbekannt" }
            runOnUiThread {
                if (!scanning || found.containsKey(address)) return@runOnUiThread
                found[address] = device
                deviceList.addView(Button(this@MainActivity).apply {
                    text = "$name — $address"
                    isAllCaps = false
                    setOnClickListener { connect(device, name) }
                })
            }
        }
        override fun onScanFailed(errorCode: Int) { runOnUiThread { scanning = false; status.text = "Suche fehlgeschlagen: $errorCode" } }
    }
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, statusCode: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED && statusCode == BluetoothGatt.GATT_SUCCESS) {
                runOnUiThread { status.text = "Verbunden – Dienste werden gelesen …" }
                try { if (!g.discoverServices()) runOnUiThread { status.text = "Dienstsuche konnte nicht gestartet werden." } }
                catch (_: SecurityException) { runOnUiThread { status.text = "Bluetooth-Berechtigung fehlt." } }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED || statusCode != BluetoothGatt.GATT_SUCCESS) {
                runOnUiThread { status.text = "Verbindung getrennt (Status $statusCode)."; importing = false }
                g.close()
                if (gatt === g) gatt = null
            }
        }
        override fun onServicesDiscovered(g: BluetoothGatt, statusCode: Int) {
            runOnUiThread {
                if (statusCode == BluetoothGatt.GATT_SUCCESS) {
                    status.text = "Verbunden. ${g.services.size} GATT-Dienste erkannt."
                    details.text = g.services.joinToString("\n\n") { service ->
                        "Dienst: ${service.uuid}" + service.characteristics.joinToString("") { c ->
                            "\n  Merkmal: ${c.uuid} (Eigenschaften: ${c.properties})"
                        }
                    }
                    val glucose = g.getService(glucoseService)
                    val measurement = glucose?.getCharacteristic(glucoseMeasurement)
                    val racp = glucose?.getCharacteristic(racpUuid)
                    if (measurement == null || racp == null) {
                        details.append("\nGlucose-Dienst unvollständig: Messwert oder RACP fehlt.")
                    } else {
                        records = 0
                        received = 0
                        importing = false
                        subscriptions.clear()
                        pendingMeals.clear()
                        subscriptions.add(measurement to BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                        glucose.getCharacteristic(contextUuid)?.let {
                            subscriptions.add(it to BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                        }
                        subscriptions.add(racp to BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
                        subscribeNext(g)
                    }
                } else status.text = "Dienstsuche fehlgeschlagen: $statusCode"
            }
        }

    override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
        val copied = value.copyOf()
        runOnUiThread { processValue(characteristic.uuid, copied) }
    }
    @Suppress("DEPRECATION")
    override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        val copied = characteristic.value?.copyOf() ?: return
        runOnUiThread { processValue(characteristic.uuid, copied) }
    }
    private fun processValue(uuid: UUID, bytes: ByteArray) {
        if (uuid == glucoseMeasurement) {
            val result = decodeGlucose(bytes)
            if (result != null) {
                received++
                val (sequence, timestamp, concentration) = result
                val prefs = getSharedPreferences("glucose_records", MODE_PRIVATE)
                val array = try { JSONArray(prefs.getString("values", "[]")) } catch (_: Exception) { JSONArray() }
                val key = "$sequence:$timestamp"
                var exists = false
                for (i in 0 until array.length()) if (array.optJSONObject(i)?.optString("key") == key) exists = true
                if (!exists) {
                    val entry = JSONObject().put("key", key).put("sequence", sequence)
                        .put("date", timestamp).put("mgdl", concentration)
                    pendingMeals.remove(sequence)?.let { entry.put("meal", it) }
                    array.put(entry)
                    prefs.edit().putString("values", array.toString()).apply()
                    records++
                    updateSavedCount()
                    updateAnalysis()
                }
                log("\n$timestamp | ${concentration?.let { String.format(Locale.GERMANY, "%.1f mg/dL", it) } ?: "Wert nicht enthalten"} | Nr. $sequence")
            } else log("\nMesswertpaket nicht vollständig: ${bytes.toHex()}")
        } else if (uuid == racpUuid) {
            val op = bytes.getOrNull(0)?.toInt()?.and(255)
            val code = bytes.getOrNull(3)?.toInt()?.and(255)
            if (op == 0x06 && bytes.size >= 4) {
                importing = false
                val message = when (code) {
                    0x01 -> "Import abgeschlossen"
                    0x06 -> "Keine Datensätze vorhanden"
                    0x02 -> "Vorgang nicht unterstützt"
                    0x03 -> "Ungültiger Operator"
                    0x04 -> "Operator nicht unterstützt"
                    0x05 -> "Ungültige Anfrage"
                    0x07 -> "Fehler bei der Datensatzsuche"
                    0x08 -> "Import abgebrochen"
                    0x09 -> "Abbruch nicht erfolgreich"
                    else -> "RACP-Antwortcode $code"
                }
                status.text = "$message: $records neu, $received empfangen."
                log("\n$message | ${bytes.toHex()} | $records neue Datensätze gespeichert.")
            } else {
                log("\nRACP-Antwort: ${bytes.toHex()}")
            }
        } else if (uuid == contextUuid) {
            val context = decodeGlucoseContext(bytes)
            if (context == null) {
                log("\nMesswertkontext nicht vollständig: ${bytes.toHex()}")
            } else {
                val (sequence, meal) = context
                if (meal == null) {
                    log("\nMesswertkontext Nr. $sequence ohne Mahlzeitenkennzeichen.")
                } else {
                    pendingMeals[sequence] = meal
                    val prefs = getSharedPreferences("glucose_records", MODE_PRIVATE)
                    val arr = try { JSONArray(prefs.getString("values", "[]")) } catch (_: Exception) { JSONArray() }
                    // Gleiche Sequenznummer kann bei späterer Gerätehistorie erneut vorkommen.
                    // Nur den zuletzt passenden Datensatz zuordnen, nie manuelle Kennzeichen überschreiben.
                    for (i in arr.length() - 1 downTo 0) {
                        val row = arr.optJSONObject(i) ?: continue
                        if (row.optInt("sequence", -1) == sequence) {
                            val current = row.optString("meal", "Nicht zugeordnet")
                            if (current == "Nicht zugeordnet" || current.isBlank()) {
                                row.put("meal", meal)
                                prefs.edit().putString("values", arr.toString()).apply()
                                updateAnalysis()
                            }
                            pendingMeals.remove(sequence)
                            break
                        }
                    }
                    log("\nGL49-Kontext Nr. $sequence: $meal")
                }
            }
        }
    }
    private fun ByteArray.toHex() = joinToString(" ") { "%02X".format(it.toInt() and 255) }

    override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, statusCode: Int) {
        runOnUiThread {
            if (statusCode == BluetoothGatt.GATT_SUCCESS) subscribeNext(g)
            else { importing = false; status.text = "Bluetooth-Konfiguration fehlgeschlagen ($statusCode)"; log("\nCCCD-Schreiben fehlgeschlagen: $statusCode") }
        }
    }
    override fun onCharacteristicWrite(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, statusCode: Int) {
        if (characteristic.uuid == racpUuid && statusCode != BluetoothGatt.GATT_SUCCESS)
            runOnUiThread { importing = false; status.text = "Importanforderung fehlgeschlagen ($statusCode)"; log("\nRACP-Schreibfehler $statusCode") }
    }
    } // Ende BluetoothGattCallback

    // Zentraler Diagnose-Log: aus Activity und Bluetooth-Callback erreichbar.
    private fun log(message: String) = runOnUiThread { details.append(message) }

    // Als Activity-Methode verfügbar: für Callback und Schaltfläche „Messwerte erneut abrufen“.
    private fun subscribeNext(g: BluetoothGatt) {
        if (subscriptions.isEmpty()) {
            val racp = g.getService(glucoseService)?.getCharacteristic(racpUuid) ?: return
            if (importing) { log("\nImport läuft bereits."); return }
            importing = true
            received = 0
            records = 0
            // RACP: Report Stored Records / All records (kein Löschen).
            try {
                val command = byteArrayOf(0x01, 0x01)
                val started = if (Build.VERSION.SDK_INT >= 33)
                    g.writeCharacteristic(racp, command, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothGatt.GATT_SUCCESS
                else {
                    @Suppress("DEPRECATION")
                    racp.value = command
                    @Suppress("DEPRECATION")
                    g.writeCharacteristic(racp)
                }
                if (!started) { importing = false; status.text = "RACP-Anforderung konnte nicht gestartet werden." }
                log(if (started) "\nAlle gespeicherten Messwerte angefordert …" else "\nRACP-Anforderung konnte nicht gesendet werden.")
            } catch (e: SecurityException) { importing = false; log("\nBluetooth-Berechtigung fehlt.") }
            return
        }
        val (c, mode) = subscriptions.removeFirst()
        try {
            if (!g.setCharacteristicNotification(c, true)) { log("\nAktivierung fehlgeschlagen: ${c.uuid}"); status.text = "Benachrichtigungen konnten nicht aktiviert werden."; return }
            val d = c.getDescriptor(cccd) ?: run { log("\nCCCD fehlt: ${c.uuid}"); status.text = "CCCD fehlt."; return }
            val started = if (Build.VERSION.SDK_INT >= 33)
                g.writeDescriptor(d, mode) == BluetoothGatt.GATT_SUCCESS
            else {
                @Suppress("DEPRECATION")
                d.value = mode
                @Suppress("DEPRECATION")
                g.writeDescriptor(d)
            }
            if (!started) { log("\nCCCD-Schreibvorgang fehlgeschlagen: ${c.uuid}"); status.text = "Bluetooth-Aktivierung fehlgeschlagen." }
        } catch (e: SecurityException) { log("\nBluetooth-Berechtigung fehlt.") }
    }

    private data class Reading(val sequence: Int, val timestamp: String, val mgdl: Double?)
    private fun decodeGlucose(b: ByteArray): Reading? {
        if (b.size < 10) return null
        val flags = b[0].toInt() and 255
        fun u(i: Int) = b[i].toInt() and 255
        val seq = u(1) or (u(2) shl 8)
        val year = u(3) or (u(4) shl 8)
        val month = u(5); val day = u(6); val hour = u(7); val minute = u(8); val second = u(9)
        if (year !in 2000..2199 || month !in 1..12 || day !in 1..31 || hour > 23 || minute > 59 || second > 59) return null
        var offset = 10
        var minutesOffset = 0
        if (flags and 0x01 != 0) {
            if (b.size < offset + 2) return null
            val rawOffset = u(offset) or (u(offset + 1) shl 8)
            minutesOffset = rawOffset.toShort().toInt()
            if (minutesOffset == -32768) minutesOffset = 0 // unbekannt
            offset += 2
        }
        val calendar = GregorianCalendar().apply {
            isLenient = false
            set(year, month - 1, day, hour, minute, second)
            set(Calendar.MILLISECOND, 0)
        }
        val timestamp = try {
            calendar.add(Calendar.MINUTE, minutesOffset)
            String.format(Locale.ROOT, "%04d-%02d-%02d %02d:%02d:%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND))
        } catch (_: IllegalArgumentException) { return null }
        var mgdl: Double? = null
        if (flags and 0x02 != 0) {
            if (b.size < offset + 3) return null
            val raw = u(offset) or (u(offset+1) shl 8)
            var mantissa = raw and 0x0fff
            if (mantissa >= 0x800) mantissa -= 0x1000
            var exponent = (raw ushr 12) and 0x0f
            if (exponent >= 8) exponent -= 16
            // Reserved IEEE-11073 SFLOAT values are not numeric measurements.
            if (mantissa !in listOf(0x7ff,0x7fe,0x7fd,-2048,-2047,-2046)) {
                val quantity = mantissa * Math.pow(10.0, exponent.toDouble())
                mgdl = if (flags and 0x04 != 0) quantity * 18015.59 else quantity * 100000.0
                if (!mgdl.isFinite() || mgdl < 0) mgdl = null
            }
        }
        return Reading(seq, timestamp, mgdl)
    }


    /** Bluetooth SIG Glucose Measurement Context (0x2A34), optionales Meal-Feld. */
    private fun decodeGlucoseContext(bytes: ByteArray): Pair<Int, String?>? {
        if (bytes.size < 3) return null
        fun u(index: Int) = bytes[index].toInt() and 0xff
        val flags = u(0)
        val sequence = u(1) or (u(2) shl 8)
        var offset = 3
        if (flags and 0x01 != 0) offset += 1  // Extended flags
        if (flags and 0x02 != 0) offset += 3  // Carbohydrate ID + SFLOAT
        if (flags and 0x04 == 0) return sequence to null
        if (bytes.size <= offset) return null
        val meal = when (u(offset)) {
            1 -> "Vor dem Essen"
            2 -> "Nach dem Essen"
            3 -> "Nüchtern"
            4 -> "Zwischendurch"
            5 -> null // Vor dem Schlafengehen; keine passende manuelle Kategorie
            else -> null
        }
        return sequence to meal
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lowerLimit = chartPrefs.getFloat("lower", 70f).toDouble()
        upperLimit = chartPrefs.getFloat("upper", 180f).toDouble()
        fixedAxisMin = if (chartPrefs.contains("axisMin")) chartPrefs.getFloat("axisMin", 0f).toDouble() else null
        fixedAxisMax = if (chartPrefs.contains("axisMax")) chartPrefs.getFloat("axisMax", 0f).toDouble() else null
        val density = resources.displayMetrics.density
        val padding = (16 * density).toInt()
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }
        scroll.addView(root)
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        header.addView(TextView(this).apply {
            text = "Blutzucker Blick"
            textSize = 25f
            setTextColor(Color.rgb(20, 103, 123))
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(Button(this).apply {
            text = "Menü"
            contentDescription = "Menü öffnen: Bluetooth, Hilfe und Zuordnung vorhandener Werte"
            textSize = 16f
            isAllCaps = false
            setTextColor(Color.rgb(20, 103, 123))
            setOnClickListener { showMainMenu() }
        }, LinearLayout.LayoutParams((90 * density).toInt(), (52 * density).toInt()))
        root.addView(header)
        root.addView(TextView(this).apply {
            text = "Messwert erfassen"
            textSize = 21f
            setPadding(0, (16 * density).toInt(), 0, (6 * density).toInt())
        })
        val entryButtons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        entryButtons.addView(Button(this).apply {
            text = "📝"
            textSize = 28f
            isAllCaps = false
            contentDescription = "Messwert manuell eingeben"
            setOnClickListener { showReadingEditor(null) }
        }, LinearLayout.LayoutParams(0, (66 * density).toInt(), 1f).apply {
            marginEnd = (5 * density).toInt()
        })
        entryButtons.addView(Button(this).apply {
            text = "📷"
            textSize = 28f
            isAllCaps = false
            contentDescription = "Display automatisch erkennen"
            setOnClickListener { launchLiveScanner() }
        }, LinearLayout.LayoutParams(0, (66 * density).toInt(), 1f).apply {
            marginStart = (5 * density).toInt()
        })
        root.addView(entryButtons)
        status = TextView(this).apply { text = "Noch nicht verbunden."; textSize = 14f }
        root.addView(status)
        deviceList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(deviceList)
        savedLabel = TextView(this).apply { textSize = 15f }
        root.addView(savedLabel)
        updateSavedCount()
        latestMood = TextView(this).apply {
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(103, 58, 183)) // Markierung des neuesten Messwerts
            setPadding(0, (8 * density).toInt(), 0, (6 * density).toInt())
        }
        root.addView(latestMood)
        root.addView(TextView(this).apply {
            text = "Messwertübersicht"
            textSize = 21f
            setPadding(0, padding, 0, 0)
        })
        val choices = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (i in periodNames.indices) {
            choices.addView(Button(this).apply {
                text = periodNames[i]
                isAllCaps = false
                textSize = 12f
                setOnClickListener { selectedPeriod = i; recentValuesExpanded = false; updateAnalysis() }
                periodButtons.add(this)
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        root.addView(choices)
        summaryLabel = TextView(this).apply { textSize = 15f; setPadding(0, 10, 0, 8) }
        root.addView(summaryLabel)
        // Die Kurve zeichnet ihre eigene verschiebbare Zeitachse. Das ist zuverlässiger
        // als eine HorizontalScrollView innerhalb der vertikal scrollbaren Hauptseite.
        trendChart = TrendChart()
        root.addView(trendChart, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (230 * density).toInt()))
        hba1cButton = Button(this).apply {
            text = "Geschätzter HbA1c: —"
            isAllCaps = false
            textSize = 17f
            setPadding(0, padding, 0, 0)
            setOnClickListener {
                hba1cExpanded = !hba1cExpanded
                updateHba1cVisibility()
            }
        }
        root.addView(hba1cButton)
        hba1cTable = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        root.addView(hba1cTable)
        recentValuesButton = Button(this).apply {
            text = "Letzte Messwerte im Zeitraum anzeigen"
            isAllCaps = false
            contentDescription = "Letzte Messwerte im gewählten Zeitraum anzeigen oder ausblenden"
            setOnClickListener {
                recentValuesExpanded = !recentValuesExpanded
                updateRecentValuesVisibility()
            }
        }
        root.addView(recentValuesButton)
        recentValuesLabel = TextView(this).apply {
            textSize = 14f
            setPadding((8 * density).toInt(), (4 * density).toInt(), 0, (12 * density).toInt())
            visibility = View.GONE
        }
        root.addView(recentValuesLabel)
        // Das Diagnoseprotokoll ist nur über „Menü → Bluetooth & Messwerte → Diagnose“
        // sichtbar. Es bleibt als TextView erhalten, damit BLE-Ereignisse weiter
        // protokolliert werden, auch wenn der Dialog geschlossen ist.
        details = TextView(this).apply { textSize = 12f; setTextIsSelectable(true) }
        root.addView(TextView(this).apply {
            text = "Messwerte werden lokal gespeichert. Die HbA1c-Schätzung ist kein Laborwert und nicht für Therapieentscheidungen geeignet. Die Ziffernerkennung läuft lokal und ohne Internet; bitte Werte immer prüfen."
            textSize = 12f
        })
        setContentView(scroll)
        updateAnalysis()
    }

    private fun launchLiveScanner() {
        val intent = Intent(this, AutoScanActivity::class.java)
        @Suppress("DEPRECATION")
        startActivityForResult(intent, liveScanRequestCode)
    }

    @Deprecated("Android Activity Result callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != liveScanRequestCode || resultCode != RESULT_OK) return
        val recognized = data?.getIntExtra(AutoScanActivity.EXTRA_VALUE, -1) ?: -1
        if (recognized in 20..600) {
            val stamp = data?.getLongExtra(AutoScanActivity.EXTRA_TIME, System.currentTimeMillis())
                ?: System.currentTimeMillis()
            showReadingEditor(recognized.toDouble(), stamp,
                "Live-Erkennung: Zahl bitte mit dem Messgerät vergleichen. Nur nach Prüfung speichern.")
        }
    }

    private fun mealSpinner(initial: String = "Nicht zugeordnet"): Spinner = Spinner(this).apply {
        adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, mealLabels)
        setSelection(mealOptions.indexOf(visibleMeal(initial)).coerceAtLeast(0))
    }

    private fun showReadingEditor(prefill: Double?, timestamp: Long = System.currentTimeMillis(), note: String = "") {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val space = (20 * resources.displayMetrics.density).toInt()
            setPadding(space, space / 2, space, space / 2)
        }
        layout.addView(TextView(this).apply {
            text = "Blutzuckerwert in mg/dL"
            textSize = 14f
            setPadding(0, 0, 0, (6 * resources.displayMetrics.density).toInt())
        })
        val valueInput = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setSingleLine(true)
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            textSize = 24f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setText(prefill?.let { String.format(Locale.ROOT, "%.0f", it) } ?: "")
        }
        layout.addView(valueInput)
        // Für jedes neue Kamera- oder manuell erfasste Ergebnis muss bewusst
        // ein Essenszeitpunkt gewählt werden. Kein unsichtbarer Standardwert.
        layout.addView(TextView(this).apply {
            text = "Wann wurde gemessen? Bitte auswählen:"
            textSize = 16f
            setPadding(0, (12 * resources.displayMetrics.density).toInt(), 0, 0)
        })
        val mealChoices = listOf(
            "Vor dem Essen" to "🍽 Vor dem Essen",
            "Nach dem Essen" to "✓ Nach dem Essen",
            "Zwischendurch" to "☀️ Zwischendurch",
            "Morgens nach dem Schlaf" to "🌅 Morgens nach dem Schlaf"
        )
        val mealGroup = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        val mealIds = mutableMapOf<Int, String>()
        mealChoices.forEach { (value, caption) ->
            val option = RadioButton(this).apply {
                id = View.generateViewId()
                text = caption
                textSize = 15f
                setPadding(0, (5 * resources.displayMetrics.density).toInt(), 0,
                    (5 * resources.displayMetrics.density).toInt())
            }
            mealIds[option.id] = value
            mealGroup.addView(option)
        }
        layout.addView(mealGroup)
        val dialogContent = ScrollView(this).apply {
            isFillViewport = false
            addView(layout)
        }
        val dialog = AlertDialog.Builder(this).setTitle("Messwert prüfen & speichern")
            .setView(dialogContent).setNegativeButton("Abbrechen", null)
            .setPositiveButton("Speichern", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val glucose = valueInput.text.toString().replace(',', '.').toDoubleOrNull()
                if (glucose == null || glucose < 20.0 || glucose > 600.0) {
                    valueInput.error = "Bitte gültigen Wert (20–600 mg/dL) eingeben"
                    return@setOnClickListener
                }
                val selectedMeal = mealIds[mealGroup.checkedRadioButtonId]
                if (selectedMeal == null) {
                    Toast.makeText(this, "Bitte einen Messzeitpunkt auswählen", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val prefs = getSharedPreferences("glucose_records", MODE_PRIVATE)
                val array = try { JSONArray(prefs.getString("values", "[]")) } catch (_: Exception) { JSONArray() }
                val key = "photo-manual:$timestamp"
                if ((0 until array.length()).any { array.optJSONObject(it)?.optString("key") == key }) {
                    valueInput.error = "Dieser Eintrag existiert bereits"
                    return@setOnClickListener
                }
                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date(timestamp))
                array.put(JSONObject().put("key", key).put("date", date)
                    .put("mgdl", glucose).put("meal", selectedMeal)
                    .put("source", if (prefill != null) "Kamera" else "Manuell"))
                // Ein synchroner Commit bestätigt, dass der Eintrag wirklich gespeichert wurde.
                val saved = prefs.edit().putString("values", array.toString()).commit()
                if (!saved) {
                    valueInput.error = "Speichern fehlgeschlagen. Bitte erneut versuchen."
                    Toast.makeText(this, "Messwert konnte nicht gespeichert werden", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                updateSavedCount(); updateAnalysis(); dialog.dismiss()
                Toast.makeText(this, "Messwert gespeichert", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    /** Nur optische Rückmeldung auf Basis der selbst eingestellten Grenzwerte. */
    private fun moodFor(mgdl: Double): String {
        val tolerance = 20.0 // optische Abstufung in mg/dL, keine medizinische Schwelle
        return when {
            mgdl < lowerLimit - tolerance -> "😟 Deutlich unter dem eingestellten Bereich"
            mgdl < lowerLimit -> "😐 Leicht unter dem eingestellten Bereich"
            mgdl > upperLimit + tolerance -> "😲 Deutlich über dem eingestellten Bereich"
            mgdl > upperLimit -> "😐 Leicht über dem eingestellten Bereich"
            else -> "😊 Im eingestellten Bereich"
        }
    }

    private fun showHelp() {
        val teal = Color.rgb(10, 111, 104)
        val dark = Color.rgb(17, 68, 68)
        val muted = Color.rgb(90, 115, 115)
        val dp = resources.displayMetrics.density
        fun panelBackground(color: Int, radius: Float, stroke: Boolean = false) =
            GradientDrawable().apply {
                setColor(color)
                cornerRadius = radius * dp
                if (stroke) setStroke((1 * dp).toInt().coerceAtLeast(1), Color.rgb(216, 227, 223))
            }
        val dialog = Dialog(this)
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = panelBackground(Color.rgb(248, 251, 249), 28f)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding((22*dp).toInt(), (18*dp).toInt(), (14*dp).toInt(), (14*dp).toInt())
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            text = "Hilfe"
            textSize = 24f
            setTextColor(dark)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(TextView(this).apply {
            text = "×"
            contentDescription = "Hilfe schließen"
            textSize = 33f
            gravity = android.view.Gravity.CENTER
            setTextColor(dark)
            setOnClickListener { dialog.dismiss() }
        }, LinearLayout.LayoutParams((52*dp).toInt(), (52*dp).toInt()))
        outer.addView(header)
        outer.addView(View(this).apply { setBackgroundColor(Color.rgb(217, 229, 224)) },
            LinearLayout.LayoutParams(-1, (1*dp).toInt().coerceAtLeast(1)))
        val scroll = ScrollView(this).apply { isFillViewport = false }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((22*dp).toInt(), (20*dp).toInt(), (22*dp).toInt(), (20*dp).toInt())
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((18*dp).toInt(), (20*dp).toInt(), (18*dp).toInt(), (20*dp).toInt())
            background = panelBackground(Color.WHITE, 22f, true)
        }
        card.addView(TextView(this).apply {
            text = "ÜBER DIESE APP"
            textSize = 14f
            setTextColor(muted)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        val aboutHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, (16*dp).toInt(), 0, (12*dp).toInt())
        }
        aboutHeader.addView(ImageView(this).apply {
            setImageResource(R.drawable.blood_drop_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            background = panelBackground(Color.TRANSPARENT, 30f)
        }, LinearLayout.LayoutParams((78*dp).toInt(), (78*dp).toInt()))
        val aboutTitle = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((15*dp).toInt(), 0, 0, 0)
        }
        aboutTitle.addView(TextView(this).apply {
            text = "Blutzucker Blick"
            textSize = 19f
            setTextColor(dark)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        aboutTitle.addView(TextView(this).apply {
            text = "Version 1.3.4"
            textSize = 14f
            setTextColor(muted)
        })
        aboutHeader.addView(aboutTitle)
        card.addView(aboutHeader)
        card.addView(TextView(this).apply {
            text = "Blutzuckerwerte per Bluetooth, Kamera oder manuell erfassen und übersichtlich auswerten. Die Messwerte werden lokal im App-Speicher abgelegt."
            textSize = 15f
            setTextColor(muted)
            setPadding(0, (10*dp).toInt(), 0, (16*dp).toInt())
        })
        card.addView(View(this).apply { setBackgroundColor(Color.rgb(219, 228, 223)) },
            LinearLayout.LayoutParams(-1, (1*dp).toInt().coerceAtLeast(1)))
        card.addView(TextView(this).apply {
            text = "Open-Source-Projekt Blutzucker-Blick"
            textSize = 15f
            setTextColor(dark)
            setPadding(0, (14*dp).toInt(), 0, (10*dp).toInt())
        })
        card.addView(TextView(this).apply {
            text = "Lizenz: GNU GPL v3 (GPL-3.0-only). Quellcode und Lizenztext sind im Projekt-Repository verfügbar. Die Software wird ohne Gewährleistung bereitgestellt. Ziffernerkennung: offline, ohne externe OCR-Bibliothek."
            textSize = 14f
            setTextColor(muted)
        })
        body.addView(card)
        fun helpSection(title: String, description: String) {
            body.addView(TextView(this).apply {
                text = title
                textSize = 18f
                setTextColor(dark)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(0, (25*dp).toInt(), 0, (10*dp).toInt())
            })
            body.addView(TextView(this).apply {
                text = description
                textSize = 15f
                setTextColor(muted)
                setLineSpacing(4f * dp, 1f)
            })
        }
        helpSection("Messwert erfassen",
            "Unter 'Messwert erfassen' öffnet 📝 die manuelle Eingabe und 📷 die Live-Kamera für das Display. Halte die Ziffern in den Rahmen; die Erkennung prüft sie mehrfach. Kontrolliere die Zahl, wähle 'Vor dem Essen', 'Nach dem Essen', 'Zwischendurch' oder 'Morgens nach dem Schlaf' und tippe auf 'Speichern'. Ohne diese Auswahl wird nicht gespeichert. Über das Menü oben rechts kannst du vorhandene Werte nachträglich zuordnen.")
        helpSection("Essenszeitpunkt",
            "🍽 Vor dem Essen. ✓ Nach dem Essen. ☀️ Zwischendurch = unabhängig von einer Mahlzeit. 🌅 Morgens nach dem Schlaf = unmittelbar nach dem Aufstehen. Bitte den Zeitpunkt beim Speichern kontrollieren.")
        helpSection("Bluetooth verbinden",
            "Öffne 'Menü → Bluetooth & Messwerte' und wähle 'Geräte suchen & verbinden'. Aktiviere die Bluetooth-Übertragung am Messgerät gemäß seiner Anleitung und bestätige bei Bedarf die Kopplung. Danach lassen sich unterstützte Messwerte abrufen.")
        helpSection("Diagramme und Grenzwerte",
            "Tag, Woche, Monat oder Jahr auswählen. Diagramm nach links und rechts wischen oder die Pfeiltasten verwenden. Tippe auf 'Letzte Messwerte im Zeitraum anzeigen', um die sechs neuesten Werte des gewählten Zeitraums aufzuklappen. Im Menü 'Grenzwerte & Skala' kannst du deine eigenen Warnschwellen festlegen. 😊 liegt innerhalb des Bereichs, 😐 knapp darüber oder darunter, 😲 deutlich darüber. Niedrige Werte werden ebenfalls gesondert markiert. 🍬 unter der Untergrenze und 🏃 über der Obergrenze sind nur optische Warnsymbole, keine Empfehlungen zur Behandlung oder Bewegung.")
        helpSection("Geschätzter HbA1c",
            "Der Button 'Geschätzter HbA1c' zeigt direkt den aktuellen Schätzwert. Beim Antippen werden die Details der 90-Tage-Auswertung eingeblendet. Diese Schätzung ist nur eine mathematische Näherung aus vorhandenen Einzelmessungen und ersetzt keinen Labor-HbA1c.")
        scroll.addView(body)
        outer.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val footer = LinearLayout(this).apply {
            setPadding((22*dp).toInt(), (12*dp).toInt(), (22*dp).toInt(), (16*dp).toInt())
        }
        footer.addView(Button(this).apply {
            text = "Verstanden"
            isAllCaps = false
            textSize = 17f
            setTextColor(Color.WHITE)
            background = panelBackground(teal, 16f)
            setOnClickListener { dialog.dismiss() }
        }, LinearLayout.LayoutParams(-1, (58*dp).toInt()))
        outer.addView(footer)
        dialog.setContentView(outer)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.94).toInt(),
            (resources.displayMetrics.heightPixels * 0.88).toInt())
    }

    private fun selectReadingForMeal() {
        val prefs = getSharedPreferences("glucose_records", MODE_PRIVATE)
        val array = try { JSONArray(prefs.getString("values", "[]")) } catch (_: Exception) { JSONArray() }
        val candidates = (0 until array.length()).mapNotNull { i ->
            array.optJSONObject(i)?.takeIf { it.has("mgdl") && !it.isNull("mgdl") }?.let { i to it }
        }.takeLast(30).asReversed()
        if (candidates.isEmpty()) {
            AlertDialog.Builder(this).setMessage("Noch keine Messwerte vorhanden.").setPositiveButton("OK", null).show()
            return
        }
        val labels = candidates.map { (_, entry) ->
            "${entry.optString("date")} · ${entry.optString("mgdl")} mg/dL · ${visibleMeal(entry.optString("meal", "Nicht zugeordnet"))}"
        }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Messwert auswählen").setItems(labels) { _, choice ->
            val entry = candidates[choice].second
            val spinner = mealSpinner(entry.optString("meal", "Nicht zugeordnet"))
            AlertDialog.Builder(this).setTitle("Essenszeitpunkt zuordnen")
                .setView(spinner).setNegativeButton("Abbrechen", null)
                .setPositiveButton("Speichern") { _, _ ->
                    entry.put("meal", mealOptions[spinner.selectedItemPosition])
                    prefs.edit().putString("values", array.toString()).apply()
                    updateAnalysis()
                }.show()
        }.show()
    }

    private fun showMainMenu() {
        val options = arrayOf(
            "Bluetooth & Messwerte",
            "Hilfe",
            "Diagramm-Grenzwerte & Skala",
            "Essenszeitpunkt vorhandener Werte ändern"
        )
        AlertDialog.Builder(this)
            .setTitle("Menü")
            .setItems(options) { _, selected ->
                when (selected) {
                    0 -> showBluetoothMenu()
                    1 -> showHelp()
                    2 -> showChartSettings()
                    3 -> selectReadingForMeal()
                }
            }
            .setNegativeButton("Schließen", null)
            .show()
    }

    private fun showBluetoothMenu() {
        val items = arrayOf("Geräte suchen & verbinden", "Messwerte erneut abrufen", "Verbindung trennen", "Verbindungsanleitung", "Diagnose")
        AlertDialog.Builder(this)
            .setTitle("Bluetooth & Messwerte")
            .setItems(items) { _, selected ->
                when (selected) {
                    0 -> startScan()
                    1 -> {
                        val active = gatt
                        if (active?.getService(glucoseService)?.getCharacteristic(racpUuid) != null && subscriptions.isEmpty()) {
                            subscribeNext(active)
                        } else {
                            status.text = "Bitte erst verbinden und die Einrichtung abwarten."
                        }
                    }
                    2 -> disconnect()
                    3 -> AlertDialog.Builder(this)
                        .setTitle("Verbindungsanleitung")
                        .setMessage("1. Bluetooth am Handy einschalten.\n2. Am Messgerät die Bluetooth-Übertragung gemäß dessen Bedienungsanleitung aktivieren.\n3. 'Geräte suchen & verbinden' wählen und das gewünschte Gerät antippen.\n4. Falls erforderlich die Kopplung bestätigen und anschließend Messwerte abrufen.\n\nHinweis: Die Verbindungsschritte unterscheiden sich je nach Messgerät.")
                        .setPositiveButton("OK", null)
                        .show()
                    4 -> showBluetoothDiagnostics()
                }
            }
            .setNegativeButton("Schließen", null)
            .show()
    }

    private fun showBluetoothDiagnostics() {
        val padding = (16 * resources.displayMetrics.density).toInt()
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }
        content.addView(TextView(this).apply {
            text = "Verbindungsstatus: ${status.text}\n\nDiagnoseprotokoll:"
            textSize = 14f
            setPadding(0, 0, 0, padding / 2)
        })
        // Das bestehende Protokoll direkt anzeigen: Neue Bluetooth-Meldungen
        // erscheinen dadurch auch, solange der Diagnose-Dialog geöffnet ist.
        (details.parent as? android.view.ViewGroup)?.removeView(details)
        if (details.text.isNullOrBlank()) {
            content.addView(TextView(this).apply {
                text = "Noch keine Diagnosedaten. Zuerst ein Messgerät suchen oder verbinden."
                textSize = 12f
            })
        }
        content.addView(details)
        val scroll = ScrollView(this).apply { addView(content) }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Diagnose")
            .setView(scroll)
            .setPositiveButton("Schließen", null)
            .create()
        dialog.setOnDismissListener { content.removeView(details) }
        dialog.show()
    }

    private data class ValuePoint(val time: Long, val mgdl: Double, val meal: String = "Nicht zugeordnet")

    private fun savedReadings(): List<ValuePoint> {
        val prefs = getSharedPreferences("glucose_records", MODE_PRIVATE)
        val entries = try { JSONArray(prefs.getString("values", "[]")) } catch (_: Exception) { JSONArray() }
        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).apply { isLenient = false }
        val output = mutableListOf<ValuePoint>()
        for (i in 0 until entries.length()) {
            val item = entries.optJSONObject(i) ?: continue
            val value = item.optDouble("mgdl", Double.NaN)
            val date = try { parser.parse(item.optString("date")) } catch (_: Exception) { null }
            if (date != null && value.isFinite() && value > 0 && value < 1500.0)
                output.add(ValuePoint(date.time, value, item.optString("meal", "Nicht zugeordnet")))
        }
        return output.sortedBy { it.time }
    }

    private fun showChartSettings() {
        val padding = (18 * resources.displayMetrics.density).toInt()
        val holder = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, 8, padding, 0)
        }
        fun numericField(title: String, value: Double?): EditText {
            holder.addView(TextView(this).apply { text = title; textSize = 14f })
            return EditText(this).apply {
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                setSingleLine(true)
                setText(value?.let { String.format(Locale.ROOT, "%.0f", it) } ?: "")
                holder.addView(this)
            }
        }
        val low = numericField("Untere Warnschwelle (mg/dL)", lowerLimit)
        val high = numericField("Obere Warnschwelle (mg/dL)", upperLimit)
        holder.addView(TextView(this).apply {
            text = "Optionale Diagrammskala: Felder leer lassen für automatische Anpassung."
            textSize = 13f
        })
        val axisMin = numericField("Diagramm-Minimum (leer = automatisch)", fixedAxisMin)
        val axisMax = numericField("Diagramm-Maximum (leer = automatisch)", fixedAxisMax)
        val dialog = AlertDialog.Builder(this).setTitle("Grenzwerte & Diagrammskala")
            .setView(holder).setNegativeButton("Abbrechen", null)
            .setPositiveButton("Speichern", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val l = low.text.toString().replace(',', '.').toDoubleOrNull()
                val h = high.text.toString().replace(',', '.').toDoubleOrNull()
                val a = axisMin.text.toString().replace(',', '.').takeIf { it.isNotBlank() }?.toDoubleOrNull()
                val b = axisMax.text.toString().replace(',', '.').takeIf { it.isNotBlank() }?.toDoubleOrNull()
                if (l == null || h == null || l !in 20.0..500.0 || h !in 21.0..600.0 || l >= h) {
                    low.error = "Grenzen: 20–600 mg/dL; untere Grenze muss kleiner sein"
                    return@setOnClickListener
                }
                if ((axisMin.text.isNotBlank() && a == null) || (axisMax.text.isNotBlank() && b == null) ||
                    (a != null && a !in 0.0..600.0) || (b != null && b !in 20.0..1000.0) ||
                    (a != null && b != null && a >= b) || (a != null && a > l) || (b != null && b < h)) {
                    axisMin.error = "Diagrammskala prüfen: Minimum < Maximum und Warnschwellen einschließen"
                    return@setOnClickListener
                }
                lowerLimit = l; upperLimit = h; fixedAxisMin = a; fixedAxisMax = b
                chartPrefs.edit().putFloat("lower", l.toFloat()).putFloat("upper", h.toFloat())
                    .apply { if (a == null) remove("axisMin") else putFloat("axisMin", a.toFloat()) }
                    .apply { if (b == null) remove("axisMax") else putFloat("axisMax", b.toFloat()) }
                    .apply()
                updateAnalysis()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun updateRecentValuesVisibility() {
        if (!::recentValuesButton.isInitialized || !::recentValuesLabel.isInitialized) return
        val hasValues = recentValuesLabel.text.isNotBlank()
        if (!hasValues) recentValuesExpanded = false
        recentValuesLabel.visibility = if (recentValuesExpanded && hasValues) View.VISIBLE else View.GONE
        val action = if (recentValuesExpanded) "ausblenden ▲" else "anzeigen ▼"
        recentValuesButton.text = "Letzte Messwerte im Zeitraum $action"
        recentValuesButton.isEnabled = hasValues
    }

    private fun updateAnalysis() {
        if (!::summaryLabel.isInitialized) return
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        when (selectedPeriod) {
            0 -> cal.set(Calendar.HOUR_OF_DAY, 0)
            1 -> {
                cal.firstDayOfWeek = Calendar.MONDAY
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                cal.set(Calendar.HOUR_OF_DAY, 0)
            }
            2 -> { cal.set(Calendar.DAY_OF_MONTH, 1); cal.set(Calendar.HOUR_OF_DAY, 0) }
            else -> { cal.set(Calendar.DAY_OF_YEAR, 1); cal.set(Calendar.HOUR_OF_DAY, 0) }
        }
        cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        val readings = savedReadings()
        val period = readings.filter { it.time >= start && it.time <= now }
        val periodTitle = if (selectedPeriod == 0) {
            "Tag · " + SimpleDateFormat("EEEE, dd.MM.yyyy", Locale.GERMANY).format(Date(now))
        } else periodNames[selectedPeriod]
        periodButtons.forEachIndexed { i, button -> button.isEnabled = i != selectedPeriod }
        if (period.isEmpty()) {
            summaryLabel.text = "$periodTitle: Keine gespeicherten Messwerte in diesem Zeitraum."
            recentValuesLabel.text = ""
        } else {
            val average = period.map { it.mgdl }.average()
            val minValue = period.minOf { it.mgdl }
            val maxValue = period.maxOf { it.mgdl }
            summaryLabel.text = String.format(Locale.GERMANY,
                "%s · %d Messwerte\nDurchschnitt: %.1f mg/dL\nMinimum: %.1f mg/dL     Maximum: %.1f mg/dL",
                periodTitle, period.size, average, minValue, maxValue)
            // In der Tagesansicht Datum und Wochentag bei jedem Messwert anzeigen.
            val formatter = SimpleDateFormat(
                if (selectedPeriod == 0) "EEEE, dd.MM.yyyy HH:mm" else "dd.MM.yyyy HH:mm",
                Locale.GERMANY
            )
            recentValuesLabel.text = period.takeLast(6).asReversed()
                .joinToString("\n") { "${formatter.format(Date(it.time))}: ${String.format(Locale.GERMANY, "%.1f", it.mgdl)} mg/dL · ${visibleMeal(it.meal)} · ${moodFor(it.mgdl).substringBefore(' ')}" }
        }
        updateRecentValuesVisibility()
        // Jedes Aktualisieren zeigt zunächst die neuesten Daten; danach bleibt die
        // Diagrammposition beim Wischen oder per Pfeiltaste unabhängig veränderbar.
        trendChart.setPoints(period)
        val newest = readings.lastOrNull()
        if (newest == null) {
            latestMood.text = "Noch keine gespeicherten Messwerte"
        } else {
            val smiley = moodFor(newest.mgdl).substringBefore(' ')
            // Zusätzliche Symbole orientieren sich exakt an den einstellbaren
            // Unter- und Obergrenzen, unabhängig von der Smiley-Toleranzstufe.
            val symbols = ReadingStatusIcons.forReading(newest.mgdl, lowerLimit, upperLimit, smiley)
            val label = "Letzter Wert: ${String.format(Locale.GERMANY, "%.0f", newest.mgdl)} mg/dL  $symbols"
            latestMood.text = SpannableString(label).apply {
                val start = label.lastIndexOf(symbols)
                // Bisher waren die Smileys 2,0x groß; alle Symbole jetzt 20 % größer.
                if (start >= 0) setSpan(RelativeSizeSpan(2.4f), start, start + symbols.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        val ninetyDaysAgo = now - 90L * 24 * 60 * 60 * 1000
        val last90 = readings.filter { it.time in ninetyDaysAgo..now }
        hba1cTable.removeAllViews()
        if (last90.isEmpty()) {
            hba1cButton.text = "Geschätzter HbA1c: —"
            addTableRow(hba1cTable, "Letzte 90 Tage", "Keine Messwerte")
        } else {
            val mean = last90.map { it.mgdl }.average()
            val estimate = (mean + 46.7) / 28.7
            hba1cButton.text = "Geschätzter HbA1c: " + String.format(Locale.GERMANY, "%.1f %%", estimate)
            val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
            val coverage = last90.map { dayFormat.format(Date(it.time)) }.distinct().size
            addTableRow(hba1cTable, "HbA1c (Schätzung)", String.format(Locale.GERMANY, "%.1f %%", estimate))
            addTableRow(hba1cTable, "Ø Blutzucker", String.format(Locale.GERMANY, "%.1f mg/dL", mean))
            addTableRow(hba1cTable, "Messwerte", "${last90.size}")
            addTableRow(hba1cTable, "Messtage", "$coverage")
            addTableRow(hba1cTable, "Zeitraum", "Letzte 90 Tage")
            val monthKey = SimpleDateFormat("yyyy-MM", Locale.ROOT)
            val monthTitle = SimpleDateFormat("MMMM yyyy", Locale.GERMANY)
            val monthly = last90.groupBy { monthKey.format(Date(it.time)) }.toSortedMap(reverseOrder())
            hba1cTable.addView(TextView(this).apply {
                text = "Monatsübersicht (Schätzwerte)"
                textSize = 16f
                setPadding(0, 16, 0, 8)
            })
            for ((_, values) in monthly) {
                val monthMean = values.map { it.mgdl }.average()
                val monthEstimate = (monthMean + 46.7) / 28.7
                addTableRow(hba1cTable, monthTitle.format(Date(values.first().time)),
                    String.format(Locale.GERMANY, "%.1f %% · %d Werte", monthEstimate, values.size))
            }
        }
        hba1cTable.addView(TextView(this).apply {
            text = "Rechnerische Näherung aus einzelnen Messungen, kein Labor-HbA1c. Bei unregelmäßigen Messzeiten kann die Schätzung stark abweichen. Nicht für Therapieentscheidungen verwenden."
            textSize = 12f
            setPadding(0, 12, 0, 12)
        })
        updateHba1cVisibility()
    }

    private fun updateHba1cVisibility() {
        hba1cTable.visibility = if (hba1cExpanded) View.VISIBLE else View.GONE
    }

    private fun addTableRow(table: LinearLayout, name: String, value: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(10, 12, 10, 12)
        }
        row.addView(TextView(this).apply {
            text = name
            textSize = 14f
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(TextView(this).apply {
            text = value
            textSize = 14f
            setTextColor(Color.rgb(20, 103, 123))
            gravity = android.view.Gravity.END
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        table.addView(row)
        table.addView(View(this).apply { setBackgroundColor(Color.LTGRAY) },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (resources.displayMetrics.density).toInt().coerceAtLeast(1)))
    }

    /**
     * Diagramm mit feststehender mg/dL-Skala und eigener horizontaler Zeitachse.
     * Messwerte werden in chronologischer Reihenfolge in konstantem Abstand
     * gezeichnet. Dadurch bleiben auch zeitlich nahe Messungen unterscheidbar.
     */
    private inner class TrendChart : View(this@MainActivity) {
        private var points: List<ValuePoint> = emptyList()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val good = Color.rgb(13, 116, 142)
        private val lowColor = Color.rgb(207, 114, 17)
        private val highColor = Color.rgb(197, 47, 49)
        private val density = resources.displayMetrics.density
        private val textDensity = resources.displayMetrics.scaledDensity
        private val leftMargin = 62f * density
        private val rightMargin = 14f * density
        private val pointSpacing = 54f * density
        private val endSpacing = 15f * density
        private val touchSlop = ViewConfiguration.get(this@MainActivity).scaledTouchSlop.toFloat()
        private var offsetPx = 0f
        private var downX = 0f
        private var downY = 0f
        private var lastX = 0f
        private var horizontalDragging = false

        init { isClickable = true; contentDescription = "Blutzuckerverlauf nach links und rechts wischen" }

        private fun plotWidth() = (width - leftMargin - rightMargin).coerceAtLeast(0f)
        private fun graphWidth() = if (points.size <= 1) plotWidth()
            else (points.size - 1) * pointSpacing + endSpacing
        private fun maxOffset() = (graphWidth() - plotWidth()).coerceAtLeast(0f)
        private fun clampOffset() { offsetPx = offsetPx.coerceIn(0f, maxOffset()) }

        fun setPoints(values: List<ValuePoint>) {
            points = values
            // Bei Zeitraumwechsel und neuen Messwerten zuerst die neuesten Werte zeigen.
            post { offsetPx = maxOffset(); invalidate() }
            invalidate()
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            clampOffset()
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    lastX = event.x
                    horizontalDragging = false
                    // Erst die Richtung feststellen; nach horizontaler Bewegung
                    // den senkrechten ScrollView nicht dazwischenfunken lassen.
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - downX
                    val dy = event.y - downY
                    if (!horizontalDragging && kotlin.math.abs(dx) > touchSlop &&
                        kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                        horizontalDragging = true
                        parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    if (horizontalDragging) {
                        offsetPx = (offsetPx + lastX - event.x).coerceIn(0f, maxOffset())
                        invalidate()
                        lastX = event.x
                        return true
                    }
                    // Senkrechte Wischbewegungen gehören der Hauptansicht.
                    if (kotlin.math.abs(dy) > touchSlop && kotlin.math.abs(dy) > kotlin.math.abs(dx)) {
                        parent?.requestDisallowInterceptTouchEvent(false)
                        return false
                    }
                    lastX = event.x
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    if (!horizontalDragging) performClick()
                    horizontalDragging = false
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    horizontalDragging = false
                    return true
                }
            }
            return super.onTouchEvent(event)
        }

        override fun performClick(): Boolean {
            super.performClick()
            return true
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val left = leftMargin
            val right = width - rightMargin
            val top = 18f * density
            val bottom = height - 42f * density
            if (right <= left || bottom <= top) return
            paint.style = Paint.Style.FILL
            paint.textSize = 11f * textDensity
            if (points.isEmpty()) {
                paint.color = Color.DKGRAY
                canvas.drawText("Keine Daten für das Diagramm", left, height / 2f, paint)
                return
            }
            clampOffset()
            val minValue = minOf(points.minOf { it.mgdl }, lowerLimit)
            val maxValue = maxOf(points.maxOf { it.mgdl }, upperLimit)
            val minY = fixedAxisMin ?: (minValue - 12.0).coerceAtLeast(0.0)
            val maxY = fixedAxisMax ?: maxOf(maxValue + 12.0, minY + 25)
            fun x(index: Int): Float = left + index * pointSpacing - offsetPx
            fun y(value: Double): Float = bottom - ((value - minY) / (maxY - minY) * (bottom - top)).toFloat()
            fun category(value: Double) = when {
                value < lowerLimit -> lowColor
                value > upperLimit -> highColor
                else -> good
            }
            val lowY = y(lowerLimit).coerceIn(top, bottom)
            val highY = y(upperLimit).coerceIn(top, bottom)
            // Hintergrund und Linien nur im Diagrammfenster; Skala links bleibt fest.
            canvas.save()
            canvas.clipRect(left, top, right, bottom)
            paint.color = Color.rgb(255, 241, 239)
            canvas.drawRect(left, top, right, highY, paint)
            paint.color = Color.rgb(234, 249, 247)
            canvas.drawRect(left, highY, right, lowY, paint)
            paint.color = Color.rgb(255, 247, 228)
            canvas.drawRect(left, lowY, right, bottom, paint)
            for (i in 0..4) {
                val yy = y(maxY - i * (maxY - minY) / 4)
                paint.color = Color.LTGRAY
                paint.strokeWidth = density
                canvas.drawLine(left, yy, right, yy, paint)
            }
            for ((threshold, color) in listOf(lowerLimit to lowColor, upperLimit to highColor)) {
                paint.color = color
                paint.strokeWidth = 1.6f * density
                val yy = y(threshold)
                if (yy >= top && yy <= bottom) canvas.drawLine(left, yy, right, yy, paint)
            }
            paint.strokeWidth = 2.6f * density
            for (i in 1 until points.size) {
                if (x(i) < left || x(i - 1) > right) continue
                val p0 = points[i - 1]
                val p1 = points[i]
                val dividers = mutableListOf(0.0, 1.0)
                val delta = p1.mgdl - p0.mgdl
                if (delta != 0.0) {
                    for (limit in listOf(lowerLimit, upperLimit)) {
                        val t = (limit - p0.mgdl) / delta
                        if (t > 0.0 && t < 1.0) dividers.add(t)
                    }
                }
                dividers.sort()
                for (j in 1 until dividers.size) {
                    val t0 = dividers[j - 1]
                    val t1 = dividers[j]
                    val yy0 = p0.mgdl + delta * t0
                    val yy1 = p0.mgdl + delta * t1
                    paint.color = category(p0.mgdl + delta * ((t0 + t1) / 2))
                    canvas.drawLine(
                        x(i-1) + (x(i)-x(i-1)) * t0.toFloat(), y(yy0),
                        x(i-1) + (x(i)-x(i-1)) * t1.toFloat(), y(yy1), paint
                    )
                }
            }
            for (i in points.indices) {
                if (x(i) < left || x(i) > right) continue
                val point = points[i]
                val pointY = y(point.mgdl)
                paint.color = category(point.mgdl)
                if (pointY in top..bottom)
                    canvas.drawCircle(x(i), pointY, 3.6f * density, paint)
            }
            // Der neueste Messwert wird unabhängig von den individuell gesetzten
            // Grenzwerten hervorgehoben. Die innere Punktfarbe zeigt weiterhin
            // den eingestellten Bereich (orange/türkis/rot) an.
            val lastIndex = points.lastIndex
            if (lastIndex >= 0) {
                val last = points[lastIndex]
                val lastX = x(lastIndex)
                val lastY = y(last.mgdl)
                if (lastX in left..right && lastY in top..bottom) {
                    paint.style = Paint.Style.FILL
                    paint.color = Color.rgb(103, 58, 183) // kräftiger violetter Außenring
                    canvas.drawCircle(lastX, lastY, 9.0f * density, paint)
                    paint.color = Color.WHITE
                    canvas.drawCircle(lastX, lastY, 6.5f * density, paint)
                    paint.color = category(last.mgdl)
                    canvas.drawCircle(lastX, lastY, 4.6f * density, paint)
                }
            }
            canvas.restore()
            // Feste y-Achse: unabhängig von der seitlich verschobenen Kurve.
            paint.color = Color.DKGRAY
            paint.textSize = 11f * textDensity
            for (i in 0..4) {
                val marker = maxY - i * (maxY - minY) / 4
                val yy = y(marker)
                canvas.drawText(String.format(Locale.GERMANY, "%.0f", marker), 3f * density, yy + 4 * density, paint)
            }
            val fmt = SimpleDateFormat(if (selectedPeriod == 0) "HH:mm" else "dd.MM", Locale.GERMANY)
            // X-Achse ebenfalls im sichtbaren Bereich mit den Punkten verschieben.
            canvas.save()
            canvas.clipRect(left, bottom + 2 * density, right, height.toFloat())
            for (i in points.indices) {
                // Nur jede zweite Beschriftung, damit das Datum lesbar bleibt.
                if (i % 2 != 0 || x(i) < left || x(i) > right) continue
                paint.color = Color.DKGRAY
                canvas.drawText(fmt.format(Date(points[i].time)), x(i), height - 10f * density, paint)
            }
            canvas.restore()
        }
    }

    private fun permissionsOk(): Boolean {
        val required = if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT) else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val missing = required.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) { requestPermissions(missing.toTypedArray(), 42); return false }
        return true
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 42 && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) startScan()
        else if (requestCode == 42) status.text = "Bluetooth-Berechtigung nicht erteilt."
    }
    private fun startScan() {
        if (!permissionsOk()) return
        val adapter = getSystemService(BluetoothManager::class.java).adapter
        if (adapter == null || !adapter.isEnabled) { status.text = "Bitte Bluetooth einschalten."; return }
        stopScan(); found.clear(); deviceList.removeAllViews(); details.text = ""; status.text = "Suche läuft (12 Sekunden) …"
        try {
            val scanner = adapter.bluetoothLeScanner ?: run { status.text = "BLE-Scanner nicht verfügbar."; return }
            scanner.startScan(scanCallback); scanning = true
        } catch (_: SecurityException) { status.text = "Bluetooth-Berechtigung fehlt."; return }
        handler.postDelayed({ if (scanning) { stopScan(); status.text = "Suche beendet. ${found.size} Geräte gefunden. Gerät antippen." } }, 12000)
    }
    private fun connect(device: BluetoothDevice, name: String) {
        if (!permissionsOk()) return
        stopScan(); disconnect(); details.text = ""; status.text = "Verbinde mit $name …"
        try { gatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE) }
        catch (e: Exception) { status.text = "Verbindung nicht möglich: ${e.message}" }
    }
    private fun updateSavedCount() {
        val values = getSharedPreferences("glucose_records", MODE_PRIVATE).getString("values", "[]")
        val count = try { JSONArray(values).length() } catch (_: Exception) { 0 }
        savedLabel.text = "Lokal gespeicherte Messwert-Einträge: $count"
    }
    private fun disconnect() {
        val old = gatt ?: return
        gatt = null
        importing = false
        subscriptions.clear()
        try { old.disconnect() } catch (_: SecurityException) { old.close() }
        status.text = "Verbindung wird getrennt …"
    }
    private fun stopScan() {
        if (!scanning) return
        try { getSystemService(BluetoothManager::class.java).adapter?.bluetoothLeScanner?.stopScan(scanCallback) } catch (_: SecurityException) { }
        scanning = false
    }
    override fun onDestroy() { stopScan(); disconnect(); super.onDestroy() }
}
