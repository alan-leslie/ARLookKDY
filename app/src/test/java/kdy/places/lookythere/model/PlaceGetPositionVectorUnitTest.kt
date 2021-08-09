package kdy.places.lookythere.model

import com.google.ar.sceneform.math.Vector3
import org.junit.Assert
import org.junit.Test

/**
 * Test examples of getPositionVector with the most obvious headings
 * i.e. 90 degrees north and south, 45 degrees north west and north east
 */
class PlaceGetPositionVectorUnitTest {
    val currentLocation = GeometryLocation(lat = 56.111773, lng = -3.154543)
    val north5 = GeometryLocation(currentLocation.lat + .045, currentLocation.lng)
    val south5 = GeometryLocation(currentLocation.lat - .045, currentLocation.lng)
    val east5 = GeometryLocation(currentLocation.lat, currentLocation.lng + .055)
    val west5 = GeometryLocation(currentLocation.lat, currentLocation.lng - .055)
    val northwest5 = GeometryLocation(56.14361, -3.211667)
    val northeast5 = GeometryLocation(56.14361, -3.0975)

    val azimuthn = 0.0f
    val azimuths = (Math.PI).toFloat()

    val maxDistance = 10.0f
    val elevation = 0.1f
    val oblique = 7.07f

    @Test
    fun vector_east5_isCorrect() {
        val firstPlace = Place(
            place_id = "east5",
            icon = "https://maps.gstatic.com/mapfiles/place_api/icons/v1/png_71/park-71.png",
            name = "east5",
            geometry = Geometry(location = east5)
        )

        val positionVector = firstPlace.getPositionVector(azimuthn, currentLocation.latLng)
        val requiredVector = Vector3(maxDistance, elevation, 0.0f)
        Assert.assertEquals(requiredVector.x, positionVector.x, 0.00001f)
        Assert.assertEquals(requiredVector.y, positionVector.y)
        Assert.assertEquals(requiredVector.z, positionVector.z, 0.01f)
    }

    @Test
    fun vector_west5_isCorrect() {
        val firstPlace = Place(
            place_id = "west5",
            icon = "https://maps.gstatic.com/mapfiles/place_api/icons/v1/png_71/park-71.png",
            name = "west5",
            geometry = Geometry(location = west5)
        )

        val positionVector = firstPlace.getPositionVector(azimuthn, currentLocation.latLng)
        val requiredVector = Vector3(maxDistance * -1.0f, elevation, 0.0f)
        Assert.assertEquals(requiredVector.x, positionVector.x, 0.00001f)
        Assert.assertEquals(requiredVector.y, positionVector.y)
        Assert.assertEquals(requiredVector.z, positionVector.z, 0.01f)
    }

    @Test
    fun vector_north5_isCorrect() {
        val firstPlace = Place(
            place_id = "north5",
            icon = "https://maps.gstatic.com/mapfiles/place_api/icons/v1/png_71/park-71.png",
            name = "north5",
            geometry = Geometry(location = north5)
        )

        val positionVector = firstPlace.getPositionVector(azimuthn, currentLocation.latLng)
        val requiredVector = Vector3(0.0f, elevation, maxDistance * -1.0f)
        Assert.assertEquals(requiredVector.x, positionVector.x, 0.00001f)
        Assert.assertEquals(requiredVector.y, positionVector.y)
        Assert.assertEquals(requiredVector.z, positionVector.z, 0.01f)
    }

    @Test
    fun vector_south5_isCorrect() {
        val firstPlace = Place(
            place_id = "south5",
            icon = "https://maps.gstatic.com/mapfiles/place_api/icons/v1/png_71/park-71.png",
            name = "south5",
            geometry = Geometry(location = south5)
        )

        val positionVector = firstPlace.getPositionVector(azimuthn, currentLocation.latLng)
        val requiredVector = Vector3(0.0f, elevation, maxDistance)
        Assert.assertEquals(requiredVector.x, positionVector.x, 0.00001f)
        Assert.assertEquals(requiredVector.y, positionVector.y)
        Assert.assertEquals(requiredVector.z, positionVector.z, 0.01f)
    }

    @Test
    fun vector_north5_azimuths_isCorrect() {
        val firstPlace = Place(
            place_id = "north5",
            icon = "https://maps.gstatic.com/mapfiles/place_api/icons/v1/png_71/park-71.png",
            name = "north5",
            geometry = Geometry(location = north5)
        )

        val positionVector = firstPlace.getPositionVector(azimuths, currentLocation.latLng)
        val requiredVector = Vector3(0.0f, elevation, maxDistance)
        Assert.assertEquals(requiredVector.x, positionVector.x, 0.00001f)
        Assert.assertEquals(requiredVector.y, positionVector.y)
        Assert.assertEquals(requiredVector.z, positionVector.z, 0.01f)
    }

    @Test
    fun vector_south5_azimuths_isCorrect() {
        val firstPlace = Place(
            place_id = "south5",
            icon = "https://maps.gstatic.com/mapfiles/place_api/icons/v1/png_71/park-71.png",
            name = "south5",
            geometry = Geometry(location = south5)
        )

        val positionVector = firstPlace.getPositionVector(azimuths, currentLocation.latLng)
        val requiredVector = Vector3(0.0f, elevation, maxDistance * -1.0f)
        Assert.assertEquals(requiredVector.x, positionVector.x, 0.00001f)
        Assert.assertEquals(requiredVector.y, positionVector.y)
        Assert.assertEquals(requiredVector.z, positionVector.z, 0.01f)
    }

    @Test
    fun vector_northeast_isCorrect() {
        val firstPlace = Place(
            place_id = "northeast5",
            icon = "https://maps.gstatic.com/mapfiles/place_api/icons/v1/png_71/park-71.png",
            name = "northeast5",
            geometry = Geometry(location = northeast5)
        )

        val positionVector = firstPlace.getPositionVector(azimuthn, currentLocation.latLng)
        val requiredVector = Vector3(oblique, elevation, oblique * -1.0f)
        Assert.assertEquals(requiredVector.x, positionVector.x, 0.01f)
        Assert.assertEquals(requiredVector.y, positionVector.y)
        Assert.assertEquals(requiredVector.z, positionVector.z, 0.01f)
    }

    @Test
    fun vector_northwest5_isCorrect() {
        val firstPlace = Place(
            place_id = "northwest5",
            icon = "https://maps.gstatic.com/mapfiles/place_api/icons/v1/png_71/park-71.png",
            name = "northwest5",
            geometry = Geometry(location = northwest5)
        )

        val positionVector = firstPlace.getPositionVector(azimuthn, currentLocation.latLng)
        val requiredVector = Vector3(oblique * -1.0f, elevation, oblique * -1.0f)

        Assert.assertEquals(requiredVector.x, positionVector.x, 0.01f)
        Assert.assertEquals(requiredVector.y, positionVector.y)
        Assert.assertEquals(requiredVector.z, positionVector.z, 0.01f)
    }
}