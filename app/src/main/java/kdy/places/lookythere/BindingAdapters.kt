package kdy.places.lookythere

import android.view.View
import android.widget.TextView
import androidx.databinding.BindingAdapter

@BindingAdapter("app:currentAzimuth")
fun angleAndDirection(view: TextView, number: Int) {
    view.text = "Current azimuth is <$number>"
}