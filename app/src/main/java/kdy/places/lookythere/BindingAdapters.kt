package kdy.places.lookythere

import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.databinding.BindingAdapter


@BindingAdapter("current", "original", "adjustment")
fun azimuthText(view: TextView, current: Float, original: Float, adjustment : Int) {
    val cAsInt = current.toInt()
    val oAsInt = original.toInt()

    view.text = "Azimuth <$oAsInt($adjustment)>/<$cAsInt>"
}