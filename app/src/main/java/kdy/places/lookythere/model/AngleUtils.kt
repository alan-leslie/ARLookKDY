package kdy.places.lookythere.model

fun normalizeAngle(angle: Float): Float {
    return wrap(angle, 0f, 360f) % 360
}

fun wrap(value: Float, min: Float, max: Float): Float {
    return wrap(value.toDouble(), min.toDouble(), max.toDouble()).toFloat()
}

fun wrap(value: Double, min: Double, max: Double): Double {
    val range = max - min

    var newValue = value

    while (newValue > max) {
        newValue -= range
    }

    while (newValue < min) {
        newValue += range
    }

    return newValue
}
