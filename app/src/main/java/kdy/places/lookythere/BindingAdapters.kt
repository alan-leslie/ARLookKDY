package kdy.places.lookythere

import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.databinding.BindingAdapter


@BindingAdapter("current", "original", "adjustment")
fun azimuthText(view: TextView, current: Int, original: Int, adjustment : Int) {
    view.text = "Azimuth <$original($adjustment)>/<$current>"
}