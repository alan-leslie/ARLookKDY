package kdy.places.lookythere.viewmodel

import android.app.Activity
import android.content.Context
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.Sensor

import android.hardware.SensorEvent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlin.math.atan2
import com.google.ar.sceneform.math.Vector3
import kdy.places.lookythere.model.Bearing

class OrientationSensor (context: Context) : SensorEventListener {
    val SENSOR_UNAVAILABLE = -1

    private var _azimuth: MutableLiveData<Float> = MutableLiveData(0.0f)
    val azimuth : LiveData<Float> = _azimuth

    // references to other objects
    private var sensorManager: SensorManager? = null
    // non-null if this class should call its parent after onSensorChanged(...) and onAccuracyChanged(...) notifications
    private var m_parent : SensorEventListener? = null
    // current activity for call to getWindowManager().getDefaultDisplay().getRotation()
    private var m_activity : Activity? = null

    // raw inputs from Android sensors
    // Normalised gravity vector, (i.e. length of this vector is 1), which points straight up into space
    private var m_NormGravityVector : FloatArray? = null
    // Normalised magnetic field vector, (i.e. length of this vector is 1)
    private var m_NormMagFieldValues : FloatArray? = null

    // accuracy specifications. SENSOR_UNAVAILABLE if unknown, otherwise SensorManager.SENSOR_STATUS_UNRELIABLE, SENSOR_STATUS_ACCURACY_LOW, SENSOR_STATUS_ACCURACY_MEDIUM or SENSOR_STATUS_ACCURACY_HIGH
    // accuracy of gravity sensor
    private var m_GravityAccuracy = 0
    // accuracy of magnetic field sensor
    private var m_MagneticFieldAccuracy = 0

    var m_OrientationOK // set true if m_azimuth_radians and m_pitch_radians have successfully been calculated following a call to onSensorChanged(...)
            = false

    init {
        sensorManager = context
            .getSystemService(Context.SENSOR_SERVICE) as SensorManager
//        var gsensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var msensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        var gsensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_GRAVITY)
    }

    fun start(activity: Activity) {
        register(activity, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        unregister()
    }

    fun register(activity: Activity, sensorSpeed: Int): Int {
        m_activity =
            activity // current activity required for call to getWindowManager().getDefaultDisplay().getRotation()
        m_NormGravityVector = FloatArray(3)
        m_NormMagFieldValues = FloatArray(3)
        m_OrientationOK = false
        var count = 0
        val SensorGravity = sensorManager!!.getDefaultSensor(Sensor.TYPE_GRAVITY)
        if (SensorGravity != null) {
            sensorManager!!.registerListener(this, SensorGravity, sensorSpeed)
            m_GravityAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
            count++
        } else {
            m_GravityAccuracy = SENSOR_UNAVAILABLE
        }
        val SensorMagField = sensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (SensorMagField != null) {
            sensorManager!!.registerListener(this, SensorMagField, sensorSpeed)
            m_MagneticFieldAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
            count++
        } else {
            m_MagneticFieldAccuracy = SENSOR_UNAVAILABLE
        }
        return count
    }

    fun unregister() {
        m_activity = null
        m_NormMagFieldValues = null
        m_NormGravityVector = m_NormMagFieldValues
        m_OrientationOK = false
        sensorManager!!.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val alpha = 0.97f
        val SensorType = event.sensor.type
        when (SensorType) {
            Sensor.TYPE_GRAVITY -> {
                if (m_NormGravityVector == null) m_NormGravityVector = FloatArray(3)
                m_NormGravityVector!![0] = alpha * m_NormGravityVector!![0] + (1 - alpha) * event.values[0]
                m_NormGravityVector!![1] = alpha * m_NormGravityVector!![1] + (1 - alpha) * event.values[1]
                m_NormGravityVector!![2] = alpha * m_NormGravityVector!![2] + (1 - alpha) * event.values[2]
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                if (m_NormMagFieldValues == null) m_NormMagFieldValues = FloatArray(3)
                m_NormMagFieldValues!![0] = alpha * m_NormMagFieldValues!![0] + (1 - alpha) * event.values[0]
                m_NormMagFieldValues!![1] = alpha * m_NormMagFieldValues!![1] + (1 - alpha) * event.values[1]
                m_NormMagFieldValues!![2] = alpha * m_NormMagFieldValues!![2] + (1 - alpha) * event.values[2]
            }
        }

        if (m_NormGravityVector != null && m_NormMagFieldValues != null) {
            val newAzimuth = calculateAzimuth(m_NormGravityVector!!, m_NormMagFieldValues!!)
            newAzimuth?.let{
                _azimuth.value = newAzimuth
            }
        }
        if (m_parent != null) m_parent!!.onSensorChanged(event)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        val sensorName = sensor?.name
        Log.d("onAccuracyChanged", "sensor <$sensorName> accuracy is <$accuracy>")
    }

    fun calculateAzimuth(
        gravity: FloatArray,
        magneticField: FloatArray,
        checkMagnitude: Boolean = true
    ): Float? {
        val gravityVector = Vector3(gravity[0], gravity[1], gravity[2])
        val magneticFieldVector = Vector3(magneticField[0], magneticField[1], magneticField[2])

        val normGravity = gravityVector.normalized()
        val normMagField = magneticFieldVector.normalized()

        // East vector
        val east = Vector3.cross(normMagField, normGravity)
        val normEast = east.normalized()

        if (checkMagnitude) {
            val eastMagnitude = east.length()
            val gravityMagnitude = gravityVector.length()
            val magneticMagnitude = magneticFieldVector.length()
            if (gravityMagnitude * magneticMagnitude * eastMagnitude < 0.1f) {
                return null
            }
        }

        // North vector
        val dotProduct = Vector3.dot(normGravity, normMagField)
        val northX = normMagField!!.x - normGravity!!.x * dotProduct
        val northY = normMagField.y - normGravity.y * dotProduct
        val northZ = normMagField.z - normGravity.z * dotProduct
        val north = Vector3(northX, northY, northZ)
        val normNorth = north.normalized()

        // Azimuth
        // NB: see https://math.stackexchange.com/questions/381649/whats-the-best-3d-angular-co-ordinate-system-for-working-with-smartfone-apps
        val sin = normEast.y - normNorth.x
        val cos = normEast.x + normNorth.y
        val newAzimuth = if ((sin == 0f && cos == 0f)) 0f else atan2(sin, cos)
        // if both are zero, should be invalid?

        if (newAzimuth.isNaN()){
            return null
        }

        return newAzimuth
    }
}
