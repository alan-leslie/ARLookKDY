package kdy.places.lookythere.model

import com.google.android.gms.maps.model.LatLng
import com.google.ar.sceneform.math.Vector3
import com.google.maps.android.ktx.utils.sphericalHeading
import com.google.maps.android.ktx.utils.sphericalPathLength
import kotlin.math.cos
import kotlin.math.sin

/**
 * A model describing details about a Place (location, name, type, etc.).
 */
data class Peak(
    val place_id: String,
    val icon: String,
    val name: String,
    val geometry: kdy.places.lookythere.model.Geometry
) {
    val _TAG = "Peak"

    override fun equals(other: Any?): Boolean {
        if (other !is Peak) {
            return false
        }

        return this.place_id == other.place_id
    }

    override fun hashCode(): Int {
        return this.place_id.hashCode()
    }
}

fun Peak.getPathLength(latLng: LatLng): Float {
    val placeLatLng = this.geometry.location.latLng
    val path: List<LatLng> = listOf(latLng, placeLatLng)
    return path.sphericalPathLength().toFloat()
}

fun Peak.getPositionVector(azimuth: Float, latLng: LatLng): Vector3 {
    val placeLatLng = this.geometry.location.latLng
    val heading = latLng.sphericalHeading(placeLatLng)
    val heading2rad =  Math.toRadians(heading)
    val pathLength = getPathLength(latLng)
    val relativeAngleRad = (azimuth * -1.0).toDouble() + heading2rad
    val relativeAngleDeg = Math.toDegrees(relativeAngleRad)
    val radius = calcRadius(pathLength)
    val x = radius * sin((azimuth * -1.0).toDouble() + heading2rad).toFloat()
    val y = 0.1f
    val z = -radius * cos((azimuth * -1.0).toDouble() + heading2rad).toFloat()
    return Vector3(x, y, z)
}

private fun calcRadius(pathLength: Float): Float
{
    if(pathLength <= 100.0f)
    {
        return 4.0f
    }
    else
    {
        if(pathLength <= 500.0f)
        {
            val diff = pathLength  - 100.0f
            return 4.0f + diff / 200.0f
        }
        else
        {
            if (pathLength < 4500)
            {
                val diff = pathLength - 500.0f
                return 6.0f + diff/ 1000.0f
            }
            else
            {
                return 10.0f
            }
        }
    }

    return 10.0f
}
