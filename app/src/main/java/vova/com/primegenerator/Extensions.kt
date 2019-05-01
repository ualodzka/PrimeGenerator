package vova.com.primegenerator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat


fun ViewGroup.inflate(layoutRes: Int): View {
    return LayoutInflater.from(context).inflate(layoutRes, this, false)
}

fun View.visible() {
    visibility = View.VISIBLE
    this.invalidate()
}

fun View.invisible() {
    visibility = View.INVISIBLE
    this.invalidate()
}

fun View.gone() {
    visibility = View.GONE
    this.invalidate()
}

fun Button.deactivate() {
    this.isEnabled = false
    background = ContextCompat.getDrawable(context, R.drawable.background_grey_solid_rounded)
    setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
}

fun Button.activate() {
    this.isEnabled = true
    background = ContextCompat.getDrawable(context, R.drawable.background_white_solid_rounded)
    setTextColor(ContextCompat.getColor(context, android.R.color.black))
}



