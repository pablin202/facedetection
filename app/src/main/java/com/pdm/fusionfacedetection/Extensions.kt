package com.pdm.fusionfacedetection

import android.widget.ImageView

fun ImageView.setOkImage() {
    this.setImageResource(R.drawable.ic_check)
}

fun ImageView.setErrorImage() {
    this.setImageResource(R.drawable.ic_cancel)
}
