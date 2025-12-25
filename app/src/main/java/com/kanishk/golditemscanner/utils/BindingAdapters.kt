package com.kanishk.golditemscanner.utils

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener

@SuppressLint("SetTextI18n")
@BindingAdapter("android:text")
fun bindDoubleToText(view: EditText, value: Double?) {
    if (value != null && view.text.toString() != value.toString()) {
        view.setText(value.toString())
    }
}

@InverseBindingAdapter(attribute = "android:text")
fun bindTextToDouble(view: EditText): Double {
    return view.text.toString().toDoubleOrNull() ?: 0.0
}

@BindingAdapter("android:textAttrChanged")
fun setListener(view: EditText, listener: InverseBindingListener?) {
    if (listener != null) {
        view.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                listener.onChange()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
}