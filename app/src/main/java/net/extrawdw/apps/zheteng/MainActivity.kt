package net.extrawdw.apps.zheteng

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import net.extrawdw.apps.zheteng.ui.main.AdViewModel
import net.extrawdw.apps.zheteng.ui.main.ZhetengApp
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity(), SensorEventListener {
    private val model: AdViewModel by viewModels()
    private lateinit var sensors: SensorManager
    private var hinge: Sensor? = null
    private var sensorAvailable by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        window.isNavigationBarContrastEnforced = false
        sensors = getSystemService(SENSOR_SERVICE) as SensorManager
        hinge = sensors.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE)
        sensorAvailable = hinge != null
        if (!sensorAvailable && !model.demo) model.enableDemo()
        setContent { ZhetengApp(model, sensorAvailable) }
    }

    override fun onResume() {
        super.onResume()
        model.setForeground(true)
        hinge?.let {
            if (!sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)) {
                sensorAvailable = false
                if (!model.demo) model.enableDemo()
            }
        }
    }
    override fun onPause() {
        model.setForeground(false)
        sensors.unregisterListener(this)
        super.onPause()
    }
    override fun onSensorChanged(event: SensorEvent) {
        if (!model.demo && event.sensor.type == Sensor.TYPE_HINGE_ANGLE) model.sample(event.values[0])
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
