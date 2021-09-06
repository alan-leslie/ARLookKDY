// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package kdy.places.lookythere

import android.Manifest
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.service.autofill.TextValueSanitizer
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import kdy.places.lookythere.ar.PlaceNode
import kdy.places.lookythere.ar.PlacesArFragment
import com.google.maps.android.ktx.utils.sphericalHeading
import kdy.places.lookythere.database.place.PlaceData
import kdy.places.lookythere.databinding.ActivityMainBinding
import kdy.places.lookythere.model.*
import kdy.places.lookythere.viewmodel.PlacesViewModel
import kdy.places.lookythere.viewmodel.PlacesViewModelFactory
import kotlin.math.abs
import kotlin.math.max


class MainActivity : AppCompatActivity(), SensorEventListener {

    private val _TAG = "MainActivity"
    private lateinit var arFragment: PlacesArFragment
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var azimuthText: TextView
    private lateinit var directionText: TextView
    private var currentAzimuth: Int = 0
    private var azimuthIncrement: Int = 1
    private var originalAzimuth: Float = -1.0f
    private var originalAz2deg: MutableLiveData<Int> = MutableLiveData(0)
    private var azimuthAdjustment: MutableLiveData<Int> = MutableLiveData(0)

    private var _angle: MutableLiveData<Int> = MutableLiveData(0)
    val angle : LiveData<Int> = _angle


    // Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Sensor
    private lateinit var sensorManager: SensorManager
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

//    private var fusedLocationProvider: FusedLocationProviderClient? = null
    private var anchor: Anchor? = null
    private var anchorNode: AnchorNode? = null
    private var markers: MutableList<Marker> = emptyList<Marker>().toMutableList()
    private var currentLocation: Location? = null
    private var map: GoogleMap? = null
    private var redSphereRenderable: ModelRenderable? = null
    lateinit var materialColor: Material
    private val placesViewModel: PlacesViewModel by viewModels {
        PlacesViewModelFactory((application as ARApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isSupportedDevice()) {
            return
        }
//        setContentView(R.layout.activity_main)
        val binding : ActivityMainBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.angle = angle
        binding.originalAz2deg = originalAz2deg
        binding.azimuthAdjustment = azimuthAdjustment

        binding.lifecycleOwner = this  // use Fragment.viewLifecycleOwner for fragments

//        azimuthText = findViewById(R.id.azimuth)
//        directionText = findViewById(R.id.direction)


        arFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment) as PlacesArFragment
        mapFragment =
            supportFragmentManager.findFragmentById(R.id.maps_fragment) as SupportMapFragment

        initMaterials()

        val service : Any? = getSystemService(SENSOR_SERVICE)

        if (service != null) {
            sensorManager = service as SensorManager
        }
        else
        {
            Toast.makeText(this, "OnCreate: cannot get SensorManager", Toast.LENGTH_LONG)
            .show()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        checkCameraPermission()
        checkLocationPermission()

        setUpAr()
        setUpMaps()
    }

//    private val barIcon: BitmapDescriptor by lazy {
//        val color = ContextCompat.getColor(this.requireContext(), R.color.colorPrimary)
//        BitmapHelper.vectorToBitmap(this.requireContext(), R.drawable.ic_baseline_local_bar_24, color)
//    }

    private fun getBounds(currentLocation: Location, places :List<Place>) : LatLngBounds.Builder
    {
        val bounds = LatLngBounds.builder()
        var maxLatDiff = 0.0
        var maxLonDiff = 0.0

        places.forEach {
            val latDiff = abs(it.geometry.location.lat - currentLocation.latitude)
            maxLatDiff = max(maxLatDiff, latDiff)

            val lonDiff = abs(it.geometry.location.lng - currentLocation.longitude)
            maxLonDiff = max(maxLonDiff, lonDiff)
        }

        bounds.include(LatLng(currentLocation.latitude + maxLatDiff, currentLocation.longitude + maxLonDiff))
        bounds.include(LatLng(currentLocation.latitude - maxLatDiff, currentLocation.longitude - maxLonDiff))

        return bounds
    }

    private fun initMaterials() {
        MaterialFactory.makeOpaqueWithColor(this,
            Color(Color.RED)
        )
            .thenAccept {
                materialColor = it
            }
    }

    private fun getSelectedMaterial(): Material {
        return materialColor
    }

    override fun onResume() {
        super.onResume()
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    private fun setUpAr() {
        arFragment.setOnTapArPlaneListener { hitResult, _, _ ->
            if (anchor == null) {
                // Create anchor
                val anchor1 = hitResult.createAnchor()
                val newp1 = anchor1.pose
                val rot = newp1.rotationQuaternion[3]
                val azimuth = orientationAngles[0]
                val az2deg = Math.toDegrees(azimuth.toDouble())

                val session: Session? = arFragment.arSceneView.session
                val pos = floatArrayOf(0f, 0f, 0f)
                val rotation = floatArrayOf(0f, 1.00f, 0f, az2deg.toFloat())
                val anchor: Anchor = session!!.createAnchor(Pose(pos, rotation))
                val anchorLocalPosition = anchor.pose
                val anchorLocalRotation = newp1.rotationQuaternion.component4()

                Log.d(_TAG, "anchorLocalPosition is <$anchorLocalPosition>, anchorLocalRotation is <$anchorLocalRotation>")

                anchorNode = AnchorNode(anchor)
                anchorNode?.setParent(arFragment.arSceneView.scene)
                addPlaces(anchorNode!!)
            }
        }
    }

    private fun movePlaces(anchorNode: AnchorNode, adjustment: Int) {
        val adjustmentAsRadians = Math.toRadians(adjustment.toDouble())
        for (child in anchorNode.children) {
            if(child is PlaceNode)
            {
                val thePlaceNode = child as PlaceNode
                thePlaceNode.place.let {
                    currentLocation.let {
                        val localPosition =
                            thePlaceNode.place!!.getPositionVector(originalAzimuth + adjustmentAsRadians.toFloat(), currentLocation!!.latLng)
                        thePlaceNode.localPosition = localPosition
                    }
                }
            }
        }
    }

    private fun movePlacesPlus(anchorNode: AnchorNode) {
        azimuthAdjustment.value = azimuthAdjustment.value!! + azimuthIncrement
        movePlaces(anchorNode, azimuthAdjustment.value!!)
    }

    private fun movePlacesMinus(anchorNode: AnchorNode) {
        azimuthAdjustment.value = azimuthAdjustment.value!! - azimuthIncrement
        movePlaces(anchorNode, azimuthAdjustment.value!!)
    }

    private fun addPlaces(anchorNode: AnchorNode) {
        val anchorLocalPosition = anchorNode.localPosition
        val anchorLocalRotation = anchorNode.localRotation

        Log.d(_TAG, "anchorNodeLocalPosition is <$anchorLocalPosition>, anchorNodeLocalRotation is <$anchorLocalRotation>")

        val currentLocation = currentLocation
        if (currentLocation == null) {
            Log.w(_TAG, "Location has not been determined yet")
            return
        }

        val places = placesViewModel.places.value
        if (places == null) {
            Log.w(_TAG, "No places to put")
            return
        }

        val material = getSelectedMaterial()
        val size = 0.03f
        val redSphereRenderable = ShapeFactory.makeSphere(
            size,
            Vector3(size / 2.0f, size / 2.0f, size / 2.0f),
            material)

        originalAzimuth = orientationAngles[0]
        originalAz2deg.value = (Math.round(((Math.toDegrees(originalAzimuth.toDouble()) + 360.0) % 360.0).toDouble())).toInt()
        val az2deg = originalAz2deg.value
        Log.d(_TAG, "addPlaces: azimuth is <$originalAzimuth>, <$az2deg>")

//        azimuthText.text = "Original azimuth is <$originalAz2deg>"


        for (place in places) {
            val pathLength = place.getPathLength(currentLocation.latLng)
            val placeName = place.name
            Log.d(_TAG, "addPlaces: name is <$placeName, pathLength is <$pathLength>")

            if(pathLength > 1.0f) {
                // Add the place in AR
                val placeNode = PlaceNode(this, place)
                placeNode.setParent(anchorNode)
                val heading =
                    currentLocation.latLng.sphericalHeading(place.geometry.location.latLng)
                val placeName = place.name
                val heading2rad = Math.toRadians(heading)
                val relativeAngleRad = (originalAzimuth * -1.0).toDouble() + heading2rad
                val relativeAngleDeg = Math.toDegrees(relativeAngleRad)

                Log.d(_TAG, "addPlaces: heading is <$heading>, place is <$placeName>")
                Log.d(
                    _TAG,
                    "relativeAngleRad is <$relativeAngleRad>, relativeAngleDeg is <$relativeAngleDeg>"
                )

                val localPosition = place.getPositionVector(originalAzimuth, currentLocation.latLng)
                if (placeName == "home") {
                    placeNode.localPosition = Vector3(0.0f, 0.0f, 0.0f)
                } else {
                    placeNode.localPosition = localPosition
                }

//            placeNode.renderable = redSphereRenderable
//            val icon = placeNode.renderable

                val locPos = placeNode.localPosition
                val worldPos = placeNode.worldPosition

                Log.d(_TAG, "locPos is <$locPos>")
                Log.d(_TAG, "worldPos is <$worldPos>")

                placeNode.setOnTapListener { _, _ ->
                    showInfoWindow(place)
                }
            }

            // Add the place in maps
            map?.let {
                val marker = it.addMarker(
                    MarkerOptions()
                        .position(place.geometry.location.latLng)
                        .title(place.name)
                )
                marker!!.tag = place
                markers.add(marker)
            }
        }

        if (places.isNotEmpty()) {
            val bounds = getBounds(currentLocation, places)
            map!!.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
        }
    }

    private fun showInfoWindow(place: Place) {
        // Show in AR
        val matchingPlaceNode = anchorNode?.children?.filter {
            it is PlaceNode
        }?.first {
            val otherPlace = (it as PlaceNode).place ?: return@first false
            return@first otherPlace == place
        } as? PlaceNode
        matchingPlaceNode?.showInfoWindow()

        // Show as marker
        val matchingMarker = markers.firstOrNull {
            val placeTag = (it.tag as? Place) ?: return@firstOrNull false
            return@firstOrNull placeTag == place
        }
        matchingMarker?.showInfoWindow()
    }

    private fun setUpMaps() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED)
        {
            mapFragment.getMapAsync { googleMap ->
                googleMap.isMyLocationEnabled = true

                getCurrentLocation {
                    val pos = CameraPosition.fromLatLngZoom(it.latLng, 10f)
                    googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(pos))
                    getNearbyPlaces(it)
                }
                googleMap.setOnMarkerClickListener { marker ->
                    val tag = marker.tag
                    if (tag !is Place) {
                        return@setOnMarkerClickListener false
                    }
                    showInfoWindow(tag)
                    return@setOnMarkerClickListener true
                }
                map = googleMap
            }
        }
    }

    private fun getCurrentLocation(onSuccess: (Location) -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED)
        {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                currentLocation = location
                onSuccess(location)
            }.addOnFailureListener {
                Log.e(_TAG, "Could not get location")
            }
        }
    }

    fun onPlus(view: View)
    {
//        Toast.makeText(this, "onPlus triggered", Toast.LENGTH_SHORT)
//            .show()
        anchorNode?.let {
            movePlacesPlus(anchorNode!!)
        }
    }

    fun onMinus(view: View)
    {
//        Toast.makeText(this, "onMinus triggered", Toast.LENGTH_SHORT)
//            .show()
        anchorNode?.let {
            movePlacesMinus(anchorNode!!)
        }
    }

    private fun getNearbyPlaces(location: Location) {
        placesViewModel.getNearbyPlaces(location)
    }

    private fun isSupportedDevice(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val openGlVersionString = activityManager.deviceConfigurationInfo.glEsVersion
        if (openGlVersionString.toDouble() < 3.0) {
            Toast.makeText(this, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                .show()
            finish()
            return false
        }
        return true
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        val sensorName = sensor?.name
        Log.d("onAccuracyChanged", "sensor <$sensorName> accuracy is <$accuracy>")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) {
            return
        }

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
        }

        updateOrientationAngles()
    }

    private fun updateOrientationAngles() {
        val result = SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )

        if (result){
            val orientation = SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val degrees : Double = ((Math.toDegrees(orientation.get(0).toDouble()) + 360.0) % 360.0).toDouble()

            _angle.value = Math.round(degrees).toInt()
        }
    }

    private fun checkCameraPermission(){
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.CAMERA
                )
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder(this)
                    .setTitle("Camera Permission Needed")
                    .setMessage("This app needs the camera permission to work with AR, please accept to use camera functionality")
                    .setPositiveButton(
                        "OK"
                    ) { _, _ ->
                        //Prompt the user once explanation has been shown
                        requestCameraPermission()
                    }
                    .create()
                    .show()
            } else {
                // No explanation needed, we can request the permission.
                requestCameraPermission()
            }
        }
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.CAMERA),
            MY_PERMISSIONS_REQUEST_CAMERA
        )
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder(this)
                    .setTitle("Location Permission Needed")
                    .setMessage("This app needs the Location permission, please accept to use location functionality")
                    .setPositiveButton(
                        "OK"
                    ) { _, _ ->
                        //Prompt the user once explanation has been shown
                        requestLocationPermission()
                    }
                    .create()
                    .show()
            } else {
                // No explanation needed, we can request the permission.
                requestLocationPermission()
            }
        }
    }
    private fun requestLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ),
                MY_PERMISSIONS_REQUEST_LOCATION
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MY_PERMISSIONS_REQUEST_LOCATION
            )
        }
    }

    private val locationRequest: LocationRequest =  LocationRequest.create().apply {
        interval = 30
        fastestInterval = 10
        priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        maxWaitTime= 60
    }

    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val locationList = locationResult.locations
            if (locationList.isNotEmpty()) {
                //The last location in the list is the newest
                val location = locationList.last()
                Toast.makeText(
                    this@MainActivity,
                    "Got Location: " + location.toString(),
                    Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        fusedLocationClient?.requestLocationUpdates(
                            locationRequest,
                            locationCallback,
                            Looper.getMainLooper()
                        )
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "location permission denied", Toast.LENGTH_LONG).show()
                }
                return
            }
            MY_PERMISSIONS_REQUEST_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show()
                    }
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show()
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    companion object {
        private const val MY_PERMISSIONS_REQUEST_LOCATION = 99
        private const val MY_PERMISSIONS_REQUEST_CAMERA = 200
    }
}

val Location.latLng: LatLng
    get() = LatLng(this.latitude, this.longitude)

