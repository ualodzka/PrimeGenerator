package vova.com.primegenerator

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormatSymbols
import java.util.*


fun ViewGroup.inflate(layoutRes: Int): View {
    return LayoutInflater.from(context).inflate(layoutRes, this, false)
}

fun Double.formatCurrency(): String {
    val formatSymbols = DecimalFormatSymbols(Locale.ENGLISH)
    formatSymbols.decimalSeparator = '.'
    formatSymbols.groupingSeparator = ' '
    val formatter = java.text.DecimalFormat("#,###.##", formatSymbols)
    return formatter.format(this)
}

fun Int.formatCurrency(): String {
    val formatSymbols = DecimalFormatSymbols(Locale.ENGLISH)
    formatSymbols.decimalSeparator = ' '
    formatSymbols.groupingSeparator = ' '
    val formatter = java.text.DecimalFormat("#,###", formatSymbols)
    return formatter.format(this)
}

fun Float?.metersToKmFormat(): String {
    return "${"%.1f".format(this?.div(1000))} км"
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

fun SwitchCompat.setCheckedWithoutAnimation(checked: Boolean) {
    val beforeVisibility = visibility
    visibility = View.INVISIBLE
    isChecked = checked
    visibility = beforeVisibility
}

fun RecyclerView.smoothSnapToPosition(
    position: Int,
    snapMode: Int = LinearSmoothScroller.SNAP_TO_START
) {
    val smoothScroller = object : LinearSmoothScroller(this.context) {
        override fun getVerticalSnapPreference(): Int {
            return snapMode
        }

        override fun getHorizontalSnapPreference(): Int {
            return snapMode
        }
    }
    smoothScroller.targetPosition = position
    layoutManager?.startSmoothScroll(smoothScroller)
}



fun View.hideKeyboardRequestFocus() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
    this.requestFocus()
}

fun TextView?.setOnActionDoneListener(viewToFocus: View?) {
    this?.setOnEditorActionListener { v, actionId, event ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            viewToFocus?.hideKeyboardRequestFocus()
        }
        false
    }
}

fun Activity?.hideKeyboard() {
    val imm = this?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    var view = this.currentFocus
    if (view == null) {
        view = View(this)
    }
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

