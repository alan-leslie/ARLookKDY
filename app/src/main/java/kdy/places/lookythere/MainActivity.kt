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
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.getSystemService
import androidx.fragment.app.viewModels
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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
import kdy.places.lookythere.model.*
import kdy.places.lookythere.viewmodel.PlacesViewModel
import kdy.places.lookythere.viewmodel.PlacesViewModelFactory
import kotlin.math.abs
import kotlin.math.max


class MainActivity : AppCompatActivity(), SensorEventListener {

    private val _TAG = "MainActivity"
    private lateinit var arFragment: PlacesArFragment
    private lateinit var mapFragment: SupportMapFragment

    // Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Sensor
    private lateinit var sensorManager: SensorManager
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isSupportedDevice()) {
            return
        }
        setContentView(R.layout.activity_main)

        arFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment) as PlacesArFragment
        mapFragment =
            supportFragmentManager.findFragmentById(R.id.maps_fragment) as SupportMapFragment

        initMaterials()

        sensorManager = getSystemService()!!
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setUpAr()
        setUpMaps()
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
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
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
                val az2deg = (azimuth * (180.0/Math.PI)).toFloat()

                val session: Session? = arFragment.arSceneView.session
                val pos = floatArrayOf(0f, 0f, 0f)
                val rotation = floatArrayOf(0f, 1.00f, 0f, az2deg)
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

        val azimuth = orientationAngles[0]
        val az2deg = azimuth * (180.0/Math.PI)
        Log.d(_TAG, "addPlaces: azimuth is <$azimuth>, <$az2deg>")

        for (place in places) {
            val pathLength = place.getPathLength(currentLocation.latLng)
            if(pathLength > 5.0f) {
                // Add the place in AR
                val placeNode = PlaceNode(this, place)
                placeNode.setParent(anchorNode)
                val heading =
                    currentLocation.latLng.sphericalHeading(place.geometry.location.latLng)
                val placeName = place.name
                val heading2rad = heading * Math.PI / 180.0
                val relativeAngleRad = (azimuth * -1.0).toDouble() + heading2rad
                val relativeAngleDeg = relativeAngleRad * 180.0 / Math.PI

                Log.d(_TAG, "addPlaces: heading is <$heading>, place is <$placeName>")
                Log.d(
                    _TAG,
                    "relativeAngleRad is <$relativeAngleRad>, relativeAngleDeg is <$relativeAngleDeg>"
                )

                val localPosition = place.getPositionVector(azimuth, currentLocation.latLng)
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
        mapFragment.getMapAsync { googleMap ->
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
//                return
            }
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

    private fun getCurrentLocation(onSuccess: (Location) -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            currentLocation = location
            onSuccess(location)
        }.addOnFailureListener {
            Log.e(_TAG, "Could not get location")
        }
    }

    private fun getNearbyPlaces(location: Location) {
//        placesViewModel.getNearbyPlaces(location)
//        placesViewModel.getCardinalPointPlaces(location)

        placesViewModel.getDBPlaces()
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

        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
    }
}

val Location.latLng: LatLng
    get() = LatLng(this.latitude, this.longitude)

