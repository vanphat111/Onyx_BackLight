package vn.vanphat111.onyxbacklight

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener

class NotificationSettingsActivity : AppCompatActivity() {
    private val TAG = "OnyxLED_NotifSettings"

    data class AppItem(val appName: String, val packageName: String, val icon: android.graphics.drawable.Drawable, var isSelected: Boolean, val isSystem: Boolean)

    private lateinit var listViewApps: ListView
    private lateinit var seekBarBrightness: SeekBar
    private val appList = mutableListOf<AppItem>()
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var viewColorPreview: View
    private lateinit var cbShowSystemApps: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_settings)

        prefs = getSharedPreferences("OnyxPrefs", Context.MODE_PRIVATE)
        listViewApps = findViewById(R.id.listViewApps)
        seekBarBrightness = findViewById(R.id.seekBarBrightness)

        viewColorPreview = findViewById(R.id.viewColorPreview)
        cbShowSystemApps = findViewById(R.id.cbShowSystemApps)

        val savedR = prefs.getInt("color_r", 0)
        val savedG = prefs.getInt("color_g", 255)
        val savedB = prefs.getInt("color_b", 255)
        viewColorPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.rgb(savedR, savedG, savedB))

        cbShowSystemApps.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "Show System Apps: $isChecked")
            loadApps(isChecked)
        }

        loadApps(false)
        setupBrightnessControl()
        setupColorPicker()
        setupAdvancedSettings()
        setupDemoButton()
    }

    private fun loadApps(showSystem: Boolean) {
        appList.clear()
        val pm = packageManager
        val packages = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
        val allowedApps = prefs.getStringSet("allowed_apps", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        for (packageInfo in packages) {
            val isSystemApp = (packageInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (!showSystem && isSystemApp) continue

            val rawName = pm.getApplicationLabel(packageInfo).toString()
            val appName = if (isSystemApp) "$rawName (System)" else rawName
            val packageName = packageInfo.packageName
            val icon = pm.getApplicationIcon(packageInfo)
            val isSelected = allowedApps.contains(packageName)

            appList.add(AppItem(appName, packageName, icon, isSelected, isSystemApp))
        }

        appList.sortWith(compareBy({ it.isSystem }, { it.appName.lowercase() }))

        val adapter = AppAdapter(this, appList)
        listViewApps.adapter = adapter

        listViewApps.setOnItemClickListener { _, _, position, _ ->
            val app = appList[position]
            app.isSelected = !app.isSelected
            adapter.notifyDataSetChanged()

            if (app.isSelected) {
                Log.d(TAG, "Added App to whitelist: ${app.packageName}")
                allowedApps.add(app.packageName)
            } else {
                Log.d(TAG, "Removed App from whitelist: ${app.packageName}")
                allowedApps.remove(app.packageName)
            }

            prefs.edit().putStringSet("allowed_apps", allowedApps).apply()
        }
    }

    private fun setupBrightnessControl() {
        val savedBrightness = prefs.getInt("notif_brightness", 128)
        seekBarBrightness.progress = savedBrightness

        val lblBrightness = findViewById<TextView>(R.id.lblBrightness)
        lblBrightness.text = "LED Brightness: $savedBrightness"

        seekBarBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                lblBrightness.text = "LED Brightness: $progress"
                if (fromUser) {
                    Log.d(TAG, "Notif Brightness: $progress")
                    prefs.edit().putInt("notif_brightness", progress).apply()
                    LedController().sendCommand("BRIGHTNESS $progress")
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupAdvancedSettings() {
        val seekBlinkCount = findViewById<SeekBar>(R.id.seekBlinkCount)
        val lblBlinkCount = findViewById<TextView>(R.id.lblBlinkCount)
        val seekBlinkSpeed = findViewById<SeekBar>(R.id.seekBlinkSpeed)
        val lblBlinkSpeed = findViewById<TextView>(R.id.lblBlinkSpeed)
        val btnReset = findViewById<Button>(R.id.btnResetDefaults)

        fun updateAdvancedUI() {
            val count = prefs.getInt("notif_blink_count", 3)
            val speed = prefs.getInt("notif_blink_speed", 400)

            seekBlinkCount.progress = count - 1
            lblBlinkCount.text = "Blink Count: $count times"

            seekBlinkSpeed.progress = (speed - 100) / 50
            lblBlinkSpeed.text = "Blink Speed (Delay): ${speed}ms"
        }

        updateAdvancedUI()

        seekBlinkCount.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val count = progress + 1
                lblBlinkCount.text = "Blink Count: $count times"
                if (fromUser) {
                    Log.d(TAG, "Notif Blink Count: $count")
                    prefs.edit().putInt("notif_blink_count", count).apply()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBlinkSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val speed = progress * 50 + 100
                lblBlinkSpeed.text = "Blink Speed (Delay): ${speed}ms"
                if (fromUser) {
                    Log.d(TAG, "Notif Blink Speed: ${speed}ms")
                    prefs.edit().putInt("notif_blink_speed", speed).apply()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnReset.setOnClickListener {
            Log.d(TAG, "Restoring Default Notif Settings")
            prefs.edit()
                .putInt("notif_blink_count", 3)
                .putInt("notif_blink_speed", 400)
                .putInt("notif_brightness", 128)
                .putInt("color_r", 0)
                .putInt("color_g", 255)
                .putInt("color_b", 255)
                .putString("notif_color", "FRAME 0x00FFFF 0x00FFFF 0x00FFFF 0x00FFFF")
                .apply()

            updateAdvancedUI()

            seekBarBrightness.progress = 128
            findViewById<TextView>(R.id.lblBrightness).text = "LED Brightness: 128"
            LedController().sendCommand("BRIGHTNESS 128")

            viewColorPreview.setBackgroundColor(android.graphics.Color.rgb(0, 255, 255))
            LedController().sendCommand("FRAME 0x00FFFF 0x00FFFF 0x00FFFF 0x00FFFF")

            Toast.makeText(this, "All settings restored to defaults!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupColorPicker() {
        val led = LedController()

        fun saveAndPreviewColor(r: Int, g: Int, b: Int) {
            val hex = String.format("0x%02X%02X%02X", r, g, b)
            val command = "FRAME $hex $hex $hex $hex"
            Log.d(TAG, "Notif Color Selected: $command")

            prefs.edit()
                .putInt("color_r", r)
                .putInt("color_g", g)
                .putInt("color_b", b)
                .putString("notif_color", command)
                .apply()

            viewColorPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.rgb(r, g, b))
            led.sendCommand(command)
        }

        findViewById<View>(R.id.preRed).setOnClickListener { saveAndPreviewColor(255, 0, 0) }
        findViewById<View>(R.id.preGreen).setOnClickListener { saveAndPreviewColor(0, 255, 0) }
        findViewById<View>(R.id.preBlue).setOnClickListener { saveAndPreviewColor(0, 0, 255) }
        findViewById<View>(R.id.preCyan).setOnClickListener { saveAndPreviewColor(0, 255, 255) }
        findViewById<View>(R.id.prePink).setOnClickListener { saveAndPreviewColor(255, 0, 255) }
        findViewById<View>(R.id.preYellow).setOnClickListener { saveAndPreviewColor(255, 255, 0) }
        findViewById<View>(R.id.preWhite).setOnClickListener { saveAndPreviewColor(255, 255, 255) }

        findViewById<Button>(R.id.btnPickColor).setOnClickListener {
            val initialColor = android.graphics.Color.rgb(
                prefs.getInt("color_r", 0),
                prefs.getInt("color_g", 255),
                prefs.getInt("color_b", 255)
            )

            ColorPickerDialog.Builder(this)
                .setTitle("Custom LED Color")
                .setPreferenceName("OnyxColorPicker")
                .setPositiveButton("Select", ColorEnvelopeListener { envelope, _ ->
                    val color = envelope.color
                    val r = android.graphics.Color.red(color)
                    val g = android.graphics.Color.green(color)
                    val b = android.graphics.Color.blue(color)
                    saveAndPreviewColor(r, g, b)
                })
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .attachAlphaSlideBar(false)
                .attachBrightnessSlideBar(true)
                .setBottomSpace(12)
                .show()
        }
    }

    class AppAdapter(context: Context, private val apps: List<AppItem>) :
        ArrayAdapter<AppItem>(context, 0, apps) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_app_toggle, parent, false)

            val app = apps[position]
            val imgIcon = view.findViewById<ImageView>(R.id.imgAppIcon)
            val txtName = view.findViewById<TextView>(R.id.txtAppName)
            val checkbox = view.findViewById<CheckBox>(R.id.checkboxApp)

            imgIcon.setImageDrawable(app.icon)
            txtName.text = app.appName
            checkbox.isChecked = app.isSelected

            return view
        }
    }

    private fun setupDemoButton() {
        val btnDemo = findViewById<Button>(R.id.btnDemoEffect)

        btnDemo.setOnClickListener {
            val colorCommand = prefs.getString("notif_color", "FRAME 0x00FFFF 0x00FFFF 0x00FFFF 0x00FFFF") ?: return@setOnClickListener
            val brightness = prefs.getInt("notif_brightness", 128)
            val blinkCount = prefs.getInt("notif_blink_count", 3)
            val blinkSpeed = prefs.getInt("notif_blink_speed", 400).toLong()

            Log.d(TAG, "Playing Normal Notif Demo: $colorCommand | Brightness: $brightness")
            Toast.makeText(this, "Playing Demo...", Toast.LENGTH_SHORT).show()

            kotlin.concurrent.thread {
                val led = LedController()
                led.sendCommand("BRIGHTNESS $brightness")
                for (i in 1..blinkCount) {
                    led.sendCommand(colorCommand)
                    Thread.sleep(blinkSpeed)
                    led.sendCommand("OFF")
                    Thread.sleep(blinkSpeed)
                }
                LedStateManager.restoreBaseState(applicationContext)
            }
        }
    }
}